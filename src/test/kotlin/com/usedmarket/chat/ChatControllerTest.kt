package com.usedmarket.chat

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
import org.junit.jupiter.api.Assertions.assertEquals
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
class ChatControllerTest {

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
            name = "Chat Test Product",
            slug = "chat-test-${System.nanoTime()}",
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

    @Test
    fun `customer creates a room with an initial message about a product`() {
        val staffToken = tokenFor(RoleName.STAFF, "staff-chat1@example.com")
        val customerToken = tokenFor(RoleName.CUSTOMER, "cust-chat1@example.com")
        val productId = createProduct(staffToken)

        val result = mockMvc.perform(
            post("/api/chat/rooms")
                .header("Authorization", "Bearer $customerToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"productId":"$productId","initialMessage":"Is this still available?"}""")
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.status").value("OPEN"))
            .andExpect(jsonPath("$.staffId").doesNotExist())
            .andReturn()
        val roomId = objectMapper.readTree(result.response.contentAsString).get("id").asText()

        mockMvc.perform(get("/api/chat/rooms/$roomId/messages").header("Authorization", "Bearer $customerToken"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].content").value("Is this still available?"))
    }

    @Test
    fun `creating a second room for the same product reuses the open one`() {
        val staffToken = tokenFor(RoleName.STAFF, "staff-chat2@example.com")
        val customerToken = tokenFor(RoleName.CUSTOMER, "cust-chat2@example.com")
        val productId = createProduct(staffToken)

        val first = mockMvc.perform(
            post("/api/chat/rooms")
                .header("Authorization", "Bearer $customerToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"productId":"$productId","initialMessage":"Question 1"}""")
        ).andExpect(status().isCreated).andReturn()
        val firstId = objectMapper.readTree(first.response.contentAsString).get("id").asText()

        val second = mockMvc.perform(
            post("/api/chat/rooms")
                .header("Authorization", "Bearer $customerToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"productId":"$productId","initialMessage":"Question 2"}""")
        ).andExpect(status().isCreated).andReturn()
        val secondId = objectMapper.readTree(second.response.contentAsString).get("id").asText()

        assertEquals(firstId, secondId)

        mockMvc.perform(get("/api/chat/rooms/$firstId/messages").header("Authorization", "Bearer $customerToken"))
            .andExpect(jsonPath("$.length()").value(2))
    }

    @Test
    fun `first staff reply auto-claims the room, and a second staff member is then blocked`() {
        val staff1Token = tokenFor(RoleName.STAFF, "staff-chat3-a@example.com")
        val staff2Token = tokenFor(RoleName.STAFF, "staff-chat3-b@example.com")
        val customerToken = tokenFor(RoleName.CUSTOMER, "cust-chat3@example.com")
        val productId = createProduct(staff1Token)

        val roomResult = mockMvc.perform(
            post("/api/chat/rooms")
                .header("Authorization", "Bearer $customerToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"productId":"$productId","initialMessage":"Any defects?"}""")
        ).andExpect(status().isCreated).andReturn()
        val roomId = objectMapper.readTree(roomResult.response.contentAsString).get("id").asText()

        mockMvc.perform(
            post("/api/chat/rooms/$roomId/messages")
                .header("Authorization", "Bearer $staff1Token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"content":"No defects, works perfectly."}""")
        ).andExpect(status().isCreated)

        mockMvc.perform(
            post("/api/chat/rooms/$roomId/messages")
                .header("Authorization", "Bearer $staff2Token")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"content":"Let me also help..."}""")
        ).andExpect(status().isForbidden)
    }

    @Test
    fun `reading messages marks the other party's messages as read`() {
        val staffToken = tokenFor(RoleName.STAFF, "staff-chat4@example.com")
        val customerToken = tokenFor(RoleName.CUSTOMER, "cust-chat4@example.com")
        val productId = createProduct(staffToken)

        val roomResult = mockMvc.perform(
            post("/api/chat/rooms")
                .header("Authorization", "Bearer $customerToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"productId":"$productId","initialMessage":"Hi there"}""")
        ).andExpect(status().isCreated).andReturn()
        val roomId = objectMapper.readTree(roomResult.response.contentAsString).get("id").asText()

        mockMvc.perform(
            post("/api/chat/rooms/$roomId/messages")
                .header("Authorization", "Bearer $staffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"content":"Hello! How can I help?"}""")
        ).andExpect(status().isCreated)

        mockMvc.perform(get("/api/chat/rooms/$roomId/messages").header("Authorization", "Bearer $customerToken"))
            .andExpect(jsonPath("$[1].isRead").value(true))
    }

    @Test
    fun `a customer cannot access another customer's chat room`() {
        val staffToken = tokenFor(RoleName.STAFF, "staff-chat5@example.com")
        val ownerToken = tokenFor(RoleName.CUSTOMER, "cust-chat5-owner@example.com")
        val intruderToken = tokenFor(RoleName.CUSTOMER, "cust-chat5-intruder@example.com")
        val productId = createProduct(staffToken)

        val roomResult = mockMvc.perform(
            post("/api/chat/rooms")
                .header("Authorization", "Bearer $ownerToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"productId":"$productId"}""")
        ).andExpect(status().isCreated).andReturn()
        val roomId = objectMapper.readTree(roomResult.response.contentAsString).get("id").asText()

        mockMvc.perform(get("/api/chat/rooms/$roomId/messages").header("Authorization", "Bearer $intruderToken"))
            .andExpect(status().isNotFound)
    }

    @Test
    fun `sending a message to a closed room is rejected`() {
        val staffToken = tokenFor(RoleName.STAFF, "staff-chat6@example.com")
        val customerToken = tokenFor(RoleName.CUSTOMER, "cust-chat6@example.com")
        val productId = createProduct(staffToken)

        val roomResult = mockMvc.perform(
            post("/api/chat/rooms")
                .header("Authorization", "Bearer $customerToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"productId":"$productId"}""")
        ).andExpect(status().isCreated).andReturn()
        val roomId = objectMapper.readTree(roomResult.response.contentAsString).get("id").asText()

        mockMvc.perform(patch("/api/chat/rooms/$roomId/close").header("Authorization", "Bearer $customerToken"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("CLOSED"))

        mockMvc.perform(
            post("/api/chat/rooms/$roomId/messages")
                .header("Authorization", "Bearer $customerToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"content":"Still there?"}""")
        ).andExpect(status().isBadRequest)
    }
}
