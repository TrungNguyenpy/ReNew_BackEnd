package com.usedmarket.payment.entity

import com.usedmarket.common.entity.BaseEntity
import com.usedmarket.order.entity.Order
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToOne
import jakarta.persistence.Table
import java.math.BigDecimal
import java.time.Instant

@Entity
@Table(name = "payments")
class Payment(

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false, unique = true)
    var order: Order,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var method: PaymentMethod,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: PaymentStatus = PaymentStatus.PENDING,

    @Column(nullable = false, precision = 12, scale = 2)
    var amount: BigDecimal,

    /** Stripe PaymentIntent id, only set when method = STRIPE. */
    @Column(name = "stripe_payment_intent_id", length = 255)
    var stripePaymentIntentId: String? = null,

    @Column(name = "stripe_charge_id", length = 255)
    var stripeChargeId: String? = null,

    @Column(name = "paid_at")
    var paidAt: Instant? = null,

    @Column(name = "failure_reason", length = 500)
    var failureReason: String? = null

) : BaseEntity()
