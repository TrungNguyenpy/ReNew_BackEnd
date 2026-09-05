package com.usedmarket.product.entity

import com.usedmarket.common.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

/**
 * One row of the condition score breakdown for a product, e.g.
 * ("Cosmetic", 85), ("Screen", 95), ("Battery", 87), ("Performance", 100), ("Accessories", 70).
 * Modeled as flexible key-value rows (rather than fixed columns) because the
 * relevant criteria differ by product category (a washing machine has no
 * "Screen"/"Battery" criteria, for example).
 * Product.conditionScore is the computed average of these items' scores.
 */
@Entity
@Table(name = "condition_score_items")
class ConditionScoreItem(

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    var product: Product,

    @Column(nullable = false, length = 100)
    var criterion: String,

    /** 0-100. */
    @Column(nullable = false)
    var score: Int,

    @Column(length = 500)
    var note: String? = null

) : BaseEntity()
