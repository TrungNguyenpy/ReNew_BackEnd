package com.usedmarket.product.dto

import com.usedmarket.product.entity.ConditionGrade
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class ProductResponse(
    val id: UUID,
    val name: String,
    val slug: String,
    val description: String?,
    val categoryId: UUID,
    val categoryName: String,
    val brandId: UUID,
    val brandName: String,
    val model: String?,
    val price: BigDecimal,
    val originalPrice: BigDecimal?,
    val stockQuantity: Int,

    val condition: ConditionGrade,
    /** Overall 0-100 score; null until Phase 5 computes it from ConditionScoreItem breakdown. */
    val conditionScore: BigDecimal?,
    val manufactureYear: Int?,
    val purchaseYear: Int?,
    val usageDuration: String?,
    val cosmeticCondition: String?,
    val functionalCondition: String?,
    val knownDefects: String?,
    val repairHistory: String?,
    val accessoriesIncluded: String?,

    val isActive: Boolean,
    val isHidden: Boolean,

    val images: List<ProductImageResponse>,
    val specifications: List<ProductSpecificationResponse>,

    val createdAt: Instant?,
    val updatedAt: Instant?
)
