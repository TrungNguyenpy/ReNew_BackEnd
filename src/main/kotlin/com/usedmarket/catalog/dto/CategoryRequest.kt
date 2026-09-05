package com.usedmarket.catalog.dto

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.util.UUID

data class CategoryRequest(
    @field:NotBlank(message = "Name is required")
    @field:Size(max = 150, message = "Name must be at most 150 characters")
    val name: String,

    @field:NotBlank(message = "Slug is required")
    @field:Size(max = 150, message = "Slug must be at most 150 characters")
    val slug: String,

    @field:Size(max = 1000, message = "Description must be at most 1000 characters")
    val description: String? = null,

    val imageUrl: String? = null,

    /** Null = root-level category. */
    val parentId: UUID? = null,

    val isActive: Boolean = true,

    val displayOrder: Int = 0
)
