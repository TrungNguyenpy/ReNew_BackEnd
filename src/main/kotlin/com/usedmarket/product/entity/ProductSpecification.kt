package com.usedmarket.product.entity

import com.usedmarket.common.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

/**
 * Key-value technical specification row, e.g. ("RAM", "8GB"), ("Screen size", "6.1 inch").
 * Modeled as key-value rather than fixed columns because each product category
 * (smartphone, laptop, refrigerator...) has a completely different spec set.
 */
@Entity
@Table(name = "product_specifications")
class ProductSpecification(

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    var product: Product,

    @Column(name = "spec_key", nullable = false, length = 150)
    var specKey: String,

    @Column(name = "spec_value", nullable = false, length = 500)
    var specValue: String,

    @Column(name = "display_order", nullable = false)
    var displayOrder: Int = 0

) : BaseEntity()
