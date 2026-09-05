package com.usedmarket.order.dto

import com.usedmarket.order.entity.OrderStatus
import com.usedmarket.payment.entity.PaymentMethod
import com.usedmarket.payment.entity.PaymentStatus
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class CheckoutRequest(
    @field:NotBlank(message = "Recipient name is required")
    val recipientName: String,

    @field:NotBlank(message = "Recipient phone is required")
    val recipientPhone: String,

    @field:NotBlank(message = "Address line is required")
    val shippingAddressLine: String,

    @field:NotBlank(message = "Ward is required")
    val shippingWard: String,

    @field:NotBlank(message = "District is required")
    val shippingDistrict: String,

    @field:NotBlank(message = "Province is required")
    val shippingProvince: String,

    @field:Size(max = 500, message = "Note must be at most 500 characters")
    val note: String? = null,

    @field:NotNull(message = "Payment method is required")
    val paymentMethod: PaymentMethod,

    val couponCode: String? = null
)

data class OrderItemResponse(
    val id: UUID,
    val productId: UUID?,
    val productNameSnapshot: String,
    val conditionSnapshot: String?,
    val unitPrice: BigDecimal,
    val quantity: Int,
    val subtotal: BigDecimal
)

data class OrderResponse(
    val id: UUID,
    val orderNumber: String,
    val status: OrderStatus,
    val recipientName: String,
    val recipientPhone: String,
    val shippingAddressLine: String,
    val shippingWard: String,
    val shippingDistrict: String,
    val shippingProvince: String,
    val note: String?,
    val subtotal: BigDecimal,
    val shippingFee: BigDecimal,
    val discountAmount: BigDecimal,
    val totalAmount: BigDecimal,
    val couponCode: String?,
    val paymentMethod: PaymentMethod,
    val paymentStatus: PaymentStatus,
    val items: List<OrderItemResponse>,
    val createdAt: Instant?
)

data class OrderSummaryResponse(
    val id: UUID,
    val orderNumber: String,
    val status: OrderStatus,
    val totalAmount: BigDecimal,
    val itemCount: Int,
    val createdAt: Instant?
)

data class OrderStatusUpdateRequest(
    @field:NotNull(message = "Status is required")
    val status: OrderStatus,

    val note: String? = null
)

data class OrderStatusHistoryResponse(
    val id: UUID,
    val status: OrderStatus,
    val note: String?,
    val changedByName: String?,
    val createdAt: Instant?
)
