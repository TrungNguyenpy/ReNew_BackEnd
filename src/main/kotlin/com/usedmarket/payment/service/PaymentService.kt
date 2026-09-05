package com.usedmarket.payment.service

import com.stripe.exception.SignatureVerificationException
import com.stripe.exception.StripeException
import com.stripe.model.PaymentIntent
import com.stripe.net.Webhook
import com.stripe.param.PaymentIntentCreateParams
import com.usedmarket.common.exception.BadRequestException
import com.usedmarket.common.exception.PaymentException
import com.usedmarket.common.exception.ResourceNotFoundException
import com.usedmarket.order.repository.OrderRepository
import com.usedmarket.order.service.OrderService
import com.usedmarket.payment.dto.PaymentIntentResponse
import com.usedmarket.payment.dto.PaymentResponse
import com.usedmarket.payment.entity.PaymentMethod
import com.usedmarket.payment.entity.PaymentStatus
import com.usedmarket.payment.repository.PaymentRepository
import com.usedmarket.user.entity.RoleName
import com.usedmarket.user.entity.User
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.RoundingMode
import java.time.Instant
import java.util.UUID

@Service
class PaymentService(
    private val orderRepository: OrderRepository,
    private val paymentRepository: PaymentRepository,
    private val orderService: OrderService,
    @Value("\${app.stripe.webhook-secret}") private val webhookSecret: String
) {

    private val logger = LoggerFactory.getLogger(PaymentService::class.java)

    /**
     * Creates a Stripe PaymentIntent for a STRIPE-method order and returns the
     * client_secret the frontend needs to complete the card payment with Stripe.js.
     * VND is a Stripe zero-decimal currency, so the amount is sent as-is (no *100).
     */
    fun createPaymentIntent(orderId: UUID, requester: User): PaymentIntentResponse {
        val order = orderRepository.findById(orderId)
            .orElseThrow { ResourceNotFoundException("Order not found with id: $orderId") }
        guardOwnership(order.customer.id!!, requester)

        if (order.paymentMethod != PaymentMethod.STRIPE) {
            throw BadRequestException("This order is not set up for Stripe payment")
        }
        if (order.paymentStatus == PaymentStatus.SUCCEEDED) {
            throw BadRequestException("This order has already been paid")
        }

        val payment = paymentRepository.findByOrderId(orderId)
            .orElseThrow { ResourceNotFoundException("Payment record not found for order: $orderId") }

        val params = PaymentIntentCreateParams.builder()
            .setAmount(order.totalAmount.setScale(0, RoundingMode.HALF_UP).toLong())
            .setCurrency("vnd")
            .putMetadata("orderId", orderId.toString())
            .putMetadata("orderNumber", order.orderNumber)
            .build()

        val intent = try {
            PaymentIntent.create(params)
        } catch (ex: StripeException) {
            logger.error("Stripe PaymentIntent creation failed for order {}", orderId, ex)
            throw PaymentException("Could not initialize payment with Stripe: ${ex.message}")
        }

        payment.stripePaymentIntentId = intent.id
        paymentRepository.save(payment)

        return PaymentIntentResponse(orderId = orderId, clientSecret = intent.clientSecret, amount = order.totalAmount)
    }

    fun getPayment(orderId: UUID, requester: User): PaymentResponse {
        val order = orderRepository.findById(orderId)
            .orElseThrow { ResourceNotFoundException("Order not found with id: $orderId") }
        guardOwnership(order.customer.id!!, requester)

        val payment = paymentRepository.findByOrderId(orderId)
            .orElseThrow { ResourceNotFoundException("Payment record not found for order: $orderId") }

        return PaymentResponse(
            id = payment.id!!,
            orderId = orderId,
            method = payment.method,
            status = payment.status,
            amount = payment.amount,
            paidAt = payment.paidAt,
            failureReason = payment.failureReason
        )
    }

    /**
     * Verifies the Stripe webhook signature and reacts to payment_intent events.
     * A bad/forged signature results in BadRequestException (400) so Stripe's retry
     * logic treats it as a rejected delivery rather than a transient server error.
     */
    @Transactional
    fun handleWebhookEvent(payload: String, signatureHeader: String) {
        val event = try {
            Webhook.constructEvent(payload, signatureHeader, webhookSecret)
        } catch (ex: SignatureVerificationException) {
            throw BadRequestException("Invalid Stripe webhook signature")
        }

        when (event.type) {
            "payment_intent.succeeded" ->
                handlePaymentIntentOutcome(event.dataObjectDeserializer.`object`.orElse(null), succeeded = true)
            "payment_intent.payment_failed" ->
                handlePaymentIntentOutcome(event.dataObjectDeserializer.`object`.orElse(null), succeeded = false)
            else -> logger.info("Ignoring unhandled Stripe event type: {}", event.type)
        }
    }

    private fun handlePaymentIntentOutcome(stripeObject: Any?, succeeded: Boolean) {
        val intent = stripeObject as? PaymentIntent ?: return
        val orderIdRaw = intent.metadata?.get("orderId") ?: return
        val orderId = try {
            UUID.fromString(orderIdRaw)
        } catch (ex: IllegalArgumentException) {
            logger.warn("Stripe PaymentIntent {} had an unparsable orderId metadata value", intent.id)
            return
        }

        val payment = paymentRepository.findByOrderId(orderId).orElse(null) ?: return
        payment.stripeChargeId = intent.latestCharge

        if (succeeded) {
            payment.status = PaymentStatus.SUCCEEDED
            payment.paidAt = Instant.now()
            paymentRepository.save(payment)
            orderService.markPaymentSucceeded(orderId)
        } else {
            payment.status = PaymentStatus.FAILED
            payment.failureReason = intent.lastPaymentError?.message
            paymentRepository.save(payment)
            orderService.markPaymentFailed(orderId)
        }
    }

    private fun guardOwnership(orderCustomerId: UUID, requester: User) {
        val isOwner = orderCustomerId == requester.id
        val isStaffOrAdmin = requester.role == RoleName.STAFF || requester.role == RoleName.ADMIN
        if (!isOwner && !isStaffOrAdmin) {
            throw ResourceNotFoundException("Order not found")
        }
    }
}
