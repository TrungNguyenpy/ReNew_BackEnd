package com.usedmarket.inventory

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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InventoryControllerTest {

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

    private fun createProductViaApi(staffToken: String, stockQuantity: Int = 5): String {
        val category = categoryRepository.save(Category(name = "Cat ${System.nanoTime()}", slug = "cat-${System.nanoTime()}"))
        val brand = brandRepository.save(Brand(name = "Brand ${System.nanoTime()}", slug = "brand-${System.nanoTime()}"))
        val request = ProductCreateRequest(
            name = "Inventory Test Product",
            slug = "inv-test-${System.nanoTime()}",
            categoryId = category.id!!,
            brandId = brand.id!!,
            price = BigDecimal("1000000"),
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
    fun `creating a product auto-initializes its inventory with a PURCHASE history entry`() {
        val staffToken = tokenFor(RoleName.STAFF, "staff-inv1@example.com")
        val productId = createProductViaApi(staffToken, stockQuantity = 5)

        mockMvc.perform(get("/api/products/$productId/inventory").header("Authorization", "Bearer $staffToken"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.currentStock").value(5))
            .andExpect(jsonPath("$.availableStock").value(5))
            .andExpect(jsonPath("$.reservedStock").value(0))

        mockMvc.perform(get("/api/products/$productId/inventory/history").header("Authorization", "Bearer $staffToken"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].changeType").value("PURCHASE"))
            .andExpect(jsonPath("$[0].newStock").value(5))
    }

    @Test
    fun `customer cannot view or adjust inventory`() {
        val staffToken = tokenFor(RoleName.STAFF, "staff-inv2@example.com")
        val customerToken = tokenFor(RoleName.CUSTOMER, "cust-inv2@example.com")
        val productId = createProductViaApi(staffToken)

        mockMvc.perform(get("/api/products/$productId/inventory").header("Authorization", "Bearer $customerToken"))
            .andExpect(status().isForbidden)
    }

    @Test
    fun `staff can adjust stock and Product stockQuantity stays in sync`() {
        val staffToken = tokenFor(RoleName.STAFF, "staff-inv3@example.com")
        val productId = createProductViaApi(staffToken, stockQuantity = 10)

        mockMvc.perform(
            post("/api/products/$productId/inventory/adjust")
                .header("Authorization", "Bearer $staffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"changeType":"DAMAGE","quantityChange":3,"note":"Dropped during handling"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.availableStock").value(7))
            .andExpect(jsonPath("$.damagedStock").value(3))
            .andExpect(jsonPath("$.currentStock").value(10))

        // Product.stockQuantity (denormalized) must reflect the new availableStock.
        mockMvc.perform(get("/api/products/$productId").header("Authorization", "Bearer $staffToken"))
            .andExpect(jsonPath("$.stockQuantity").value(7))
    }

    @Test
    fun `damage adjustment beyond available stock is rejected`() {
        val staffToken = tokenFor(RoleName.STAFF, "staff-inv4@example.com")
        val productId = createProductViaApi(staffToken, stockQuantity = 2)

        mockMvc.perform(
            post("/api/products/$productId/inventory/adjust")
                .header("Authorization", "Bearer $staffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"changeType":"DAMAGE","quantityChange":5}""")
        ).andExpect(status().isConflict)
    }
}
