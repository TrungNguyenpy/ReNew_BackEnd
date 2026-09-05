package com.usedmarket.catalog.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class BrandRequest(
    @field:NotBlank(message = "Name is required")
    @field:Size(max = 150, message = "Name must be at most 150 characters")
    val name: String,

    @field:NotBlank(message = "Slug is required")
    @field:Size(max = 150, message = "Slug must be at most 150 characters")
    val slug: String,

    val logoUrl: String? = null,

    @field:Size(max = 1000, message = "Description must be at most 1000 characters")
    val description: String? = null,

    val isActive: Boolean = true
)
