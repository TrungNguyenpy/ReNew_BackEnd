package com.usedmarket.payment.controller

import com.usedmarket.payment.dto.PaymentIntentResponse
import com.usedmarket.payment.dto.PaymentResponse
import com.usedmarket.payment.service.PaymentService
import com.usedmarket.security.CustomUserDetails
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/orders/{orderId}/payment")
class PaymentController(
    private val paymentService: PaymentService
) {

    @PostMapping("/intent")
    fun createIntent(
        @PathVariable orderId: UUID,
        @AuthenticationPrincipal principal: CustomUserDetails
    ): PaymentIntentResponse = paymentService.createPaymentIntent(orderId, principal.user)

    @GetMapping
    fun getPayment(
        @PathVariable orderId: UUID,
        @AuthenticationPrincipal principal: CustomUserDetails
    ): PaymentResponse = paymentService.getPayment(orderId, principal.user)
}
