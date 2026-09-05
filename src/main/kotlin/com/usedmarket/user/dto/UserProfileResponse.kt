package com.usedmarket.user.dto

import java.util.UUID

data class UserProfileResponse(
    val id: UUID,
    val email: String,
    val fullName: String,
    val phone: String?,
    val role: String,
    val avatarUrl: String?,
    val emailVerified: Boolean
)
