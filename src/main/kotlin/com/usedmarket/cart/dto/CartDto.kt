package com.usedmarket.cart.dto

import com.usedmarket.product.entity.ConditionGrade
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal
import java.util.UUID

data class CartItemResponse(
    val id: UUID,
    val productId: UUID,
    val productName: String,
    val productSlug: String,
    val primaryImageUrl: String?,
    val condition: ConditionGrade,
    val unitPrice: BigDecimal,
    val quantity: Int,
    val subtotal: BigDecimal,
    /** Current available stock, so the client can warn if the cart quantity now exceeds it. */
    val availableStock: Int
)

data class CartResponse(
    val id: UUID,
    val items: List<CartItemResponse>,
    val subtotal: BigDecimal,
    val shippingFee: BigDecimal,
    val discountAmount: BigDecimal,
    val totalAmount: BigDecimal,
    val appliedCouponCode: String?
)

data class AddCartItemRequest(
    @field:NotNull(message = "Product is required")
    val productId: UUID,

    @field:Min(value = 1, message = "Quantity must be at least 1")
    val quantity: Int = 1
)

data class UpdateCartItemRequest(
    @field:Min(value = 1, message = "Quantity must be at least 1")
    val quantity: Int
)

data class ApplyCouponRequest(
    @field:NotBlank(message = "Coupon code is required")
    val couponCode: String
)
