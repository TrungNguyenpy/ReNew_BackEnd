package com.usedmarket.coupon.repository

import com.usedmarket.coupon.entity.CouponUsage
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface CouponUsageRepository : JpaRepository<CouponUsage, UUID> {

    fun countByCouponIdAndUserId(couponId: UUID, userId: UUID): Long

    fun findByOrderId(orderId: UUID): List<CouponUsage>
}
