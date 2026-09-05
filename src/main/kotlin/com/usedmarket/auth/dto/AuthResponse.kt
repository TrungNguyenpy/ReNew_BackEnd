package com.usedmarket.auth.dto

data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String = "Bearer",
    /** Access token lifetime in seconds. */
    val expiresIn: Long,
    val user: UserSummaryDto
)
