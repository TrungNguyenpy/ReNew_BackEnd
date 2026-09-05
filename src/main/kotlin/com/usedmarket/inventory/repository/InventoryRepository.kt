package com.usedmarket.inventory.repository

import com.usedmarket.inventory.entity.Inventory
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Query
import jakarta.persistence.LockModeType
import java.util.Optional
import java.util.UUID

interface InventoryRepository : JpaRepository<Inventory, UUID> {

    fun findByProductId(productId: UUID): Optional<Inventory>

    /**
     * Pessimistic write lock variant for the checkout critical section
     * (spec section 20). The service layer uses this when reserving stock
     * for a stock=1 unique item to guarantee only one concurrent transaction
     * can proceed, as a belt-and-braces complement to @Version optimistic locking.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT i FROM Inventory i WHERE i.product.id = :productId")
    fun findByProductIdForUpdate(productId: UUID): Optional<Inventory>
}
