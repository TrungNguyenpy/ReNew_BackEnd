package com.usedmarket.product.repository

import com.usedmarket.product.entity.ProductInspection
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ProductInspectionRepository : JpaRepository<ProductInspection, UUID> {

    fun findByProductIdOrderByCreatedAtDesc(productId: UUID): List<ProductInspection>

    /** Latest public inspection report to show on the product detail page. */
    fun findFirstByProductIdAndIsPublicTrueOrderByCreatedAtDesc(productId: UUID): ProductInspection?
}
