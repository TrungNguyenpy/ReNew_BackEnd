package com.usedmarket.review.repository

import com.usedmarket.review.entity.Review
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ReviewRepository : JpaRepository<Review, UUID> {

    fun findByProductId(productId: UUID, pageable: Pageable): Page<Review>

    fun existsByOrderIdAndProductId(orderId: UUID, productId: UUID): Boolean
}
