package com.usedmarket.order.entity

enum class OrderStatus {
    PENDING,
    CONFIRMED,
    PROCESSING,
    PACKED,
    SHIPPED,
    DELIVERED,
    CANCELLED,
    RETURN_REQUESTED,
    RETURNED,
    REFUNDED
}
