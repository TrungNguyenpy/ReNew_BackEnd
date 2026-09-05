package com.usedmarket.inventory.mapper

import com.usedmarket.inventory.dto.InventoryHistoryResponse
import com.usedmarket.inventory.dto.InventoryResponse
import com.usedmarket.inventory.entity.Inventory
import com.usedmarket.inventory.entity.InventoryHistory
import org.springframework.stereotype.Component

@Component
class InventoryMapper {

    fun toResponse(inventory: Inventory): InventoryResponse =
        InventoryResponse(
            id = inventory.id!!,
            productId = inventory.product.id!!,
            currentStock = inventory.currentStock,
            availableStock = inventory.availableStock,
            reservedStock = inventory.reservedStock,
            soldStock = inventory.soldStock,
            damagedStock = inventory.damagedStock
        )

    fun toHistoryResponse(history: InventoryHistory): InventoryHistoryResponse =
        InventoryHistoryResponse(
            id = history.id!!,
            changeType = history.changeType,
            quantityChange = history.quantityChange,
            previousStock = history.previousStock,
            newStock = history.newStock,
            note = history.note,
            referenceType = history.referenceType,
            referenceId = history.referenceId,
            changedByName = history.changedBy?.fullName,
            createdAt = history.createdAt
        )
}
