package com.usedmarket.auth.dto

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class RegisterRequest(
    @field:NotBlank(message = "Email is required")
    @field:Email(message = "Invalid email")
    val email: String,

    @field:NotBlank(message = "Password is required")
    @field:Size(min = 8, message = "Password must be at least 8 characters")
    val password: String,

    @field:NotBlank(message = "Full name is required")
    @field:Size(max = 150, message = "Full name must be at most 150 characters")
    val fullName: String,

    val phone: String? = null
)
