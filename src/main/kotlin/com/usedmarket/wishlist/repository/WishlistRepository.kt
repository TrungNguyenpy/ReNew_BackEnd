package com.usedmarket.wishlist.repository

import com.usedmarket.wishlist.entity.Wishlist
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional
import java.util.UUID

interface WishlistRepository : JpaRepository<Wishlist, UUID> {

    fun findByUserId(userId: UUID): Optional<Wishlist>
}
