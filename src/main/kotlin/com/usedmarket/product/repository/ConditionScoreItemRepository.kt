package com.usedmarket.product.repository

import com.usedmarket.product.entity.ConditionScoreItem
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ConditionScoreItemRepository : JpaRepository<ConditionScoreItem, UUID> {

    fun findByProductId(productId: UUID): List<ConditionScoreItem>

    fun deleteByProductId(productId: UUID)
}
