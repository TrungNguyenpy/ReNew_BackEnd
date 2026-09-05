package com.usedmarket.product.dto

import jakarta.validation.constraints.NotBlank
import java.util.UUID

data class ProductSpecificationRequest(
    @field:NotBlank(message = "Spec key is required")
    val specKey: String,

    @field:NotBlank(message = "Spec value is required")
    val specValue: String,

    val displayOrder: Int = 0
)

data class ProductSpecificationResponse(
    val id: UUID,
    val specKey: String,
    val specValue: String,
    val displayOrder: Int
)
