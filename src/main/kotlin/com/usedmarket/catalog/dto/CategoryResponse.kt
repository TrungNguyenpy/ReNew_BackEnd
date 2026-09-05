package com.usedmarket.catalog.dto

import java.util.UUID

data class CategoryResponse(
    val id: UUID,
    val name: String,
    val slug: String,
    val description: String?,
    val imageUrl: String?,
    val parentId: UUID?,
    val isActive: Boolean,
    val displayOrder: Int,
    /** Populated only by the /tree endpoint; null/empty for flat listing. */
    val children: List<CategoryResponse> = emptyList()
)
