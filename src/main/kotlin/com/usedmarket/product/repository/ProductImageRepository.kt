package com.usedmarket.product.repository

import com.usedmarket.product.entity.ProductImage
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ProductImageRepository : JpaRepository<ProductImage, UUID> {

    fun findByProductIdOrderByDisplayOrderAsc(productId: UUID): List<ProductImage>

    fun findByProductIdAndIsPrimaryTrue(productId: UUID): ProductImage?
}
