package com.usedmarket.tradein

import com.fasterxml.jackson.databind.ObjectMapper
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

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TradeInControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Autowired
    lateinit var userRepository: UserRepository

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

    private val createBody = """
        {
            "productName":"iPhone 11",
            "brand":"Apple",
            "model":"iPhone 11",
            "purchaseYear":2019,
            "usageDuration":"3 years",
            "condition":"GOOD",
            "expectedPrice":5000000,
            "contactPhone":"0900000000"
        }
    """.trimIndent()

    @Test
    fun `customer submits a trade-in request in PENDING status`() {
        val customerToken = tokenFor(RoleName.CUSTOMER, "cust-ti1@example.com")

        mockMvc.perform(
            post("/api/trade-ins")
                .header("Authorization", "Bearer $customerToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createBody)
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.status").value("PENDING"))
            .andExpect(jsonPath("$.productName").value("iPhone 11"))
    }

    @Test
    fun `full happy path from submission to purchase`() {
        val staffToken = tokenFor(RoleName.STAFF, "staff-ti2@example.com")
        val customerToken = tokenFor(RoleName.CUSTOMER, "cust-ti2@example.com")

        val createResult = mockMvc.perform(
            post("/api/trade-ins")
                .header("Authorization", "Bearer $customerToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createBody)
        ).andExpect(status().isCreated).andReturn()
        val id = objectMapper.readTree(createResult.response.contentAsString).get("id").asText()

        mockMvc.perform(
            patch("/api/trade-ins/$id/status")
                .header("Authorization", "Bearer $staffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"status":"INSPECTING"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("INSPECTING"))
            .andExpect(jsonPath("$.inspectedByName").exists())

        mockMvc.perform(
            post("/api/trade-ins/$id/offer")
                .header("Authorization", "Bearer $staffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"offeredPrice":4000000,"inspectionNote":"Screen has minor scratches"}""")
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("OFFERED"))
            .andExpect(jsonPath("$.offeredPrice").value(4000000))

        mockMvc.perform(post("/api/trade-ins/$id/accept").header("Authorization", "Bearer $customerToken"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("CUSTOMER_ACCEPTED"))

        mockMvc.perform(patch("/api/trade-ins/$id/complete").header("Authorization", "Bearer $staffToken"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("PURCHASED"))
    }

    @Test
    fun `customer can decline an offer`() {
        val staffToken = tokenFor(RoleName.STAFF, "staff-ti3@example.com")
        val customerToken = tokenFor(RoleName.CUSTOMER, "cust-ti3@example.com")

        val createResult = mockMvc.perform(
            post("/api/trade-ins")
                .header("Authorization", "Bearer $customerToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createBody)
        ).andExpect(status().isCreated).andReturn()
        val id = objectMapper.readTree(createResult.response.contentAsString).get("id").asText()

        mockMvc.perform(
            patch("/api/trade-ins/$id/status")
                .header("Authorization", "Bearer $staffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"status":"INSPECTING"}""")
        ).andExpect(status().isOk)

        mockMvc.perform(
            post("/api/trade-ins/$id/offer")
                .header("Authorization", "Bearer $staffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"offeredPrice":1000000}""")
        ).andExpect(status().isOk)

        mockMvc.perform(post("/api/trade-ins/$id/decline").header("Authorization", "Bearer $customerToken"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.status").value("REJECTED"))
    }

    @Test
    fun `invalid transition is rejected`() {
        val staffToken = tokenFor(RoleName.STAFF, "staff-ti4@example.com")
        val customerToken = tokenFor(RoleName.CUSTOMER, "cust-ti4@example.com")

        val createResult = mockMvc.perform(
            post("/api/trade-ins")
                .header("Authorization", "Bearer $customerToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createBody)
        ).andExpect(status().isCreated).andReturn()
        val id = objectMapper.readTree(createResult.response.contentAsString).get("id").asText()

        mockMvc.perform(
            post("/api/trade-ins/$id/offer")
                .header("Authorization", "Bearer $staffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"offeredPrice":1000000}""")
        ).andExpect(status().isConflict)
    }

    @Test
    fun `generic status endpoint rejects OFFERED as a direct target`() {
        val staffToken = tokenFor(RoleName.STAFF, "staff-ti5@example.com")
        val customerToken = tokenFor(RoleName.CUSTOMER, "cust-ti5@example.com")

        val createResult = mockMvc.perform(
            post("/api/trade-ins")
                .header("Authorization", "Bearer $customerToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createBody)
        ).andExpect(status().isCreated).andReturn()
        val id = objectMapper.readTree(createResult.response.contentAsString).get("id").asText()

        mockMvc.perform(
            patch("/api/trade-ins/$id/status")
                .header("Authorization", "Bearer $staffToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""{"status":"OFFERED"}""")
        ).andExpect(status().isBadRequest)
    }

    @Test
    fun `a customer cannot view another customer's trade-in request`() {
        val ownerToken = tokenFor(RoleName.CUSTOMER, "cust-ti6-owner@example.com")
        val intruderToken = tokenFor(RoleName.CUSTOMER, "cust-ti6-intruder@example.com")

        val createResult = mockMvc.perform(
            post("/api/trade-ins")
                .header("Authorization", "Bearer $ownerToken")
                .contentType(MediaType.APPLICATION_JSON)
                .content(createBody)
        ).andExpect(status().isCreated).andReturn()
        val id = objectMapper.readTree(createResult.response.contentAsString).get("id").asText()

        mockMvc.perform(get("/api/trade-ins/$id").header("Authorization", "Bearer $intruderToken"))
            .andExpect(status().isNotFound)

        mockMvc.perform(get("/api/trade-ins/$id").header("Authorization", "Bearer $ownerToken"))
            .andExpect(status().isOk)
    }

    @Test
    fun `customer cannot access the staff management listing`() {
        val customerToken = tokenFor(RoleName.CUSTOMER, "cust-ti7@example.com")

        mockMvc.perform(get("/api/trade-ins/manage").header("Authorization", "Bearer $customerToken"))
            .andExpect(status().isForbidden)
    }
}
