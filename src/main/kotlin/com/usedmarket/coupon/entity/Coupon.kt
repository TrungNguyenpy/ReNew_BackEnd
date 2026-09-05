package com.usedmarket.coupon.entity

import com.usedmarket.common.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant

@Entity
@Table(name = "coupons")
class Coupon(

    @Column(nullable = false, unique = true, length = 50)
    var code: String,

    @Column(length = 500)
    var description: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false, length = 20)
    var discountType: DiscountType,

    /**
     * Meaning depends on discountType: percentage points (0-100) for PERCENTAGE,
     * a currency amount for FIXED_AMOUNT, null/ignored for FREE_SHIPPING.
     */
    @Column(name = "discount_value", precision = 12, scale = 2)
    var discountValue: BigDecimal? = null,

    @Column(name = "min_order_value", precision = 12, scale = 2)
    var minOrderValue: BigDecimal? = null,

    @Column(name = "max_discount_amount", precision = 12, scale = 2)
    var maxDiscountAmount: BigDecimal? = null,

    @Column(name = "start_date", nullable = false)
    var startDate: Instant,

    @Column(name = "end_date", nullable = false)
    var endDate: Instant,

    /** Null = unlimited total uses. */
    @Column(name = "max_usage")
    var maxUsage: Int? = null,

    @Column(name = "current_usage", nullable = false)
    var currentUsage: Int = 0,

    /** Null = no per-user limit. */
    @Column(name = "per_user_limit")
    var perUserLimit: Int? = null,

    @Column(nullable = false)
    var isActive: Boolean = true

) : BaseEntity()
