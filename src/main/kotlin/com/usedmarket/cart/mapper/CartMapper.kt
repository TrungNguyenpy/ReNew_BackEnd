package com.usedmarket.cart.mapper

import com.usedmarket.cart.dto.CartItemResponse
import com.usedmarket.cart.dto.CartResponse
import com.usedmarket.cart.entity.Cart
import com.usedmarket.cart.entity.CartItem
import org.springframework.stereotype.Component
import java.math.BigDecimal

@Component
class CartMapper {

    fun toItemResponse(item: CartItem, primaryImageUrl: String?, availableStock: Int): CartItemResponse {
        val product = item.product
        val subtotal = product.price.multiply(BigDecimal(item.quantity))
        return CartItemResponse(
            id = item.id!!,
            productId = product.id!!,
            productName = product.name,
            productSlug = product.slug,
            primaryImageUrl = primaryImageUrl,
            condition = product.condition,
            unitPrice = product.price,
            quantity = item.quantity,
            subtotal = subtotal,
            availableStock = availableStock
        )
    }

    fun toResponse(
        cart: Cart,
        itemResponses: List<CartItemResponse>,
        subtotal: BigDecimal,
        shippingFee: BigDecimal,
        discountAmount: BigDecimal,
        appliedCouponCode: String?
    ): CartResponse {
        var total = subtotal.subtract(discountAmount).add(shippingFee)
        if (total < BigDecimal.ZERO) total = BigDecimal.ZERO
        return CartResponse(
            id = cart.id!!,
            items = itemResponses,
            subtotal = subtotal,
            shippingFee = shippingFee,
            discountAmount = discountAmount,
            totalAmount = total,
            appliedCouponCode = appliedCouponCode
        )
    }
}
