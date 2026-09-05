package com.usedmarket.payment.config

import com.stripe.Stripe
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

/**
 * The Stripe Java SDK is configured via a single static field (Stripe.apiKey)
 * rather than an injectable client bean — this component just sets that field
 * once at application startup from our own configuration.
 */
@Component
class StripeConfig(
    @Value("\${app.stripe.secret-key}") private val secretKey: String
) {

    @PostConstruct
    fun init() {
        if (secretKey.isNotBlank()) {
            Stripe.apiKey = secretKey
        }
        // If blank (no key configured yet), Stripe.apiKey stays unset — any actual
        // payment-intent creation will then fail clearly rather than silently using
        // a stale/wrong key. Every other endpoint in the app is unaffected.
    }
}
