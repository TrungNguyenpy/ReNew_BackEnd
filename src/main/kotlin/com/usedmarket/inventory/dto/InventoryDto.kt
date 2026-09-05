package com.usedmarket.inventory.dto

import com.usedmarket.inventory.entity.InventoryChangeType
import jakarta.validation.constraints.NotNull
import java.time.Instant
import java.util.UUID

data class InventoryResponse(
    val id: UUID,
    val productId: UUID,
    val currentStock: Int,
    val availableStock: Int,
    val reservedStock: Int,
    val soldStock: Int,
    val damagedStock: Int
)

data class InventoryAdjustRequest(
    @field:NotNull(message = "Change type is required")
    val changeType: InventoryChangeType,

    /**
     * Signed magnitude of the change. Sign convention depends on changeType:
     * PURCHASE/RETURN: positive (stock entering/coming back).
     * SALE/DAMAGE: positive value meaning "N units removed from available stock".
     * ADJUSTMENT: signed — positive to increase, negative to correct downward.
     */
    @field:NotNull(message = "Quantity change is required")
    val quantityChange: Int,

    val note: String? = null
)

data class InventoryHistoryResponse(
    val id: UUID,
    val changeType: InventoryChangeType,
    val quantityChange: Int,
    val previousStock: Int,
    val newStock: Int,
    val note: String?,
    val referenceType: String?,
    val referenceId: UUID?,
    val changedByName: String?,
    val createdAt: Instant?
)
