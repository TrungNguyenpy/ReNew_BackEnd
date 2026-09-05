package com.usedmarket.product.entity

import com.usedmarket.catalog.entity.Brand
import com.usedmarket.catalog.entity.Category
import com.usedmarket.common.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.math.BigDecimal

@Entity
@Table(name = "products")
class Product(

    @Column(nullable = false, length = 255)
    var name: String,

    @Column(nullable = false, unique = true, length = 255)
    var slug: String,

    @Column(columnDefinition = "TEXT")
    var description: String? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    var category: Category,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "brand_id", nullable = false)
    var brand: Brand,

    @Column(length = 150)
    var model: String? = null,

    /** Current selling price. */
    @Column(nullable = false, precision = 12, scale = 2)
    var price: BigDecimal,

    /** Original retail price when the item was new — shown for price-drop comparison. */
    @Column(name = "original_price", precision = 12, scale = 2)
    var originalPrice: BigDecimal? = null,

    /**
     * Simple on-hand quantity for display/browsing purposes.
     * The authoritative, concurrency-safe stock ledger lives in the Inventory
     * entity (Batch 2D) — this field must stay in sync with it but is not
     * itself used for reservation locking.
     */
    @Column(name = "stock_quantity", nullable = false)
    var stockQuantity: Int = 0,

    // ---- Used-product specific information (spec section 3) ----

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var condition: ConditionGrade,

    /**
     * Overall 0-100 condition score, denormalized here for fast display on
     * listing/search pages (spec section 4: "Condition: Very Good - 87/100").
     * The per-criterion breakdown (cosmetic, screen, battery...) lives in
     * ConditionScoreItem; this field is the computed average, kept in sync
     * by the service layer whenever score items are created/updated.
     */
    @Column(name = "condition_score", precision = 5, scale = 2)
    var conditionScore: BigDecimal? = null,

    @Column(name = "manufacture_year")
    var manufactureYear: Int? = null,

    @Column(name = "purchase_year")
    var purchaseYear: Int? = null,

    /** Free-text usage duration, e.g. "8 months", "2 years". */
    @Column(name = "usage_duration", length = 100)
    var usageDuration: String? = null,

    @Column(name = "cosmetic_condition", length = 500)
    var cosmeticCondition: String? = null,

    @Column(name = "functional_condition", length = 500)
    var functionalCondition: String? = null,

    @Column(name = "known_defects", columnDefinition = "TEXT")
    var knownDefects: String? = null,

    @Column(name = "repair_history", columnDefinition = "TEXT")
    var repairHistory: String? = null,

    @Column(name = "accessories_included", length = 500)
    var accessoriesIncluded: String? = null,

    @Column(nullable = false)
    var isActive: Boolean = true,

    @Column(nullable = false)
    var isHidden: Boolean = false

) : BaseEntity() {

    /**
     * Optimistic locking version.
     * Critical per spec section 20: used items often have stock = 1, so two
     * customers checking out the same unique item concurrently must not both
     * succeed. This @Version field lets Hibernate detect concurrent updates
     * to stockQuantity and reject the losing transaction with
     * OptimisticLockException, which the service layer (Batch 2D/Phase 8)
     * will translate into a proper "item no longer available" error.
     */
    @Version
    @Column(nullable = false)
    var version: Long = 0
}
