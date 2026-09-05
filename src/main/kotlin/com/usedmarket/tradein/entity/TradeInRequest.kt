package com.usedmarket.tradein.entity

import com.usedmarket.catalog.entity.Category
import com.usedmarket.common.entity.BaseEntity
import com.usedmarket.product.entity.ConditionGrade
import com.usedmarket.user.entity.User
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table
import java.math.BigDecimal

@Entity
@Table(name = "trade_in_requests")
class TradeInRequest(

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    var customer: User,

    @Column(name = "product_name", nullable = false, length = 255)
    var productName: String,

    /** Free-text brand name — the item may not exist in the Brand catalog yet. */
    @Column(length = 150)
    var brand: String? = null,

    @Column(length = 150)
    var model: String? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    var category: Category? = null,

    @Column(name = "purchase_year")
    var purchaseYear: Int? = null,

    @Column(name = "usage_duration", length = 100)
    var usageDuration: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    var condition: ConditionGrade? = null,

    @Column(columnDefinition = "TEXT")
    var description: String? = null,

    @Column(name = "expected_price", precision = 12, scale = 2)
    var expectedPrice: BigDecimal? = null,

    /** Set by staff after inspection. */
    @Column(name = "offered_price", precision = 12, scale = 2)
    var offeredPrice: BigDecimal? = null,

    @Column(name = "contact_phone", nullable = false, length = 20)
    var contactPhone: String,

    @Column(name = "contact_email", length = 255)
    var contactEmail: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: TradeInStatus = TradeInStatus.PENDING,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "inspected_by")
    var inspectedBy: User? = null,

    @Column(name = "inspection_note", columnDefinition = "TEXT")
    var inspectionNote: String? = null

) : BaseEntity() {

    @OneToMany(mappedBy = "tradeInRequest", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
    var attachments: MutableList<TradeInItem> = mutableListOf()
}
