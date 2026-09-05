package com.usedmarket.inventory.entity

import com.usedmarket.common.entity.BaseEntity
import com.usedmarket.user.entity.User
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.util.UUID

/**
 * Immutable audit record created every time Inventory changes (spec section 7:
 * "Every inventory change must create an inventory history record").
 * referenceType/referenceId loosely point at the entity that triggered the
 * change (e.g. "ORDER" + orderId, "TRADE_IN" + tradeInRequestId) without a
 * hard foreign key, since the source can be any of several unrelated tables.
 */
@Entity
@Table(name = "inventory_histories")
class InventoryHistory(

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inventory_id", nullable = false)
    var inventory: Inventory,

    @Enumerated(EnumType.STRING)
    @Column(name = "change_type", nullable = false, length = 20)
    var changeType: InventoryChangeType,

    /** Signed delta applied to availableStock, e.g. -1 for a sale, +5 for a purchase intake. */
    @Column(name = "quantity_change", nullable = false)
    var quantityChange: Int,

    @Column(name = "previous_stock", nullable = false)
    var previousStock: Int,

    @Column(name = "new_stock", nullable = false)
    var newStock: Int,

    @Column(length = 500)
    var note: String? = null,

    /** e.g. "ORDER", "TRADE_IN", "MANUAL_ADJUSTMENT". */
    @Column(name = "reference_type", length = 50)
    var referenceType: String? = null,

    @Column(name = "reference_id")
    var referenceId: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by")
    var changedBy: User? = null

) : BaseEntity()
