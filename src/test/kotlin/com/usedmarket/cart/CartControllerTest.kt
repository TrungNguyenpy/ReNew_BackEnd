package com.usedmarket.cart

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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal
import java.time.Instant
import java.time.temporal.ChronoUnit

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CartControllerTest {

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
            name = "Cart Test Product",
            slug = "cart-test-${System.nanoTime()}",
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

    @Test
    fun `adding the same product twice accumulates quantity and computes subtotal`() {
        val staffToken = tokenFor(RoleName.STAFF, "staff-cart1@example.com")
        val customerToken = tokenFor(RoleName.CUSTOMER, "cust-cart1@example.com")
        val productId = createProduct(staffToken, "100000", stockQuantity = 10)

        mockMvc.perform(
            post("/api/cart/items")
                .header("Authorization", "Bearer $customerToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"productId":"$productId","quantity":2}""")
        ).andExpect(status().isOk)

        mockMvc.perform(
            post("/api/cart/items")
                .header("Authorization", "Bearer $customerToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"productId":"$productId","quantity":1}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.items.length()").value(1))
            .andExpect(jsonPath("$.items[0].quantity").value(3))
            .andExpect(jsonPath("$.subtotal").value(300000))
    }

    @Test
    fun `adding more than available stock is rejected`() {
        val staffToken = tokenFor(RoleName.STAFF, "staff-cart2@example.com")
        val customerToken = tokenFor(RoleName.CUSTOMER, "cust-cart2@example.com")
        val productId = createProduct(staffToken, "100000", stockQuantity = 2)

        mockMvc.perform(
            post("/api/cart/items")
                .header("Authorization", "Bearer $customerToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"productId":"$productId","quantity":5}""")
        ).andExpect(status().isConflict)
    }

    @Test
    fun `shipping fee is flat below threshold and free above it`() {
        val staffToken = tokenFor(RoleName.STAFF, "staff-cart3@example.com")
        val customerToken = tokenFor(RoleName.CUSTOMER, "cust-cart3@example.com")

        val cheapProductId = createProduct(staffToken, "100000", stockQuantity = 5)
        mockMvc.perform(
            post("/api/cart/items")
                .header("Authorization", "Bearer $customerToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"productId":"$cheapProductId","quantity":1}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.shippingFee").value(30000))

        // Push subtotal above the free-shipping threshold (500000).
        mockMvc.perform(
            put("/api/cart/items/$cheapProductId")
                .header("Authorization", "Bearer $customerToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"quantity":5}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.shippingFee").value(0))
    }

    @Test
    fun `removing an item takes it out of the cart`() {
        val staffToken = tokenFor(RoleName.STAFF, "staff-cart4@example.com")
        val customerToken = tokenFor(RoleName.CUSTOMER, "cust-cart4@example.com")
        val productId = createProduct(staffToken, "50000", stockQuantity = 5)

        mockMvc.perform(
            post("/api/cart/items")
                .header("Authorization", "Bearer $customerToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"productId":"$productId","quantity":1}""")
        ).andExpect(status().isOk)

        mockMvc.perform(
            delete("/api/cart/items/$productId").header("Authorization", "Bearer $customerToken")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.items.length()").value(0))
            .andExpect(jsonPath("$.subtotal").value(0))
    }

    @Test
    fun `applying a valid percentage coupon computes the discount without consuming usage`() {
        val staffToken = tokenFor(RoleName.STAFF, "staff-cart5@example.com")
        val customerToken = tokenFor(RoleName.CUSTOMER, "cust-cart5@example.com")
        val productId = createProduct(staffToken, "200000", stockQuantity = 5)

        mockMvc.perform(
            post("/api/cart/items")
                .header("Authorization", "Bearer $customerToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"productId":"$productId","quantity":1}""")
        ).andExpect(status().isOk)

        couponRepository.save(
            Coupon(
                code = "SAVE10",
                discountType = DiscountType.PERCENTAGE,
                discountValue = BigDecimal("10"),
                startDate = Instant.now().minus(1, ChronoUnit.DAYS),
                endDate = Instant.now().plus(1, ChronoUnit.DAYS)
            )
        )

        mockMvc.perform(
            post("/api/cart/coupon")
                .header("Authorization", "Bearer $customerToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"couponCode":"SAVE10"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.discountAmount").value(20000))
            .andExpect(jsonPath("$.appliedCouponCode").value("SAVE10"))

        // Preview must NOT have consumed a usage slot — the coupon's currentUsage stays untouched.
        val reloaded = couponRepository.findByCode("SAVE10").get()
        assertEquals(0, reloaded.currentUsage)
    }

    @Test
    fun `expired coupon is rejected`() {
        val staffToken = tokenFor(RoleName.STAFF, "staff-cart6@example.com")
        val customerToken = tokenFor(RoleName.CUSTOMER, "cust-cart6@example.com")
        val productId = createProduct(staffToken, "200000", stockQuantity = 5)

        mockMvc.perform(
            post("/api/cart/items")
                .header("Authorization", "Bearer $customerToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"productId":"$productId","quantity":1}""")
        ).andExpect(status().isOk)

        couponRepository.save(
            Coupon(
                code = "EXPIRED5",
                discountType = DiscountType.FIXED_AMOUNT,
                discountValue = BigDecimal("5000"),
                startDate = Instant.now().minus(10, ChronoUnit.DAYS),
                endDate = Instant.now().minus(1, ChronoUnit.DAYS)
            )
        )

        mockMvc.perform(
            post("/api/cart/coupon")
                .header("Authorization", "Bearer $customerToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"couponCode":"EXPIRED5"}""")
        ).andExpect(status().isBadRequest)
    }
}
