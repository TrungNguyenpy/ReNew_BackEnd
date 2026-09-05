package com.usedmarket.coupon.entity

import com.usedmarket.common.entity.BaseEntity
import com.usedmarket.order.entity.Order
import com.usedmarket.user.entity.User
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.math.BigDecimal

@Entity
@Table(name = "coupon_usages")
class CouponUsage(

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "coupon_id", nullable = false)
    var coupon: Coupon,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    var order: Order,

    @Column(name = "discount_applied", nullable = false, precision = 12, scale = 2)
    var discountApplied: BigDecimal

) : BaseEntity()
