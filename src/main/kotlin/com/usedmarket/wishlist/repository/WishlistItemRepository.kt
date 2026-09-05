package com.usedmarket.wishlist.repository

import com.usedmarket.wishlist.entity.WishlistItem
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional
import java.util.UUID

interface WishlistItemRepository : JpaRepository<WishlistItem, UUID> {

    fun findByWishlistId(wishlistId: UUID): List<WishlistItem>

    fun findByWishlistIdAndProductId(wishlistId: UUID, productId: UUID): Optional<WishlistItem>

    fun deleteByWishlistIdAndProductId(wishlistId: UUID, productId: UUID)

    /** Used by the price-drop notification job to scan all wishlist entries for a product. */
    fun findByProductId(productId: UUID): List<WishlistItem>
}
