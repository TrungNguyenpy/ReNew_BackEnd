package com.usedmarket.order.entity

import com.usedmarket.common.entity.BaseEntity
import com.usedmarket.product.entity.Product
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.math.BigDecimal

/**
 * A line item within an Order. Product name/price/condition are snapshotted
 * at order time because:
 *  - used items are frequently unique (stock = 1) and get deactivated/deleted
 *    once sold, so the historical order must still render correctly, and
 *  - price/condition on the live Product can legitimately change over time
 *    for restocked/similar listings, which must not retroactively alter past orders.
 */
@Entity
@Table(name = "order_items")
class OrderItem(

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    var order: Order,

    /** Nullable: the referenced Product may be deleted later; the snapshot fields remain authoritative. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    var product: Product?,

    @Column(name = "product_name_snapshot", nullable = false, length = 255)
    var productNameSnapshot: String,

    @Column(name = "condition_snapshot", length = 20)
    var conditionSnapshot: String? = null,

    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    var unitPrice: BigDecimal,

    @Column(nullable = false)
    var quantity: Int = 1,

    @Column(nullable = false, precision = 12, scale = 2)
    var subtotal: BigDecimal

) : BaseEntity()
