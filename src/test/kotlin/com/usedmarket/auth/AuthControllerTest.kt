package com.usedmarket.auth

import com.fasterxml.jackson.databind.ObjectMapper
import com.usedmarket.auth.dto.LoginRequest
import com.usedmarket.auth.dto.RefreshTokenRequest
import com.usedmarket.auth.dto.RegisterRequest
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.http.MediaType
import org.springframework.test.context.ActiveProfiles
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthControllerTest {

    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @Test
    fun `register creates a new user and returns tokens`() {
        val request = RegisterRequest(
            email = "alice@example.com",
            password = "password123",
            fullName = "Alice Nguyen",
            phone = "0900000000"
        )

        mockMvc.perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isCreated)
            .andExpect(jsonPath("$.accessToken").isNotEmpty)
            .andExpect(jsonPath("$.refreshToken").isNotEmpty)
            .andExpect(jsonPath("$.user.email").value("alice@example.com"))
            .andExpect(jsonPath("$.user.role").value("CUSTOMER"))
    }

    @Test
    fun `register with duplicate email returns 409`() {
        val request = RegisterRequest(email = "bob@example.com", password = "password123", fullName = "Bob Tran")

        mockMvc.perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        ).andExpect(status().isCreated)

        mockMvc.perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        ).andExpect(status().isConflict)
    }

    @Test
    fun `register with invalid email returns 400 with field error`() {
        val request = RegisterRequest(email = "not-an-email", password = "password123", fullName = "Charlie")

        mockMvc.perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
            .andExpect(status().isBadRequest)
            .andExpect(jsonPath("$.errors.email").exists())
    }

    @Test
    fun `login with wrong password returns 401`() {
        val register = RegisterRequest(email = "dave@example.com", password = "password123", fullName = "Dave Le")
        mockMvc.perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(register))
        ).andExpect(status().isCreated)

        val badLogin = LoginRequest(email = "dave@example.com", password = "wrongpassword")
        mockMvc.perform(
            post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(badLogin))
        ).andExpect(status().isUnauthorized)
    }

    @Test
    fun `protected endpoint rejects request without a token`() {
        mockMvc.perform(get("/api/users/me"))
            .andExpect(status().isUnauthorized)
    }

    @Test
    fun `protected endpoint accepts a valid access token`() {
        val register = RegisterRequest(email = "erin@example.com", password = "password123", fullName = "Erin Pham")
        val result = mockMvc.perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(register))
        )
            .andExpect(status().isCreated)
            .andReturn()

        val accessToken = objectMapper.readTree(result.response.contentAsString).get("accessToken").asText()

        mockMvc.perform(get("/api/users/me").header("Authorization", "Bearer $accessToken"))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.email").value("erin@example.com"))
    }

    @Test
    fun `refresh token rotates and the old token becomes invalid`() {
        val register = RegisterRequest(email = "frank@example.com", password = "password123", fullName = "Frank Do")
        val result = mockMvc.perform(
            post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(register))
        )
            .andExpect(status().isCreated)
            .andReturn()

        val refreshToken = objectMapper.readTree(result.response.contentAsString).get("refreshToken").asText()

        mockMvc.perform(
            post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(RefreshTokenRequest(refreshToken)))
        )
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.accessToken").isNotEmpty)

        // Reusing the same (now-revoked) refresh token must fail — this is the rotation guarantee.
        mockMvc.perform(
            post("/api/auth/refresh")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(RefreshTokenRequest(refreshToken)))
        ).andExpect(status().isUnauthorized)
    }
}
