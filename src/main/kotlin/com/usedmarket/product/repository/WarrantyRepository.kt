package com.usedmarket.product.repository

import com.usedmarket.product.entity.Warranty
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional
import java.util.UUID

interface WarrantyRepository : JpaRepository<Warranty, UUID> {

    fun findByProductId(productId: UUID): Optional<Warranty>
}
