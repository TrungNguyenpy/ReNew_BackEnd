package com.usedmarket.payment.controller

import com.usedmarket.payment.service.PaymentService
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.nio.charset.StandardCharsets

/**
 * Deliberately reads the raw body via HttpServletRequest rather than binding it
 * with @RequestBody: Stripe's signature check requires the exact original bytes,
 * and letting Spring's message-converter machinery touch the body first (even as
 * a plain String target) risks subtle re-encoding that would break verification.
 */
@RestController
@RequestMapping("/api/webhooks/stripe")
class StripeWebhookController(
    private val paymentService: PaymentService
) {

    @PostMapping
    fun handleWebhook(
        request: HttpServletRequest,
        @RequestHeader("Stripe-Signature") signatureHeader: String
    ): ResponseEntity<Void> {
        val payload = request.inputStream.bufferedReader(StandardCharsets.UTF_8).use { it.readText() }
        paymentService.handleWebhookEvent(payload, signatureHeader)
        return ResponseEntity.ok().build()
    }
}
