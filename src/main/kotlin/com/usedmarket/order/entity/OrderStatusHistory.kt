package com.usedmarket.order.entity

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

/**
 * Immutable timeline entry for order tracking (spec section 3 "Order tracking":
 * Order created → Confirmed → Processing → Packed → Shipped → Delivered).
 * BaseEntity.createdAt serves as the timestamp of this status change.
 */
@Entity
@Table(name = "order_status_histories")
class OrderStatusHistory(

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    var order: Order,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: OrderStatus,

    @Column(length = 500)
    var note: String? = null,

    /** Null when the transition was made by the system (e.g. automatic timeout cancellation). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by")
    var changedBy: User? = null

) : BaseEntity()
