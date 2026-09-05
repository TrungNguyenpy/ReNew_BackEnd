package com.usedmarket.payment.repository

import com.usedmarket.payment.entity.Payment
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional
import java.util.UUID

interface PaymentRepository : JpaRepository<Payment, UUID> {

    fun findByOrderId(orderId: UUID): Optional<Payment>

    fun findByStripePaymentIntentId(stripePaymentIntentId: String): Optional<Payment>
}
