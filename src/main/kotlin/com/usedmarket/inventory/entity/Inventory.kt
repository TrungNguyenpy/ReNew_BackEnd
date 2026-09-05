package com.usedmarket.inventory.entity

import com.usedmarket.common.entity.BaseEntity
import com.usedmarket.product.entity.Product
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import jakarta.persistence.Version

/**
 * Authoritative stock ledger for a product. Product.stockQuantity is a
 * denormalized copy of availableStock for fast listing display; this table
 * is the source of truth and the only place stock reservation/locking logic
 * (spec section 20) should operate on.
 */
@Entity
@Table(name = "inventories")
class Inventory(

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false, unique = true)
    var product: Product,

    /** Total units physically on hand (available + reserved + damaged, excluding sold). */
    @Column(name = "current_stock", nullable = false)
    var currentStock: Int = 0,

    /** Units that can be added to a cart / purchased right now. */
    @Column(name = "available_stock", nullable = false)
    var availableStock: Int = 0,

    /** Units held in an active checkout (payment pending) — not yet sold, not available to others. */
    @Column(name = "reserved_stock", nullable = false)
    var reservedStock: Int = 0,

    @Column(name = "sold_stock", nullable = false)
    var soldStock: Int = 0,

    @Column(name = "damaged_stock", nullable = false)
    var damagedStock: Int = 0

) : BaseEntity() {

    /**
     * Optimistic lock for concurrent stock mutations. Combined with
     * Product.version, this is the mechanism backing spec section 20:
     * when two customers attempt to reserve the same stock=1 item
     * simultaneously, only one transaction wins; the other gets
     * OptimisticLockException and must retry / see "no longer available".
     */
    @Version
    @Column(nullable = false)
    var version: Long = 0
}
