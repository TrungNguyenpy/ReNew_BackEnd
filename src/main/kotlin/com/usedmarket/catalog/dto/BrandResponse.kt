package com.usedmarket.catalog.dto

import java.util.UUID

data class BrandResponse(
    val id: UUID,
    val name: String,
    val slug: String,
    val logoUrl: String?,
    val description: String?,
    val isActive: Boolean
)
