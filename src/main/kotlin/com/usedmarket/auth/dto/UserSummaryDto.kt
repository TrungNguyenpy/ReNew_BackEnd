package com.usedmarket.auth.dto

import java.util.UUID

data class UserSummaryDto(
    val id: UUID,
    val email: String,
    val fullName: String,
    val role: String
)
