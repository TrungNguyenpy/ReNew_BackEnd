package com.usedmarket.common.exception

import java.time.Instant

data class ErrorResponse(
    val timestamp: Instant = Instant.now(),
    val status: Int,
    val message: String,
    val errors: Map<String, String>? = null
)
