package com.usedmarket.product

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
class ProductCatalogControllerTest {

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

    /** Mints a valid access token for a freshly created user of the given role, bypassing HTTP register (which always defaults to CUSTOMER). */
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

    @Test
    fun `public can browse categories, brands and products without auth`() {
        mockMvc.perform(get("/api/categories")).andExpect(status().isOk)
        mockMvc.perform(get("/api/brands")).andExpect(status().isOk)
        mockMvc.perform(get("/api/products")).andExpect(status().isOk)
    }

    @Test
    fun `customer cannot create a category`() {
        val token = tokenFor(RoleName.CUSTOMER, "cust1@example.com")
        val body = """{"name":"Laptop","slug":"laptop-${System.nanoTime()}"}"""

        mockMvc.perform(
            post("/api/categories")
                .header("Authorization", "Bearer $token")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        ).andExpect(status().isForbidden)
    }

    @Test
    fun `staff cannot create a category but admin can`() {
        val staffToken = tokenFor(RoleName.STAFF, "staff1@example.com")
        val adminToken = tokenFor(RoleName.ADMIN, "admin1@example.com")
        val body = """{"name":"Electronics ${System.nanoTime()}","slug":"electronics-${System.nanoTime()}"}"""

        mockMvc.perform(
            post("/api/categories")
                .header("Authorization", "Bearer $staffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        ).andExpect(status().isForbidden)

        mockMvc.perform(
            post("/api/categories")
                .header("Authorization", "Bearer $adminToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body)
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.isActive").value(true))
    }

    @Test
    fun `staff can create a product but customer cannot`() {
        val staffToken = tokenFor(RoleName.STAFF, "staff2@example.com")
        val customerToken = tokenFor(RoleName.CUSTOMER, "cust2@example.com")

        val category = categoryRepository.save(Category(name = "Phones", slug = "phones-${System.nanoTime()}"))
        val brand = brandRepository.save(Brand(name = "Apple ${System.nanoTime()}", slug = "apple-${System.nanoTime()}"))

        val request = ProductCreateRequest(
            name = "iPhone 12 Used",
            slug = "iphone-12-used-${System.nanoTime()}",
            categoryId = category.id!!,
            brandId = brand.id!!,
            price = BigDecimal("8000000"),
            condition = ConditionGrade.GOOD
        )

        mockMvc.perform(
            post("/api/products")
                .header("Authorization", "Bearer $customerToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        ).andExpect(status().isForbidden)

        mockMvc.perform(
            post("/api/products")
                .header("Authorization", "Bearer $staffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.name").value("iPhone 12 Used"))
            .andExpect(jsonPath("$.condition").value("GOOD"))
    }

    @Test
    fun `search filters by keyword and price range`() {
        val staffToken = tokenFor(RoleName.STAFF, "staff3@example.com")
        val category = categoryRepository.save(Category(name = "Laptops", slug = "laptops-${System.nanoTime()}"))
        val brand = brandRepository.save(Brand(name = "Dell ${System.nanoTime()}", slug = "dell-${System.nanoTime()}"))

        fun createProduct(name: String, price: String) {
            val request = ProductCreateRequest(
                name = name,
                slug = "${name.lowercase().replace(" ", "-")}-${System.nanoTime()}",
                categoryId = category.id!!,
                brandId = brand.id!!,
                price = BigDecimal(price),
                condition = ConditionGrade.GOOD
            )
            mockMvc.perform(
                post("/api/products")
                    .header("Authorization", "Bearer $staffToken")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request))
            ).andExpect(status().isCreated)
        }

        createProduct("Dell XPS 13", "15000000")
        createProduct("Dell Inspiron", "8000000")

        mockMvc.perform(get("/api/products").param("keyword", "XPS"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content[0].name").value("Dell XPS 13"))

        mockMvc.perform(get("/api/products").param("minPrice", "10000000"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.content.length()").value(1))
    }

    @Test
    fun `hidden product is invisible to public but visible to staff`() {
        val staffToken = tokenFor(RoleName.STAFF, "staff4@example.com")
        val category = categoryRepository.save(Category(name = "Tablets", slug = "tablets-${System.nanoTime()}"))
        val brand = brandRepository.save(Brand(name = "Samsung ${System.nanoTime()}", slug = "samsung-${System.nanoTime()}"))

        val request = ProductCreateRequest(
            name = "Galaxy Tab",
            slug = "galaxy-tab-${System.nanoTime()}",
            categoryId = category.id!!,
            brandId = brand.id!!,
            price = BigDecimal("5000000"),
            condition = ConditionGrade.FAIR
        )

        val createResult = mockMvc.perform(
            post("/api/products")
                .header("Authorization", "Bearer $staffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        ).andExpect(status().isCreated).andReturn()

        val productId = objectMapper.readTree(createResult.response.contentAsString).get("id").asText()

        mockMvc.perform(
            patch("/api/products/$productId/visibility")
                .header("Authorization", "Bearer $staffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"isHidden":true}""")
        ).andExpect(status().isOk)

        mockMvc.perform(get("/api/products/$productId"))
            .andExpect(status().isNotFound)

        mockMvc.perform(get("/api/products/$productId").header("Authorization", "Bearer $staffToken"))
            .andExpect(status().isOk)
    }
}
