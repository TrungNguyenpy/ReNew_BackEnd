package com.usedmarket.coupon.repository

import com.usedmarket.coupon.entity.Coupon
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional
import java.util.UUID

interface CouponRepository : JpaRepository<Coupon, UUID> {

    fun findByCode(code: String): Optional<Coupon>

    fun existsByCode(code: String): Boolean
}
