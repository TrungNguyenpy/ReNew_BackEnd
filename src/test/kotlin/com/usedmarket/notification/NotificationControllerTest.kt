package com.usedmarket.notification

import com.fasterxml.jackson.databind.ObjectMapper
import com.usedmarket.catalog.entity.Brand
import com.usedmarket.catalog.entity.Category
import com.usedmarket.catalog.repository.BrandRepository
import com.usedmarket.catalog.repository.CategoryRepository
import com.usedmarket.product.dto.ProductCreateRequest
import com.usedmarket.product.dto.ProductUpdateRequest
import com.usedmarket.product.entity.ConditionGrade
import com.usedmarket.security.JwtService
import com.usedmarket.user.entity.RoleName
import com.usedmarket.user.entity.User
import com.usedmarket.user.repository.UserRepository
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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class NotificationControllerTest {

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

    private data class CreatedProduct(val id: String, val categoryId: UUID, val brandId: UUID)

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

    private fun createProduct(staffToken: String, price: String): CreatedProduct {
        val category = categoryRepository.save(Category(name = "Cat ${System.nanoTime()}", slug = "cat-${System.nanoTime()}"))
        val brand = brandRepository.save(Brand(name = "Brand ${System.nanoTime()}", slug = "brand-${System.nanoTime()}"))
        val request = ProductCreateRequest(
            name = "Notif Test Product",
            slug = "notif-test-${System.nanoTime()}",
            categoryId = category.id!!,
            brandId = brand.id!!,
            price = BigDecimal(price),
            condition = ConditionGrade.GOOD,
            stockQuantity = 5
        )
        val result = mockMvc.perform(
            post("/api/products")
                .header("Authorization", "Bearer $staffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        ).andExpect(status().isCreated).andReturn()
        val id = objectMapper.readTree(result.response.contentAsString).get("id").asText()
        return CreatedProduct(id, category.id!!, brand.id!!)
    }

    @Test
    fun `checkout and status transitions generate the expected notifications`() {
        val staffToken = tokenFor(RoleName.STAFF, "staff-notif1@example.com")
        val customerToken = tokenFor(RoleName.CUSTOMER, "cust-notif1@example.com")
        val product = createProduct(staffToken, "100000")

        mockMvc.perform(
            post("/api/cart/items")
                .header("Authorization", "Bearer $customerToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"productId":"${product.id}","quantity":1}""")
        ).andExpect(status().isOk)

        val checkoutBody = """
            {
                "recipientName":"Nguyen Van A","recipientPhone":"0900000000",
                "shippingAddressLine":"123 Le Loi","shippingWard":"Ben Nghe",
                "shippingDistrict":"District 1","shippingProvince":"Ho Chi Minh City",
                "paymentMethod":"COD"
            }
        """.trimIndent()

        val orderResult = mockMvc.perform(
            post("/api/orders")
                .header("Authorization", "Bearer $customerToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(checkoutBody)
        ).andExpect(status().isCreated).andReturn()
        val orderId = objectMapper.readTree(orderResult.response.contentAsString).get("id").asText()

        mockMvc.perform(get("/api/notifications").header("Authorization", "Bearer $customerToken"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content[0].type").value("ORDER_CREATED"))

        mockMvc.perform(
            patch("/api/orders/$orderId/status")
                .header("Authorization", "Bearer $staffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"status":"CONFIRMED"}""")
        ).andExpect(status().isOk)

        mockMvc.perform(get("/api/notifications").header("Authorization", "Bearer $customerToken"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content[0].type").value("ORDER_CONFIRMED"))

        mockMvc.perform(get("/api/notifications/unread-count").header("Authorization", "Bearer $customerToken"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.unreadCount").value(2))
    }

    @Test
    fun `marking a notification as read updates its status and the unread count`() {
        val staffToken = tokenFor(RoleName.STAFF, "staff-notif2@example.com")
        val customerToken = tokenFor(RoleName.CUSTOMER, "cust-notif2@example.com")
        val product = createProduct(staffToken, "100000")

        mockMvc.perform(
            post("/api/cart/items")
                .header("Authorization", "Bearer $customerToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"productId":"${product.id}","quantity":1}""")
        ).andExpect(status().isOk)

        val checkoutBody = """
            {
                "recipientName":"Nguyen Van A","recipientPhone":"0900000000",
                "shippingAddressLine":"123 Le Loi","shippingWard":"Ben Nghe",
                "shippingDistrict":"District 1","shippingProvince":"Ho Chi Minh City",
                "paymentMethod":"COD"
            }
        """.trimIndent()

        mockMvc.perform(
            post("/api/orders")
                .header("Authorization", "Bearer $customerToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(checkoutBody)
        ).andExpect(status().isCreated)

        val listResult = mockMvc.perform(get("/api/notifications").header("Authorization", "Bearer $customerToken"))
            .andExpect(status().isOk).andReturn()
        val notificationId = objectMapper.readTree(listResult.response.contentAsString)
            .get("content").get(0).get("id").asText()

        mockMvc.perform(
            patch("/api/notifications/$notificationId/read").header("Authorization", "Bearer $customerToken")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.isRead").value(true))

        mockMvc.perform(get("/api/notifications/unread-count").header("Authorization", "Bearer $customerToken"))
            .andExpect(jsonPath("$.unreadCount").value(0))
    }

    @Test
    fun `a wishlist price drop notifies everyone watching the product`() {
        val staffToken = tokenFor(RoleName.STAFF, "staff-notif3@example.com")
        val customerToken = tokenFor(RoleName.CUSTOMER, "cust-notif3@example.com")
        val product = createProduct(staffToken, "300000")

        mockMvc.perform(post("/api/wishlist/${product.id}").header("Authorization", "Bearer $customerToken"))
            .andExpect(status().isOk)

        val updateRequest = ProductUpdateRequest(
            name = "Notif Test Product",
            slug = "notif-updated-${System.nanoTime()}",
            categoryId = product.categoryId,
            brandId = product.brandId,
            price = BigDecimal("200000"),
            condition = ConditionGrade.GOOD
        )

        mockMvc.perform(
            put("/api/products/${product.id}")
                .header("Authorization", "Bearer $staffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(updateRequest))
        ).andExpect(status().isOk)

        mockMvc.perform(get("/api/notifications").header("Authorization", "Bearer $customerToken"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content[0].type").value("WISHLIST_PRICE_DROP"))
    }

    @Test
    fun `a customer cannot mark another customer's notification as read`() {
        val staffToken = tokenFor(RoleName.STAFF, "staff-notif4@example.com")
        val ownerToken = tokenFor(RoleName.CUSTOMER, "cust-notif4-owner@example.com")
        val intruderToken = tokenFor(RoleName.CUSTOMER, "cust-notif4-intruder@example.com")
        val product = createProduct(staffToken, "100000")

        mockMvc.perform(
            post("/api/cart/items")
                .header("Authorization", "Bearer $ownerToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"productId":"${product.id}","quantity":1}""")
        ).andExpect(status().isOk)

        val checkoutBody = """
            {
                "recipientName":"Nguyen Van A","recipientPhone":"0900000000",
                "shippingAddressLine":"123 Le Loi","shippingWard":"Ben Nghe",
                "shippingDistrict":"District 1","shippingProvince":"Ho Chi Minh City",
                "paymentMethod":"COD"
            }
        """.trimIndent()

        mockMvc.perform(
            post("/api/orders")
                .header("Authorization", "Bearer $ownerToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(checkoutBody)
        ).andExpect(status().isCreated)

        val listResult = mockMvc.perform(get("/api/notifications").header("Authorization", "Bearer $ownerToken"))
            .andExpect(status().isOk).andReturn()
        val notificationId = objectMapper.readTree(listResult.response.contentAsString)
            .get("content").get(0).get("id").asText()

        mockMvc.perform(
            patch("/api/notifications/$notificationId/read").header("Authorization", "Bearer $intruderToken")
        ).andExpect(status().isForbidden)
    }
}
