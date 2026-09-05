package com.usedmarket.wishlist.mapper

import com.usedmarket.wishlist.dto.WishlistItemResponse
import com.usedmarket.wishlist.dto.WishlistResponse
import com.usedmarket.wishlist.entity.Wishlist
import com.usedmarket.wishlist.entity.WishlistItem
import org.springframework.stereotype.Component

@Component
class WishlistMapper {

    fun toItemResponse(item: WishlistItem, primaryImageUrl: String?, availableStock: Int): WishlistItemResponse {
        val product = item.product
        return WishlistItemResponse(
            id = item.id!!,
            productId = product.id!!,
            productName = product.name,
            productSlug = product.slug,
            primaryImageUrl = primaryImageUrl,
            condition = product.condition,
            currentPrice = product.price,
            priceAtAddTime = item.priceAtAddTime,
            priceDropped = product.price < item.priceAtAddTime,
            availableStock = availableStock
        )
    }

    fun toResponse(wishlist: Wishlist, items: List<WishlistItemResponse>): WishlistResponse =
        WishlistResponse(id = wishlist.id!!, items = items)
}
