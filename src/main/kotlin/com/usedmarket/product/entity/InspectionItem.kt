package com.usedmarket.product.entity

import com.usedmarket.common.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

/**
 * One checklist row within a ProductInspection, e.g. ("Screen", PASS), ("Battery", WARNING, "82% health").
 * The specific set of items checked depends on the product category
 * (smartphone checklist differs from a washing machine checklist), so this
 * is a flexible row per item rather than fixed columns.
 */
@Entity
@Table(name = "inspection_items")
class InspectionItem(

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inspection_id", nullable = false)
    var inspection: ProductInspection,

    /** e.g. "Screen", "Touch", "Camera front", "IMEI". */
    @Column(nullable = false, length = 100)
    var itemName: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: InspectionItemStatus,

    @Column(length = 500)
    var note: String? = null,

    @Column(name = "display_order", nullable = false)
    var displayOrder: Int = 0

) : BaseEntity()
