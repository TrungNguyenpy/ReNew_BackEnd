package com.usedmarket.shipment.entity

import com.usedmarket.common.entity.BaseEntity
import com.usedmarket.order.entity.Order
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import java.time.Instant
import java.time.LocalDate

@Entity
@Table(name = "shipments")
class Shipment(

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    var order: Order,

    @Column(length = 100)
    var carrier: String? = null,

    @Column(name = "tracking_number", length = 100)
    var trackingNumber: String? = null,

    @Column(name = "estimated_delivery_date")
    var estimatedDeliveryDate: LocalDate? = null,

    @Column(name = "shipped_at")
    var shippedAt: Instant? = null,

    @Column(name = "delivered_at")
    var deliveredAt: Instant? = null

) : BaseEntity()
