package com.usedmarket.notification.dto

import com.usedmarket.notification.entity.NotificationType
import java.time.Instant
import java.util.UUID

data class NotificationResponse(
    val id: UUID,
    val type: NotificationType,
    val title: String,
    val message: String,
    val referenceType: String?,
    val referenceId: UUID?,
    val isRead: Boolean,
    val readAt: Instant?,
    val createdAt: Instant?
)
