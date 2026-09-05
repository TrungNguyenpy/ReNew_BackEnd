package com.usedmarket.cart.repository

import com.usedmarket.cart.entity.CartItem
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional
import java.util.UUID

interface CartItemRepository : JpaRepository<CartItem, UUID> {

    fun findByCartId(cartId: UUID): List<CartItem>

    fun findByCartIdAndProductId(cartId: UUID, productId: UUID): Optional<CartItem>

    fun deleteByCartIdAndProductId(cartId: UUID, productId: UUID)
}
