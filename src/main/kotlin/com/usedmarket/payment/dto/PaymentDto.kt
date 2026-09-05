package com.usedmarket.payment.dto

import com.usedmarket.payment.entity.PaymentMethod
import com.usedmarket.payment.entity.PaymentStatus
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class PaymentIntentResponse(
    val orderId: UUID,
    /** Passed to Stripe.js on the frontend to complete the card payment. */
    val clientSecret: String,
    val amount: BigDecimal
)

data class PaymentResponse(
    val id: UUID,
    val orderId: UUID,
    val method: PaymentMethod,
    val status: PaymentStatus,
    val amount: BigDecimal,
    val paidAt: Instant?,
    val failureReason: String?
)
