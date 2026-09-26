package com.usedmarket.finalcheck

import com.fasterxml.jackson.databind.ObjectMapper
import com.usedmarket.auth.repository.RefreshTokenRepository
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
import java.security.MessageDigest
import java.time.Instant
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FinalConsolidationTest {

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

    @Autowired
    lateinit var refreshTokenRepository: RefreshTokenRepository

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

    /** Mirrors AuthService's private hashToken() exactly, so the test can look up the stored row by its raw token. */
    private fun hashToken(rawToken: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(rawToken.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    private fun createProduct(staffToken: String, stockQuantity: Int): String {
        val category = categoryRepository.save(Category(name = "Cat ${System.nanoTime()}", slug = "cat-${System.nanoTime()}"))
        val brand = brandRepository.save(Brand(name = "Brand ${System.nanoTime()}", slug = "brand-${System.nanoTime()}"))
        val request = ProductCreateRequest(
            name = "Final Test Product",
            slug = "final-test-${System.nanoTime()}",
            categoryId = category.id!!,
            brandId = brand.id!!,
            price = BigDecimal("100000"),
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

    // ---------------------------------------------------------------
    // Gap 1: refresh token real expiry (previous tests only covered rotation)
    // ---------------------------------------------------------------

    @Test
    fun `an expired refresh token is rejected even though it was never used`() {
        val registerRequest = """
            {"email":"expiry-test@example.com","password":"password123","fullName":"Expiry Test"}
        """.trimIndent()
        val result = mockMvc.perform(
            post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(registerRequest)
        ).andExpect(status().isCreated).andReturn()
        val refreshToken = objectMapper.readTree(result.response.contentAsString).get("refreshToken").asText()

        val stored = refreshTokenRepository.findByTokenHash(hashToken(refreshToken)).orElseThrow()
        stored.expiresAt = Instant.now().minusSeconds(10)
        refreshTokenRepository.save(stored)

        mockMvc.perform(
            post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"refreshToken":"$refreshToken"}""")
        ).andExpect(status().isUnauthorized)
    }

    // ---------------------------------------------------------------
    // Gap 2: cancelling a CONFIRMED (not just PENDING) order
    // ---------------------------------------------------------------

    @Test
    fun `cancelling a CONFIRMED order returns stock from sold back to available`() {
        val staffToken = tokenFor(RoleName.STAFF, "staff-final1@example.com")
        val customerToken = tokenFor(RoleName.CUSTOMER, "cust-final1@example.com")
        val productId = createProduct(staffToken, stockQuantity = 5)

        mockMvc.perform(
            post("/api/cart/items")
                .header("Authorization", "Bearer $customerToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"productId":"$productId","quantity":2}""")
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

        mockMvc.perform(
            patch("/api/orders/$orderId/status")
                .header("Authorization", "Bearer $staffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"status":"CONFIRMED"}""")
        ).andExpect(status().isOk)

        mockMvc.perform(get("/api/products/$productId/inventory").header("Authorization", "Bearer $staffToken"))
            .andExpect(jsonPath("$.soldStock").value(2))
            .andExpect(jsonPath("$.availableStock").value(3))

        mockMvc.perform(
            patch("/api/orders/$orderId/status")
                .header("Authorization", "Bearer $staffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"status":"CANCELLED"}""")
        ).andExpect(status().isOk)

        mockMvc.perform(get("/api/products/$productId/inventory").header("Authorization", "Bearer $staffToken"))
            .andExpect(jsonPath("$.soldStock").value(0))
            .andExpect(jsonPath("$.availableStock").value(5))
            .andExpect(jsonPath("$.currentStock").value(5))
    }

    // ---------------------------------------------------------------
    // Gap 3: validation edge cases
    // ---------------------------------------------------------------

    @Test
    fun `a non-positive product price is rejected`() {
        val staffToken = tokenFor(RoleName.STAFF, "staff-final2@example.com")
        val category = categoryRepository.save(Category(name = "Cat ${System.nanoTime()}", slug = "cat-${System.nanoTime()}"))
        val brand = brandRepository.save(Brand(name = "Brand ${System.nanoTime()}", slug = "brand-${System.nanoTime()}"))

        val body = """
            {
                "name":"Bad Price Product","slug":"bad-price-${System.nanoTime()}",
                "categoryId":"${category.id}","brandId":"${brand.id}",
                "price":0,"condition":"GOOD","stockQuantity":1
            }
        """.trimIndent()

        mockMvc.perform(
            post("/api/products")
                .header("Authorization", "Bearer $staffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.errors.price").exists())
    }

    @Test
    fun `a review rating outside 1 to 5 is rejected`() {
        val staffToken = tokenFor(RoleName.STAFF, "staff-final3@example.com")
        val customerToken = tokenFor(RoleName.CUSTOMER, "cust-final3@example.com")
        val productId = createProduct(staffToken, stockQuantity = 5)

        mockMvc.perform(
            post("/api/products/$productId/reviews")
                .header("Authorization", "Bearer $customerToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"orderId":"${UUID.randomUUID()}","rating":7}""")
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.errors.rating").exists())
    }

    @Test
    fun `a non-positive PURCHASE quantity is rejected by inventory adjustment`() {
        val staffToken = tokenFor(RoleName.STAFF, "staff-final4@example.com")
        val productId = createProduct(staffToken, stockQuantity = 5)

        mockMvc.perform(
            post("/api/products/$productId/inventory/adjust")
                .header("Authorization", "Bearer $staffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"changeType":"PURCHASE","quantityChange":-3}""")
        ).andExpect(status().isBadRequest)
    }
}
