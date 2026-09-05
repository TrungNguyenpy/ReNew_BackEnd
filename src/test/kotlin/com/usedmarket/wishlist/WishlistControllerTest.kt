package com.usedmarket.wishlist

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
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import java.math.BigDecimal
import java.util.UUID

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WishlistControllerTest {

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
            name = "Wishlist Test Product",
            slug = "wishlist-test-${System.nanoTime()}",
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
        return CreatedProduct(id = id, categoryId = category.id!!, brandId = brand.id!!)
    }

    @Test
    fun `adding a product then fetching wishlist shows it, and duplicate add is rejected`() {
        val staffToken = tokenFor(RoleName.STAFF, "staff-wl1@example.com")
        val customerToken = tokenFor(RoleName.CUSTOMER, "cust-wl1@example.com")
        val product = createProduct(staffToken, "300000")

        mockMvc.perform(post("/api/wishlist/${product.id}").header("Authorization", "Bearer $customerToken"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.items.length()").value(1))
            .andExpect(jsonPath("$.items[0].priceDropped").value(false))

        mockMvc.perform(post("/api/wishlist/${product.id}").header("Authorization", "Bearer $customerToken"))
            .andExpect(status().isConflict)
    }

    @Test
    fun `removing a product takes it out of the wishlist`() {
        val staffToken = tokenFor(RoleName.STAFF, "staff-wl2@example.com")
        val customerToken = tokenFor(RoleName.CUSTOMER, "cust-wl2@example.com")
        val product = createProduct(staffToken, "300000")

        mockMvc.perform(post("/api/wishlist/${product.id}").header("Authorization", "Bearer $customerToken"))
            .andExpect(status().isOk)

        mockMvc.perform(delete("/api/wishlist/${product.id}").header("Authorization", "Bearer $customerToken"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.items.length()").value(0))
    }

    @Test
    fun `a subsequent price drop is reflected as priceDropped true`() {
        val staffToken = tokenFor(RoleName.STAFF, "staff-wl3@example.com")
        val customerToken = tokenFor(RoleName.CUSTOMER, "cust-wl3@example.com")
        val product = createProduct(staffToken, "300000")

        mockMvc.perform(post("/api/wishlist/${product.id}").header("Authorization", "Bearer $customerToken"))
            .andExpect(status().isOk)

        val updateRequest = ProductUpdateRequest(
            name = "Wishlist Test Product",
            slug = "wishlist-updated-${System.nanoTime()}",
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

        mockMvc.perform(get("/api/wishlist").header("Authorization", "Bearer $customerToken"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.items[0].priceDropped").value(true))
            .andExpect(jsonPath("$.items[0].currentPrice").value(200000))
            .andExpect(jsonPath("$.items[0].priceAtAddTime").value(300000))
    }
}
