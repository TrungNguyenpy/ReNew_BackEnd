package com.usedmarket.product.entity

import com.usedmarket.common.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import java.time.LocalDate

/**
 * Warranty terms attached to a product listing.
 * One-to-one with Product: each listed item has exactly one warranty record
 * (which may be WarrantyType.NONE). Start/end dates here describe the
 * warranty as advertised on the listing; when an order is delivered, the
 * actual customer-facing warranty period will be tracked against the
 * Order/OrderItem in a later phase.
 */
@Entity
@Table(name = "warranties")
class Warranty(

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false, unique = true)
    var product: Product,

    @Enumerated(EnumType.STRING)
    @Column(name = "warranty_type", nullable = false, length = 20)
    var warrantyType: WarrantyType = WarrantyType.NONE,

    @Column(name = "duration_months")
    var durationMonths: Int? = null,

    @Column(name = "start_date")
    var startDate: LocalDate? = null,

    @Column(name = "end_date")
    var endDate: LocalDate? = null,

    @Column(name = "policy", columnDefinition = "TEXT")
    var policy: String? = null

) : BaseEntity()
