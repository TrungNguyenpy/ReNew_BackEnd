package com.usedmarket.product.dto

import com.usedmarket.product.entity.ConditionGrade
import java.math.BigDecimal
import java.util.UUID

data class ProductSummaryResponse(
    val id: UUID,
    val name: String,
    val slug: String,
    val price: BigDecimal,
    val originalPrice: BigDecimal?,
    val condition: ConditionGrade,
    val conditionScore: BigDecimal?,
    val primaryImageUrl: String?,
    val categoryName: String,
    val brandName: String,
    val isActive: Boolean,
    val isHidden: Boolean
)
