package com.usedmarket.inventory.repository

import com.usedmarket.inventory.entity.InventoryHistory
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface InventoryHistoryRepository : JpaRepository<InventoryHistory, UUID> {

    fun findByInventoryIdOrderByCreatedAtDesc(inventoryId: UUID): List<InventoryHistory>
}
