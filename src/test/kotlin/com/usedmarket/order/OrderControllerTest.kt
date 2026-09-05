package com.usedmarket.order

import com.fasterxml.jackson.databind.ObjectMapper
import com.usedmarket.catalog.entity.Brand
import com.usedmarket.catalog.entity.Category
import com.usedmarket.catalog.repository.BrandRepository
import com.usedmarket.catalog.repository.CategoryRepository
import com.usedmarket.coupon.entity.Coupon
import com.usedmarket.coupon.entity.DiscountType
import com.usedmarket.coupon.repository.CouponRepository
import com.usedmarket.product.dto.ProductCreateRequest
import com.usedmarket.product.entity.ConditionGrade
import com.usedmarket.security.JwtService
import com.usedmarket.user.entity.RoleName
import com.usedmarket.user.entity.User
import com.usedmarket.user.repository.UserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OrderControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var categoryRepository: CategoryRepository

    @Autowired
    lateinit var brandRepository: BrandRepository

    @Autowired
    lateinit var couponRepository: CouponRepository

    @Autowired
    lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    lateinit var jwtService: JwtService

    private fun tokenFor(role: RoleName, email: String): String {
        val user = User(
            email = email,
            passwordHash = passwordEncoder.encode("password123"),
            fullName = "Test $role",
            role = role
        )
        userRepository.save(user)
        return jwtService.generateAccessToken(user)
    }

    private fun createProduct(staffToken: String, price: String, stockQuantity: Int): String {
        val category = categoryRepository.save(Category(name = "Cat ${System.nanoTime()}", slug = "cat-${System.nanoTime()}"))
        val brand = brandRepository.save(Brand(name = "Brand ${System.nanoTime()}", slug = "brand-${System.nanoTime()}"))
        val request = ProductCreateRequest(
            name = "Order Test Product",
            slug = "order-test-${System.nanoTime()}",
            categoryId = category.id!!,
            brandId = brand.id!!,
            price = BigDecimal(price),
            condition = ConditionGrade.GOOD,
            stockQuantity = stockQuantity
        )
        val result = mockMvc.perform(
            post("/api/products")
                .header("Authorization", "Bearer $staffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        ).andExpect(status().isCreated).andReturn()
        return objectMapper.readTree(result.response.contentAsString).get("id").asText()
    }

    private fun addToCart(customerToken: String, productId: String, quantity: Int = 1) {
        mockMvc.perform(
            post("/api/cart/items")
                .header("Authorization", "Bearer $customerToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"productId":"$productId","quantity":$quantity}""")
        ).andExpect(status().isOk)
    }

    private val checkoutBody = """
        {
            "recipientName":"Nguyen Van A",
            "recipientPhone":"0900000000",
            "shippingAddressLine":"123 Le Loi",
            "shippingWard":"Ben Nghe",
            "shippingDistrict":"District 1",
            "shippingProvince":"Ho Chi Minh City",
            "paymentMethod":"COD"
        }
    """.trimIndent()

    @Test
    fun `checkout creates an order, reserves stock, and empties the cart`() {
        val staffToken = tokenFor(RoleName.STAFF, "staff-ord1@example.com")
        val customerToken = tokenFor(RoleName.CUSTOMER, "cust-ord1@example.com")
        val productId = createProduct(staffToken, "100000", stockQuantity = 5)
        addToCart(customerToken, productId, 2)

        mockMvc.perform(
            post("/api/orders")
                .header("Authorization", "Bearer $customerToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(checkoutBody)
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andExpect(jsonPath("$.items.length()").value(1))
            .andExpect(jsonPath("$.items[0].quantity").value(2))
            .andExpect(jsonPath("$.subtotal").value(200000))
            .andExpect(jsonPath("$.paymentMethod").value("COD"))

        mockMvc.perform(get("/api/cart").header("Authorization", "Bearer $customerToken"))
            .andExpect(jsonPath("$.items.length()").value(0))

        mockMvc.perform(get("/api/products/$productId/inventory").header("Authorization", "Bearer $staffToken"))
            .andExpect(jsonPath("$.availableStock").value(3))
            .andExpect(jsonPath("$.reservedStock").value(2))
    }

    @Test
    fun `checkout with an empty cart is rejected`() {
        val customerToken = tokenFor(RoleName.CUSTOMER, "cust-ord2@example.com")

        mockMvc.perform(
            post("/api/orders")
                .header("Authorization", "Bearer $customerToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(checkoutBody)
        ).andExpect(status().isBadRequest)
    }

    @Test
    fun `checkout with a coupon commits usage, unlike cart preview`() {
        val staffToken = tokenFor(RoleName.STAFF, "staff-ord3@example.com")
        val customerToken = tokenFor(RoleName.CUSTOMER, "cust-ord3@example.com")
        val productId = createProduct(staffToken, "200000", stockQuantity = 5)
        addToCart(customerToken, productId, 1)

        couponRepository.save(
            Coupon(
                code = "ORDER10",
                discountType = DiscountType.PERCENTAGE,
                discountValue = BigDecimal("10"),
                startDate = Instant.now().minus(1, ChronoUnit.DAYS),
                endDate = Instant.now().plus(1, ChronoUnit.DAYS)
            )
        )

        val bodyWithCoupon = """
            {
                "recipientName":"Nguyen Van A","recipientPhone":"0900000000",
                "shippingAddressLine":"123 Le Loi","shippingWard":"Ben Nghe",
                "shippingDistrict":"District 1","shippingProvince":"Ho Chi Minh City",
                "paymentMethod":"COD","couponCode":"ORDER10"
            }
        """.trimIndent()

        mockMvc.perform(
            post("/api/orders")
                .header("Authorization", "Bearer $customerToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(bodyWithCoupon)
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.discountAmount").value(20000))

        val reloaded = couponRepository.findByCode("ORDER10").get()
        assertEquals(1, reloaded.currentUsage)
    }

    @Test
    fun `invalid status transition is rejected`() {
        val staffToken = tokenFor(RoleName.STAFF, "staff-ord4@example.com")
        val customerToken = tokenFor(RoleName.CUSTOMER, "cust-ord4@example.com")
        val productId = createProduct(staffToken, "100000", stockQuantity = 5)
        addToCart(customerToken, productId, 1)

        val result = mockMvc.perform(
            post("/api/orders")
                .header("Authorization", "Bearer $customerToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(checkoutBody)
        ).andExpect(status().isCreated).andReturn()
        val orderId = objectMapper.readTree(result.response.contentAsString).get("id").asText()

        mockMvc.perform(
            patch("/api/orders/$orderId/status")
                .header("Authorization", "Bearer $staffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"status":"SHIPPED"}""")
        ).andExpect(status().isConflict)
    }

    @Test
    fun `confirming an order moves stock from reserved to sold`() {
        val staffToken = tokenFor(RoleName.STAFF, "staff-ord5@example.com")
        val customerToken = tokenFor(RoleName.CUSTOMER, "cust-ord5@example.com")
        val productId = createProduct(staffToken, "100000", stockQuantity = 3)
        addToCart(customerToken, productId, 1)

        val result = mockMvc.perform(
            post("/api/orders")
                .header("Authorization", "Bearer $customerToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(checkoutBody)
        ).andExpect(status().isCreated).andReturn()
        val orderId = objectMapper.readTree(result.response.contentAsString).get("id").asText()

        mockMvc.perform(
            patch("/api/orders/$orderId/status")
                .header("Authorization", "Bearer $staffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"status":"CONFIRMED"}""")
        ).andExpect(status().isOk)

        mockMvc.perform(get("/api/products/$productId/inventory").header("Authorization", "Bearer $staffToken"))
            .andExpect(jsonPath("$.reservedStock").value(0))
            .andExpect(jsonPath("$.soldStock").value(1))
            .andExpect(jsonPath("$.currentStock").value(2))
    }

    @Test
    fun `a customer cannot view another customer's order`() {
        val staffToken = tokenFor(RoleName.STAFF, "staff-ord6@example.com")
        val ownerToken = tokenFor(RoleName.CUSTOMER, "cust-ord6-owner@example.com")
        val intruderToken = tokenFor(RoleName.CUSTOMER, "cust-ord6-intruder@example.com")
        val productId = createProduct(staffToken, "100000", stockQuantity = 5)
        addToCart(ownerToken, productId, 1)

        val result = mockMvc.perform(
            post("/api/orders")
                .header("Authorization", "Bearer $ownerToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(checkoutBody)
        ).andExpect(status().isCreated).andReturn()
        val orderId = objectMapper.readTree(result.response.contentAsString).get("id").asText()

        mockMvc.perform(get("/api/orders/$orderId").header("Authorization", "Bearer $intruderToken"))
            .andExpect(status().isNotFound)

        mockMvc.perform(get("/api/orders/$orderId").header("Authorization", "Bearer $ownerToken"))
            .andExpect(status().isOk)
    }

    /**
     * The core guarantee from spec section 20: a stock=1 unique item checked out by two
     * customers at the same moment must succeed for exactly one of them. This exercises
     * the pessimistic row lock in InventoryService.reserveStock() across two genuinely
     * concurrent database transactions (each MockMvc call here runs in its own request
     * transaction, not the test method's).
     */
    @Test
    fun `concurrent checkout of a stock-1 item succeeds for exactly one customer`() {
        val staffToken = tokenFor(RoleName.STAFF, "staff-ord7@example.com")
        val customerAToken = tokenFor(RoleName.CUSTOMER, "cust-ord7-a@example.com")
        val customerBToken = tokenFor(RoleName.CUSTOMER, "cust-ord7-b@example.com")
        val productId = createProduct(staffToken, "1000000", stockQuantity = 1)

        addToCart(customerAToken, productId, 1)
        addToCart(customerBToken, productId, 1)

        val startLatch = CountDownLatch(1)
        val executor = Executors.newFixedThreadPool(2)

        fun taskFor(token: String): Callable<Int> = Callable {
            startLatch.await()
            mockMvc.perform(
                post("/api/orders")
                    .header("Authorization", "Bearer $token")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(checkoutBody)
            ).andReturn().response.status
        }

        val futureA = executor.submit(taskFor(customerAToken))
        val futureB = executor.submit(taskFor(customerBToken))

        startLatch.countDown()
        val statusA = futureA.get(10, TimeUnit.SECONDS)
        val statusB = futureB.get(10, TimeUnit.SECONDS)
        executor.shutdown()

        val statuses = listOf(statusA, statusB)
        assertEquals(1, statuses.count { it == 201 }, "Exactly one checkout must succeed (got: $statuses)")
        assertEquals(1, statuses.count { it == 409 }, "Exactly one checkout must fail with 409 Conflict (got: $statuses)")

        mockMvc.perform(get("/api/products/$productId/inventory").header("Authorization", "Bearer $staffToken"))
            .andExpect(jsonPath("$.availableStock").value(0))
            .andExpect(jsonPath("$.reservedStock").value(1))
    }
}
