package com.usedmarket.product.dto

import com.usedmarket.product.entity.ImageType
import java.util.UUID

data class ProductImageResponse(
    val id: UUID,
    val imageUrl: String,
    val imageType: ImageType,
    val displayOrder: Int,
    val isPrimary: Boolean
)
