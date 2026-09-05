package com.usedmarket.product.repository

import com.usedmarket.product.entity.ProductSpecification
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ProductSpecificationRepository : JpaRepository<ProductSpecification, UUID> {

    fun findByProductIdOrderByDisplayOrderAsc(productId: UUID): List<ProductSpecification>

    fun deleteByProductId(productId: UUID)
}
