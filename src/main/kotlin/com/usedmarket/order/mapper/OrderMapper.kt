package com.usedmarket.order.mapper

import com.usedmarket.order.dto.OrderItemResponse
import com.usedmarket.order.dto.OrderResponse
import com.usedmarket.order.dto.OrderStatusHistoryResponse
import com.usedmarket.order.dto.OrderSummaryResponse
import com.usedmarket.order.entity.Order
import com.usedmarket.order.entity.OrderItem
import com.usedmarket.order.entity.OrderStatusHistory
import org.springframework.stereotype.Component

@Component
class OrderMapper {

    fun toItemResponse(item: OrderItem): OrderItemResponse =
        OrderItemResponse(
            id = item.id!!,
            productId = item.product?.id,
            productNameSnapshot = item.productNameSnapshot,
            conditionSnapshot = item.conditionSnapshot,
            unitPrice = item.unitPrice,
            quantity = item.quantity,
            subtotal = item.subtotal
        )

    fun toResponse(order: Order, items: List<OrderItem>): OrderResponse =
        OrderResponse(
            id = order.id!!,
            orderNumber = order.orderNumber,
            status = order.status,
            recipientName = order.recipientName,
            recipientPhone = order.recipientPhone,
            shippingAddressLine = order.shippingAddressLine,
            shippingWard = order.shippingWard,
            shippingDistrict = order.shippingDistrict,
            shippingProvince = order.shippingProvince,
            note = order.note,
            subtotal = order.subtotal,
            shippingFee = order.shippingFee,
            discountAmount = order.discountAmount,
            totalAmount = order.totalAmount,
            couponCode = order.couponCode,
            paymentMethod = order.paymentMethod,
            paymentStatus = order.paymentStatus,
            items = items.map(::toItemResponse),
            createdAt = order.createdAt
        )

    fun toSummaryResponse(order: Order, itemCount: Int): OrderSummaryResponse =
        OrderSummaryResponse(
            id = order.id!!,
            orderNumber = order.orderNumber,
            status = order.status,
            totalAmount = order.totalAmount,
            itemCount = itemCount,
            createdAt = order.createdAt
        )

    fun toHistoryResponse(history: OrderStatusHistory): OrderStatusHistoryResponse =
        OrderStatusHistoryResponse(
            id = history.id!!,
            status = history.status,
            note = history.note,
            changedByName = history.changedBy?.fullName,
            createdAt = history.createdAt
        )
}
