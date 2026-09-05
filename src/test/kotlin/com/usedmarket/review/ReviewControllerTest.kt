package com.usedmarket.review

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
class ReviewControllerTest {

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

    private fun createProduct(staffToken: String): String {
        val category = categoryRepository.save(Category(name = "Cat ${System.nanoTime()}", slug = "cat-${System.nanoTime()}"))
        val brand = brandRepository.save(Brand(name = "Brand ${System.nanoTime()}", slug = "brand-${System.nanoTime()}"))
        val request = ProductCreateRequest(
            name = "Review Test Product",
            slug = "review-test-${System.nanoTime()}",
            categoryId = category.id!!,
            brandId = brand.id!!,
            price = BigDecimal("100000"),
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

    private fun checkoutAndMaybeDeliver(customerToken: String, staffToken: String, productId: String, deliver: Boolean): String {
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
                "paymentMethod":"COD"
            }
        """.trimIndent()

        val result = mockMvc.perform(
            post("/api/orders")
                .header("Authorization", "Bearer $customerToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        ).andExpect(status().isCreated).andReturn()
        val orderId = objectMapper.readTree(result.response.contentAsString).get("id").asText()

        if (deliver) {
            for (nextStatus in listOf("CONFIRMED", "PROCESSING", "PACKED", "SHIPPED", "DELIVERED")) {
                mockMvc.perform(
                    patch("/api/orders/$orderId/status")
                        .header("Authorization", "Bearer $staffToken")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""{"status":"$nextStatus"}""")
                ).andExpect(status().isOk)
            }
        }
        return orderId
    }

    @Test
    fun `cannot review before the order is delivered`() {
        val staffToken = tokenFor(RoleName.STAFF, "staff-rev1@example.com")
        val customerToken = tokenFor(RoleName.CUSTOMER, "cust-rev1@example.com")
        val productId = createProduct(staffToken)
        val orderId = checkoutAndMaybeDeliver(customerToken, staffToken, productId, deliver = false)

        mockMvc.perform(
            post("/api/products/$productId/reviews")
                .header("Authorization", "Bearer $customerToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"orderId":"$orderId","rating":5,"comment":"Great!"}""")
        ).andExpect(status().isBadRequest)
    }

    @Test
    fun `can review after delivery, and duplicate review is rejected`() {
        val staffToken = tokenFor(RoleName.STAFF, "staff-rev2@example.com")
        val customerToken = tokenFor(RoleName.CUSTOMER, "cust-rev2@example.com")
        val productId = createProduct(staffToken)
        val orderId = checkoutAndMaybeDeliver(customerToken, staffToken, productId, deliver = true)

        mockMvc.perform(
            post("/api/products/$productId/reviews")
                .header("Authorization", "Bearer $customerToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"orderId":"$orderId","rating":4,"comment":"Pretty good","deliveryRating":5}""")
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.rating").value(4))
            .andExpect(jsonPath("$.deliveryRating").value(5))

        mockMvc.perform(
            post("/api/products/$productId/reviews")
                .header("Authorization", "Bearer $customerToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"orderId":"$orderId","rating":3}""")
        ).andExpect(status().isConflict)

        mockMvc.perform(get("/api/products/$productId/reviews/summary"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.totalReviews").value(1))
            .andExpect(jsonPath("$.averageRating").value(4.0))
    }

    @Test
    fun `cannot review using someone else's order`() {
        val staffToken = tokenFor(RoleName.STAFF, "staff-rev3@example.com")
        val ownerToken = tokenFor(RoleName.CUSTOMER, "cust-rev3-owner@example.com")
        val intruderToken = tokenFor(RoleName.CUSTOMER, "cust-rev3-intruder@example.com")
        val productId = createProduct(staffToken)
        val orderId = checkoutAndMaybeDeliver(ownerToken, staffToken, productId, deliver = true)

        mockMvc.perform(
            post("/api/products/$productId/reviews")
                .header("Authorization", "Bearer $intruderToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"orderId":"$orderId","rating":5}""")
        ).andExpect(status().isForbidden)
    }

    @Test
    fun `staff can reply to a review`() {
        val staffToken = tokenFor(RoleName.STAFF, "staff-rev4@example.com")
        val customerToken = tokenFor(RoleName.CUSTOMER, "cust-rev4@example.com")
        val productId = createProduct(staffToken)
        val orderId = checkoutAndMaybeDeliver(customerToken, staffToken, productId, deliver = true)

        val result = mockMvc.perform(
            post("/api/products/$productId/reviews")
                .header("Authorization", "Bearer $customerToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"orderId":"$orderId","rating":5}""")
        ).andExpect(status().isCreated).andReturn()
        val reviewId = objectMapper.readTree(result.response.contentAsString).get("id").asText()

        mockMvc.perform(
            patch("/api/products/$productId/reviews/$reviewId/reply")
                .header("Authorization", "Bearer $staffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"sellerReply":"Thank you for your feedback!"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.sellerReply").value("Thank you for your feedback!"))
    }
}
