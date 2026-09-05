package com.usedmarket.inventory.service

import com.usedmarket.common.exception.BadRequestException
import com.usedmarket.common.exception.InsufficientStockException
import com.usedmarket.common.exception.ResourceNotFoundException
import com.usedmarket.inventory.dto.InventoryAdjustRequest
import com.usedmarket.inventory.dto.InventoryHistoryResponse
import com.usedmarket.inventory.dto.InventoryResponse
import com.usedmarket.inventory.entity.Inventory
import com.usedmarket.inventory.entity.InventoryChangeType
import com.usedmarket.inventory.entity.InventoryHistory
import com.usedmarket.inventory.mapper.InventoryMapper
import com.usedmarket.inventory.repository.InventoryHistoryRepository
import com.usedmarket.inventory.repository.InventoryRepository
import com.usedmarket.product.entity.Product
import com.usedmarket.product.repository.ProductRepository
import com.usedmarket.user.entity.User
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class InventoryService(
    private val inventoryRepository: InventoryRepository,
    private val inventoryHistoryRepository: InventoryHistoryRepository,
    private val productRepository: ProductRepository,
    private val inventoryMapper: InventoryMapper
) {

    // ---------------------------------------------------------------
    // Initialization (called from ProductService.create)
    // ---------------------------------------------------------------

    /** Creates the ledger row for a brand-new product, with an initial PURCHASE history entry. */
    @Transactional
    fun initializeForProduct(product: Product, initialStock: Int, actingUser: User): Inventory {
        val inventory = Inventory(
            product = product,
            currentStock = initialStock,
            availableStock = initialStock,
            reservedStock = 0,
            soldStock = 0,
            damagedStock = 0
        )
        inventoryRepository.save(inventory)

        if (initialStock > 0) {
            recordHistory(
                inventory, InventoryChangeType.PURCHASE, initialStock,
                previousStock = 0, newStock = initialStock,
                note = "Initial stock recorded on product creation", changedBy = actingUser
            )
        }
        return inventory
    }

    // ---------------------------------------------------------------
    // Read
    // ---------------------------------------------------------------

    fun getByProductId(productId: UUID): InventoryResponse =
        inventoryMapper.toResponse(getOrBackfillEntity(productId))

    fun getHistory(productId: UUID): List<InventoryHistoryResponse> {
        val inventory = getOrBackfillEntity(productId)
        return inventoryHistoryRepository.findByInventoryIdOrderByCreatedAtDesc(inventory.id!!)
            .map(inventoryMapper::toHistoryResponse)
    }

    // ---------------------------------------------------------------
    // Manual adjustment (STAFF/ADMIN)
    // ---------------------------------------------------------------

    @Transactional
    fun adjust(productId: UUID, request: InventoryAdjustRequest, actingUser: User): InventoryResponse {
        val inventory = inventoryRepository.findByProductIdForUpdate(productId)
            .orElseGet { getOrBackfillEntity(productId) }

        val previousAvailable = inventory.availableStock

        when (request.changeType) {
            InventoryChangeType.PURCHASE -> {
                if (request.quantityChange <= 0) throw BadRequestException("PURCHASE quantity must be positive")
                inventory.availableStock += request.quantityChange
                inventory.currentStock += request.quantityChange
            }
            InventoryChangeType.RETURN -> {
                if (request.quantityChange <= 0) throw BadRequestException("RETURN quantity must be positive")
                inventory.availableStock += request.quantityChange
                inventory.soldStock = (inventory.soldStock - request.quantityChange).coerceAtLeast(0)
                inventory.currentStock += request.quantityChange
            }
            InventoryChangeType.SALE -> {
                if (request.quantityChange <= 0) throw BadRequestException("SALE quantity must be positive")
                if (inventory.availableStock < request.quantityChange) {
                    throw InsufficientStockException("Not enough available stock to record this sale")
                }
                inventory.availableStock -= request.quantityChange
                inventory.soldStock += request.quantityChange
                inventory.currentStock -= request.quantityChange
            }
            InventoryChangeType.DAMAGE -> {
                if (request.quantityChange <= 0) throw BadRequestException("DAMAGE quantity must be positive")
                if (inventory.availableStock < request.quantityChange) {
                    throw InsufficientStockException("Not enough available stock to mark as damaged")
                }
                inventory.availableStock -= request.quantityChange
                inventory.damagedStock += request.quantityChange
                // currentStock unchanged: the units are still physically on hand, just unsellable.
            }
            InventoryChangeType.ADJUSTMENT -> {
                val newAvailable = inventory.availableStock + request.quantityChange
                if (newAvailable < 0) {
                    throw InsufficientStockException("Adjustment would result in negative available stock")
                }
                inventory.availableStock = newAvailable
                inventory.currentStock += request.quantityChange
            }
        }

        inventoryRepository.save(inventory)
        recordHistory(
            inventory, request.changeType, request.quantityChange,
            previousStock = previousAvailable, newStock = inventory.availableStock,
            note = request.note, changedBy = actingUser
        )

        syncProductStockQuantity(inventory)

        return inventoryMapper.toResponse(inventory)
    }

    // ---------------------------------------------------------------
    // Reserve / release / confirm-sale primitives
    // Not yet exposed via REST — these exist for Cart (Phase 7) and
    // Order/Checkout (Phase 8) to call directly, using the pessimistic
    // lock repository method to satisfy spec section 20 (no double-sell
    // of a stock=1 unique item under concurrent checkout).
    // ---------------------------------------------------------------

    @Transactional
    fun reserveStock(productId: UUID, quantity: Int): Inventory {
        if (quantity <= 0) throw BadRequestException("Quantity to reserve must be positive")
        val inventory = inventoryRepository.findByProductIdForUpdate(productId)
            .orElseThrow { ResourceNotFoundException("Inventory not found for product: $productId") }

        if (inventory.availableStock < quantity) {
            throw InsufficientStockException("Not enough stock available to reserve")
        }
        inventory.availableStock -= quantity
        inventory.reservedStock += quantity
        inventoryRepository.save(inventory)
        syncProductStockQuantity(inventory)
        return inventory
    }

    @Transactional
    fun releaseStock(productId: UUID, quantity: Int): Inventory {
        if (quantity <= 0) throw BadRequestException("Quantity to release must be positive")
        val inventory = inventoryRepository.findByProductIdForUpdate(productId)
            .orElseThrow { ResourceNotFoundException("Inventory not found for product: $productId") }

        val releaseAmount = quantity.coerceAtMost(inventory.reservedStock)
        inventory.reservedStock -= releaseAmount
        inventory.availableStock += releaseAmount
        inventoryRepository.save(inventory)
        syncProductStockQuantity(inventory)
        return inventory
    }

    @Transactional
    fun confirmSale(productId: UUID, quantity: Int, orderId: UUID, actingUser: User?): Inventory {
        if (quantity <= 0) throw BadRequestException("Quantity to confirm must be positive")
        val inventory = inventoryRepository.findByProductIdForUpdate(productId)
            .orElseThrow { ResourceNotFoundException("Inventory not found for product: $productId") }

        val confirmAmount = quantity.coerceAtMost(inventory.reservedStock)
        val previousAvailable = inventory.availableStock

        inventory.reservedStock -= confirmAmount
        inventory.soldStock += confirmAmount
        inventory.currentStock -= confirmAmount
        inventoryRepository.save(inventory)

        recordHistory(
            inventory, InventoryChangeType.SALE, confirmAmount,
            previousStock = previousAvailable, newStock = inventory.availableStock,
            note = "Order confirmed", changedBy = actingUser,
            referenceType = "ORDER", referenceId = orderId
        )
        syncProductStockQuantity(inventory)
        return inventory
    }

    /**
     * Moves stock from soldStock back to availableStock — used when a CONFIRMED
     * order is later cancelled, or when a delivered order is returned (Phase 8).
     */
    @Transactional
    fun returnSoldStock(productId: UUID, quantity: Int, orderId: UUID?, actingUser: User?): Inventory {
        if (quantity <= 0) throw BadRequestException("Quantity to return must be positive")
        val inventory = inventoryRepository.findByProductIdForUpdate(productId)
            .orElseThrow { ResourceNotFoundException("Inventory not found for product: $productId") }

        val returnAmount = quantity.coerceAtMost(inventory.soldStock)
        val previousAvailable = inventory.availableStock

        inventory.soldStock -= returnAmount
        inventory.availableStock += returnAmount
        inventory.currentStock += returnAmount
        inventoryRepository.save(inventory)

        recordHistory(
            inventory, InventoryChangeType.RETURN, returnAmount,
            previousStock = previousAvailable, newStock = inventory.availableStock,
            note = "Order cancelled or returned", changedBy = actingUser,
            referenceType = "ORDER", referenceId = orderId
        )
        syncProductStockQuantity(inventory)
        return inventory
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    /** Backfills an Inventory row for products created before Phase 6 wired this module in. */
    private fun getOrBackfillEntity(productId: UUID): Inventory =
        inventoryRepository.findByProductId(productId).orElseGet {
            val product = productRepository.findById(productId)
                .orElseThrow { ResourceNotFoundException("Product not found with id: $productId") }
            val backfilled = Inventory(
                product = product,
                currentStock = product.stockQuantity,
                availableStock = product.stockQuantity,
                reservedStock = 0,
                soldStock = 0,
                damagedStock = 0
            )
            inventoryRepository.save(backfilled)
        }

    private fun syncProductStockQuantity(inventory: Inventory) {
        val product = inventory.product
        product.stockQuantity = inventory.availableStock
        productRepository.save(product)
    }

    private fun recordHistory(
        inventory: Inventory,
        changeType: InventoryChangeType,
        quantityChange: Int,
        previousStock: Int,
        newStock: Int,
        note: String?,
        changedBy: User?,
        referenceType: String? = null,
        referenceId: UUID? = null
    ) {
        val history = InventoryHistory(
            inventory = inventory,
            changeType = changeType,
            quantityChange = quantityChange,
            previousStock = previousStock,
            newStock = newStock,
            note = note,
            referenceType = referenceType,
            referenceId = referenceId,
            changedBy = changedBy
        )
        inventoryHistoryRepository.save(history)
    }
}
