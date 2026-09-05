package com.usedmarket.wishlist.dto

import com.usedmarket.product.entity.ConditionGrade
import java.math.BigDecimal
import java.util.UUID

data class WishlistItemResponse(
    val id: UUID,
    val productId: UUID,
    val productName: String,
    val productSlug: String,
    val primaryImageUrl: String?,
    val condition: ConditionGrade,
    val currentPrice: BigDecimal,
    val priceAtAddTime: BigDecimal,
    val priceDropped: Boolean,
    val availableStock: Int
)

data class WishlistResponse(
    val id: UUID,
    val items: List<WishlistItemResponse>
)
