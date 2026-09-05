package com.usedmarket.product

import com.fasterxml.jackson.databind.ObjectMapper
import com.usedmarket.catalog.entity.Brand
import com.usedmarket.catalog.entity.Category
import com.usedmarket.catalog.repository.BrandRepository
import com.usedmarket.catalog.repository.CategoryRepository
import com.usedmarket.product.entity.ConditionGrade
import com.usedmarket.product.entity.Product
import com.usedmarket.product.repository.ProductRepository
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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ProductDetailApisTest {

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
    lateinit var productRepository: ProductRepository

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

    private fun createProduct(): Product {
        val category = categoryRepository.save(Category(name = "Phones ${System.nanoTime()}", slug = "phones-${System.nanoTime()}"))
        val brand = brandRepository.save(Brand(name = "Brand ${System.nanoTime()}", slug = "brand-${System.nanoTime()}"))
        return productRepository.save(
            Product(
                name = "Test Phone",
                slug = "test-phone-${System.nanoTime()}",
                category = category,
                brand = brand,
                price = BigDecimal("5000000"),
                condition = ConditionGrade.GOOD
            )
        )
    }

    @Test
    fun `staff updates condition score and product conditionScore reflects the average`() {
        val staffToken = tokenFor(RoleName.STAFF, "staff-cs@example.com")
        val product = createProduct()

        val body = """
            {"items":[
                {"criterion":"Screen","score":90},
                {"criterion":"Battery","score":70}
            ]}
        """.trimIndent()

        mockMvc.perform(
            put("/api/products/${product.id}/condition-score")
                .header("Authorization", "Bearer $staffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.overallScore").value(80.0))
            .andExpect(jsonPath("$.items.length()").value(2))

        mockMvc.perform(get("/api/products/${product.id}/condition-score"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.overallScore").value(80.0))
    }

    @Test
    fun `customer cannot update condition score`() {
        val customerToken = tokenFor(RoleName.CUSTOMER, "cust-cs@example.com")
        val product = createProduct()

        mockMvc.perform(
            put("/api/products/${product.id}/condition-score")
                .header("Authorization", "Bearer $customerToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"items":[]}""")
        ).andExpect(status().isForbidden)
    }

    @Test
    fun `public inspection report is 404 until staff publishes one`() {
        val staffToken = tokenFor(RoleName.STAFF, "staff-insp@example.com")
        val product = createProduct()

        mockMvc.perform(get("/api/products/${product.id}/inspection"))
            .andExpect(status().isNotFound)

        val createBody = """
            {"resultSummary":"All good","isPublic":true,"items":[
                {"itemName":"Screen","status":"PASS"},
                {"itemName":"Battery","status":"WARNING"},
                {"itemName":"Camera","status":"FAIL"}
            ]}
        """.trimIndent()

        val createResult = mockMvc.perform(
            post("/api/products/${product.id}/inspection")
                .header("Authorization", "Bearer $staffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createBody)
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.inspectionScore").value(50)) // (100 + 50 + 0) / 3 = 50
            .andReturn()

        val inspectionId = objectMapper.readTree(createResult.response.contentAsString).get("id").asText()

        mockMvc.perform(get("/api/products/${product.id}/inspection"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.inspectionScore").value(50))
            .andExpect(jsonPath("$.items.length()").value(3))

        // Redacted public response must not leak internal notes or inspector identity.
        mockMvc.perform(get("/api/products/${product.id}/inspection"))
            .andExpect(jsonPath("$.internalNotes").doesNotExist())
            .andExpect(jsonPath("$.inspectorId").doesNotExist())

        // Un-publish and confirm it disappears from the public endpoint again.
        mockMvc.perform(
            patch("/api/products/${product.id}/inspection/$inspectionId/publish")
                .header("Authorization", "Bearer $staffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"isPublic":false}""")
        ).andExpect(status().isOk)

        mockMvc.perform(get("/api/products/${product.id}/inspection"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `warranty can be created then updated via the same PUT endpoint`() {
        val adminToken = tokenFor(RoleName.ADMIN, "admin-warranty@example.com")
        val product = createProduct()

        mockMvc.perform(
            put("/api/products/${product.id}/warranty")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"warrantyType":"STORE","durationMonths":6}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.warrantyType").value("STORE"))
            .andExpect(jsonPath("$.durationMonths").value(6))

        mockMvc.perform(
            put("/api/products/${product.id}/warranty")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"warrantyType":"MANUFACTURER","durationMonths":12}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.warrantyType").value("MANUFACTURER"))
            .andExpect(jsonPath("$.durationMonths").value(12))

        mockMvc.perform(get("/api/products/${product.id}/warranty"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.warrantyType").value("MANUFACTURER"))
    }
}
