package com.usedmarket.notification.entity

enum class NotificationType {
    ORDER_CREATED,
    ORDER_CONFIRMED,
    ORDER_SHIPPED,
    ORDER_DELIVERED,
    ORDER_CANCELLED,
    PAYMENT_SUCCESS,
    PAYMENT_FAILED,
    WARRANTY_EXPIRING,
    WISHLIST_PRICE_DROP
}
