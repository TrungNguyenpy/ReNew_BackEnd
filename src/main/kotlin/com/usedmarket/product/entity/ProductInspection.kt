package com.usedmarket.product.entity

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
import java.time.Instant

/**
 * Header record for a single inspection pass over a product, performed by staff.
 * The detailed checklist (screen, camera, battery, IMEI...) lives in InspectionItem.
 */
@Entity
@Table(name = "product_inspections")
class ProductInspection(

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    var product: Product,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "inspector_id", nullable = false)
    var inspector: User,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: InspectionStatus = InspectionStatus.PENDING,

    @Column(name = "inspection_date")
    var inspectionDate: Instant? = null,

    /**
     * Overall 0-100 score for this inspection pass, computed from InspectionItem
     * results by the service layer (e.g. weighted PASS/FAIL/WARNING ratio).
     */
    @Column(name = "inspection_score")
    var inspectionScore: Int? = null,

    /** Free-text summary shown in the public inspection report on the product page. */
    @Column(name = "result_summary", columnDefinition = "TEXT")
    var resultSummary: String? = null,

    /** Internal-only notes, never shown to customers. */
    @Column(name = "internal_notes", columnDefinition = "TEXT")
    var internalNotes: String? = null,

    /** Whether this report (or a redacted version of it) is visible on the public product page. */
    @Column(name = "is_public", nullable = false)
    var isPublic: Boolean = false

) : BaseEntity()
