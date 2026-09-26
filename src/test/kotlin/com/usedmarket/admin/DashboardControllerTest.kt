package com.usedmarket.admin

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.usedmarket.catalog.entity.Brand
import com.usedmarket.catalog.entity.Category
import com.usedmarket.catalog.repository.BrandRepository
import com.usedmarket.catalog.repository.CategoryRepository
import com.usedmarket.order.entity.OrderStatus
import com.usedmarket.product.dto.ProductCreateRequest
import com.usedmarket.product.entity.ConditionGrade
import com.usedmarket.security.JwtService
import com.usedmarket.user.entity.RoleName
import com.usedmarket.user.entity.User
import com.usedmarket.user.repository.UserRepository
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
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
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class DashboardControllerTest {

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
    lateinit var passwordEncoder: PasswordEncoder

    @Autowired
    lateinit var jwtService: JwtService

    private val zone: ZoneId = ZoneId.of("Asia/Ho_Chi_Minh")

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

    private fun createProduct(
        actorToken: String,
        name: String,
        price: String,
        stockQuantity: Int,
        categoryName: String
    ): Pair<String, String> {
        val category = categoryRepository.save(
            Category(name = categoryName, slug = "dash-cat-${System.nanoTime()}")
        )
        val brand = brandRepository.save(
            Brand(name = "Dash Brand ${System.nanoTime()}", slug = "dash-brand-${System.nanoTime()}")
        )
        val request = ProductCreateRequest(
            name = name,
            slug = "dash-product-${System.nanoTime()}",
            categoryId = category.id!!,
            brandId = brand.id!!,
            price = BigDecimal(price),
            condition = ConditionGrade.GOOD,
            stockQuantity = stockQuantity
        )
        val result = mockMvc.perform(
            post("/api/products")
                .header("Authorization", "Bearer $actorToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        ).andExpect(status().isCreated).andReturn()
        val id = objectMapper.readTree(result.response.contentAsString).get("id").asText()
        return id to categoryName
    }

    private fun checkoutAndConfirm(
        customerToken: String,
        staffOrAdminToken: String,
        productId: String,
        quantity: Int
    ): BigDecimal {
        mockMvc.perform(
            post("/api/cart/items")
                .header("Authorization", "Bearer $customerToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"productId":"$productId","quantity":$quantity}""")
        ).andExpect(status().isOk)

        val result = mockMvc.perform(
            post("/api/orders")
                .header("Authorization", "Bearer $customerToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(checkoutBody)
        ).andExpect(status().isCreated).andReturn()
        val body = objectMapper.readTree(result.response.contentAsString)
        val orderId = body.get("id").asText()
        val totalAmount = BigDecimal(body.get("totalAmount").asText())

        mockMvc.perform(
            patch("/api/orders/$orderId/status")
                .header("Authorization", "Bearer $staffOrAdminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"status":"CONFIRMED"}""")
        ).andExpect(status().isOk)

        return totalAmount
    }

    private fun getJson(path: String, adminToken: String): JsonNode {
        val result = mockMvc.perform(get(path).header("Authorization", "Bearer $adminToken"))
            .andExpect(status().isOk)
            .andReturn()
        return objectMapper.readTree(result.response.contentAsString)
    }

    private fun money(node: JsonNode): BigDecimal = BigDecimal(node.asText())

    @Test
    fun `staff and customer cannot access the admin dashboard`() {
        val staffToken = tokenFor(RoleName.STAFF, "staff-dash-rbac@example.com")
        val customerToken = tokenFor(RoleName.CUSTOMER, "cust-dash-rbac@example.com")

        mockMvc.perform(get("/api/admin/dashboard/summary"))
            .andExpect(status().isUnauthorized)

        val paths = listOf(
            "/api/admin/dashboard/summary",
            "/api/admin/dashboard/revenue-chart",
            "/api/admin/dashboard/orders-by-status",
            "/api/admin/dashboard/best-selling",
            "/api/admin/dashboard/revenue-by-category"
        )
        for (path in paths) {
            mockMvc.perform(get(path).header("Authorization", "Bearer $staffToken"))
                .andExpect(status().isForbidden)
            mockMvc.perform(get(path).header("Authorization", "Bearer $customerToken"))
                .andExpect(status().isForbidden)
        }
    }

    @Test
    fun `admin sees revenue reflected in the summary after an order is confirmed`() {
        val adminToken = tokenFor(RoleName.ADMIN, "admin-dash-sum@example.com")
        val customerToken = tokenFor(RoleName.CUSTOMER, "cust-dash-sum@example.com")
        val productId = createProduct(adminToken, "Dash Summary Item", "100000", 5, "Dash Sum Cat ${System.nanoTime()}").first

        val before = getJson("/api/admin/dashboard/summary", adminToken)
        val beforeRevenue = money(before.get("totalRevenue"))
        val beforeToday = money(before.get("revenueToday"))
        val beforeMonth = money(before.get("revenueThisMonth"))
        val beforeOrders = before.get("totalOrders").asLong()
        val beforePending = before.get("pendingOrders").asLong()
        val beforeCustomers = before.get("totalCustomers").asLong()
        val beforeProducts = before.get("totalProducts").asLong()

        val orderTotal = checkoutAndConfirm(customerToken, adminToken, productId, 1)

        val after = getJson("/api/admin/dashboard/summary", adminToken)
        assertEquals(0, money(after.get("totalRevenue")).compareTo(beforeRevenue.add(orderTotal)))
        assertEquals(0, money(after.get("revenueToday")).compareTo(beforeToday.add(orderTotal)))
        assertEquals(0, money(after.get("revenueThisMonth")).compareTo(beforeMonth.add(orderTotal)))
        assertEquals(beforeOrders + 1, after.get("totalOrders").asLong())
        assertEquals(beforePending, after.get("pendingOrders").asLong())
        assertEquals(beforeCustomers, after.get("totalCustomers").asLong())
        assertEquals(beforeProducts, after.get("totalProducts").asLong())
    }

    @Test
    fun `revenue chart returns a continuous series of the requested length`() {
        val adminToken = tokenFor(RoleName.ADMIN, "admin-dash-chart@example.com")
        val customerToken = tokenFor(RoleName.CUSTOMER, "cust-dash-chart@example.com")
        val productId = createProduct(adminToken, "Dash Chart Item", "100000", 5, "Dash Chart Cat ${System.nanoTime()}").first

        mockMvc.perform(
            get("/api/admin/dashboard/revenue-chart")
                .param("period", "daily")
                .param("limit", "7")
                .header("Authorization", "Bearer $adminToken")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(7))

        mockMvc.perform(
            get("/api/admin/dashboard/revenue-chart")
                .param("period", "monthly")
                .param("limit", "3")
                .header("Authorization", "Bearer $adminToken")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(3))
            .andExpect(jsonPath("$[2].period").value(YearMonth.now(zone).toString()))

        val today = LocalDate.now(zone).format(DateTimeFormatter.ISO_LOCAL_DATE)
        val beforeToday = money(getJson("/api/admin/dashboard/revenue-chart?period=daily&limit=1", adminToken).get(0).get("revenue"))
        val orderTotal = checkoutAndConfirm(customerToken, adminToken, productId, 1)
        val afterToday = getJson("/api/admin/dashboard/revenue-chart?period=daily&limit=1", adminToken)

        assertEquals(today, afterToday.get(0).get("period").asText())
        assertEquals(0, money(afterToday.get(0).get("revenue")).compareTo(beforeToday.add(orderTotal)))
        assertTrue(afterToday.get(0).get("orderCount").asLong() >= 1)
    }

    @Test
    fun `an invalid chart period is rejected`() {
        val adminToken = tokenFor(RoleName.ADMIN, "admin-dash-period@example.com")

        mockMvc.perform(
            get("/api/admin/dashboard/revenue-chart")
                .param("period", "yearly")
                .header("Authorization", "Bearer $adminToken")
        ).andExpect(status().isBadRequest)

        mockMvc.perform(
            get("/api/admin/dashboard/revenue-chart")
                .param("period", "weekly")
                .header("Authorization", "Bearer $adminToken")
        ).andExpect(status().isBadRequest)
    }

    @Test
    fun `orders-by-status covers every status value`() {
        val adminToken = tokenFor(RoleName.ADMIN, "admin-dash-status@example.com")
        val customerToken = tokenFor(RoleName.CUSTOMER, "cust-dash-status@example.com")
        val productId = createProduct(adminToken, "Dash Status Item", "100000", 5, "Dash Status Cat ${System.nanoTime()}").first
        checkoutAndConfirm(customerToken, adminToken, productId, 1)

        val body = getJson("/api/admin/dashboard/orders-by-status", adminToken)
        assertEquals(OrderStatus.entries.size, body.size())

        val statuses = mutableSetOf<String>()
        var confirmedCount = 0L
        for (row in body) {
            statuses.add(row.get("status").asText())
            if (row.get("status").asText() == "CONFIRMED") {
                confirmedCount = row.get("count").asLong()
            }
        }
        assertEquals(OrderStatus.entries.map { it.name }.toSet(), statuses)
        assertTrue(confirmedCount >= 1)
    }

    @Test
    fun `best-selling ranks the product with more units sold first`() {
        val adminToken = tokenFor(RoleName.ADMIN, "admin-dash-best@example.com")
        val customerA = tokenFor(RoleName.CUSTOMER, "cust-dash-best-a@example.com")
        val customerB = tokenFor(RoleName.CUSTOMER, "cust-dash-best-b@example.com")

        val popularId = createProduct(adminToken, "Very Popular Dash Item", "100000", 50, "Dash Best Cat A ${System.nanoTime()}").first
        val nicheId = createProduct(adminToken, "Niche Dash Item", "100000", 50, "Dash Best Cat B ${System.nanoTime()}").first

        checkoutAndConfirm(customerA, adminToken, popularId, 12)
        checkoutAndConfirm(customerB, adminToken, nicheId, 3)

        val body = getJson("/api/admin/dashboard/best-selling?limit=50", adminToken)
        var popularQty = -1L
        var nicheQty = -1L
        var popularIndex = -1
        var nicheIndex = -1
        for (i in 0 until body.size()) {
            val row = body.get(i)
            val id = row.get("productId").asText()
            if (id == popularId) {
                popularQty = row.get("totalQuantitySold").asLong()
                popularIndex = i
            }
            if (id == nicheId) {
                nicheQty = row.get("totalQuantitySold").asLong()
                nicheIndex = i
            }
        }
        assertTrue(popularIndex >= 0 && nicheIndex >= 0)
        assertTrue(popularQty >= 12)
        assertTrue(nicheQty >= 3)
        assertTrue(popularQty > nicheQty)
        assertTrue(popularIndex < nicheIndex)

        mockMvc.perform(
            get("/api/admin/dashboard/best-selling")
                .param("limit", "5")
                .header("Authorization", "Bearer $adminToken")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$[0].totalQuantitySold").isNumber)
    }

    @Test
    fun `revenue-by-category is accessible and well-formed`() {
        val adminToken = tokenFor(RoleName.ADMIN, "admin-dash-cat@example.com")
        val customerToken = tokenFor(RoleName.CUSTOMER, "cust-dash-cat@example.com")
        val catA = "Dash Electronics ${System.nanoTime()}"
        val catB = "Dash Appliances ${System.nanoTime()}"
        val productA = createProduct(adminToken, "Dash Cat A Item", "100000", 10, catA).first
        val productB = createProduct(adminToken, "Dash Cat B Item", "200000", 10, catB).first

        checkoutAndConfirm(customerToken, adminToken, productA, 1)
        checkoutAndConfirm(customerToken, adminToken, productB, 1)

        val body = getJson("/api/admin/dashboard/revenue-by-category", adminToken)
        val names = mutableSetOf<String>()
        for (row in body) {
            names.add(row.get("categoryName").asText())
            assertTrue(row.has("categoryId"))
            assertTrue(row.has("totalRevenue"))
            assertTrue(money(row.get("totalRevenue")) >= BigDecimal.ZERO)
        }
        assertTrue(names.contains(catA), "expected $catA in $names")
        assertTrue(names.contains(catB), "expected $catB in $names")
    }
}
