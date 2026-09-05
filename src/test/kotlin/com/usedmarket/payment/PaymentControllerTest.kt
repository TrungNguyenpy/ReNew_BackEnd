package com.usedmarket.payment

import com.fasterxml.jackson.databind.ObjectMapper
import com.usedmarket.catalog.entity.Brand
import com.usedmarket.catalog.entity.Category
import com.usedmarket.catalog.repository.BrandRepository
import com.usedmarket.catalog.repository.CategoryRepository
import com.usedmarket.product.dto.ProductCreateRequest
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
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PaymentControllerTest {

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

    private fun createProduct(staffToken: String, price: String): String {
        val category = categoryRepository.save(Category(name = "Cat ${System.nanoTime()}", slug = "cat-${System.nanoTime()}"))
        val brand = brandRepository.save(Brand(name = "Brand ${System.nanoTime()}", slug = "brand-${System.nanoTime()}"))
        val request = ProductCreateRequest(
            name = "Payment Test Product",
            slug = "pay-test-${System.nanoTime()}",
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
        return objectMapper.readTree(result.response.contentAsString).get("id").asText()
    }

    private fun checkout(customerToken: String, productId: String, paymentMethod: String): String {
        mockMvc.perform(
            post("/api/cart/items")
                .header("Authorization", "Bearer $customerToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"productId":"$productId","quantity":1}""")
        ).andExpect(status().isOk)

        val body = """
            {
                "recipientName":"Nguyen Van A","recipientPhone":"0900000000",
                "shippingAddressLine":"123 Le Loi","shippingWard":"Ben Nghe",
                "shippingDistrict":"District 1","shippingProvince":"Ho Chi Minh City",
                "paymentMethod":"$paymentMethod"
            }
        """.trimIndent()

        val result = mockMvc.perform(
            post("/api/orders")
                .header("Authorization", "Bearer $customerToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        ).andExpect(status().isCreated).andReturn()
        return objectMapper.readTree(result.response.contentAsString).get("id").asText()
    }

    private fun advanceStatus(orderId: String, staffToken: String, newStatus: String) {
        mockMvc.perform(
            patch("/api/orders/$orderId/status")
                .header("Authorization", "Bearer $staffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"status":"$newStatus"}""")
        ).andExpect(status().isOk)
    }

    @Test
    fun `Stripe intent cannot be created for a COD order`() {
        val staffToken = tokenFor(RoleName.STAFF, "staff-pay1@example.com")
        val customerToken = tokenFor(RoleName.CUSTOMER, "cust-pay1@example.com")
        val productId = createProduct(staffToken, "100000")
        val orderId = checkout(customerToken, productId, "COD")

        mockMvc.perform(
            post("/api/orders/$orderId/payment/intent").header("Authorization", "Bearer $customerToken")
        ).andExpect(status().isBadRequest)
    }

    @Test
    fun `a customer cannot view another customer's payment`() {
        val staffToken = tokenFor(RoleName.STAFF, "staff-pay2@example.com")
        val ownerToken = tokenFor(RoleName.CUSTOMER, "cust-pay2-owner@example.com")
        val intruderToken = tokenFor(RoleName.CUSTOMER, "cust-pay2-intruder@example.com")
        val productId = createProduct(staffToken, "100000")
        val orderId = checkout(ownerToken, productId, "COD")

        mockMvc.perform(get("/api/orders/$orderId/payment").header("Authorization", "Bearer $intruderToken"))
            .andExpect(status().isNotFound)

        mockMvc.perform(get("/api/orders/$orderId/payment").header("Authorization", "Bearer $ownerToken"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("PENDING"))
    }

    @Test
    fun `COD payment is only marked SUCCEEDED once the order is actually delivered`() {
        val staffToken = tokenFor(RoleName.STAFF, "staff-pay3@example.com")
        val customerToken = tokenFor(RoleName.CUSTOMER, "cust-pay3@example.com")
        val productId = createProduct(staffToken, "100000")
        val orderId = checkout(customerToken, productId, "COD")

        advanceStatus(orderId, staffToken, "CONFIRMED")
        mockMvc.perform(get("/api/orders/$orderId/payment").header("Authorization", "Bearer $customerToken"))
            .andExpect(jsonPath("$.status").value("PENDING"))

        advanceStatus(orderId, staffToken, "PROCESSING")
        advanceStatus(orderId, staffToken, "PACKED")
        advanceStatus(orderId, staffToken, "SHIPPED")
        mockMvc.perform(get("/api/orders/$orderId/payment").header("Authorization", "Bearer $customerToken"))
            .andExpect(jsonPath("$.status").value("PENDING"))

        advanceStatus(orderId, staffToken, "DELIVERED")
        mockMvc.perform(get("/api/orders/$orderId/payment").header("Authorization", "Bearer $customerToken"))
            .andExpect(jsonPath("$.status").value("SUCCEEDED"))
            .andExpect(jsonPath("$.paidAt").exists())

        mockMvc.perform(get("/api/orders/$orderId").header("Authorization", "Bearer $customerToken"))
            .andExpect(jsonPath("$.paymentStatus").value("SUCCEEDED"))
    }

    @Test
    fun `Stripe webhook with an invalid signature is rejected`() {
        mockMvc.perform(
            post("/api/webhooks/stripe")
                .header("Stripe-Signature", "t=12345,v1=not-a-real-signature")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"id":"evt_test","type":"payment_intent.succeeded"}""")
        ).andExpect(status().isBadRequest)
    }

    @Test
    fun `Stripe webhook endpoint does not require a JWT`() {
        // Missing/invalid signature still yields 400 (not 401) — proving the endpoint
        // is reachable without authentication, as required for a real Stripe callback.
        mockMvc.perform(
            post("/api/webhooks/stripe")
                .header("Stripe-Signature", "invalid")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}")
        ).andExpect(status().isBadRequest)
    }
}
