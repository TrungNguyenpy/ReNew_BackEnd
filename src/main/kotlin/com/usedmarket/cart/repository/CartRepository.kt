package com.usedmarket.cart.repository

import com.usedmarket.cart.entity.Cart
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional
import java.util.UUID

interface CartRepository : JpaRepository<Cart, UUID> {

    fun findByUserId(userId: UUID): Optional<Cart>
}
