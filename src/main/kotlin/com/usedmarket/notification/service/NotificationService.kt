package com.usedmarket.notification.service

import com.usedmarket.common.exception.ForbiddenException
import com.usedmarket.common.exception.ResourceNotFoundException
import com.usedmarket.notification.dto.NotificationResponse
import com.usedmarket.notification.entity.Notification
import com.usedmarket.notification.entity.NotificationType
import com.usedmarket.notification.repository.NotificationRepository
import com.usedmarket.user.entity.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
class NotificationService(
    private val notificationRepository: NotificationRepository
) {

    // ---------------------------------------------------------------
    // Read (owner only)
    // ---------------------------------------------------------------

    fun getMyNotifications(userId: UUID, page: Int, size: Int): Page<NotificationResponse> {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable).map(::toResponse)
    }

    fun getUnreadCount(userId: UUID): Long = notificationRepository.countByUserIdAndIsReadFalse(userId)

    @Transactional
    fun markAsRead(notificationId: UUID, requester: User): NotificationResponse {
        val notification = notificationRepository.findById(notificationId)
            .orElseThrow { ResourceNotFoundException("Notification not found with id: $notificationId") }
        if (notification.user.id != requester.id) {
            throw ForbiddenException("This notification does not belong to you")
        }
        if (!notification.isRead) {
            notification.isRead = true
            notification.readAt = Instant.now()
            notificationRepository.save(notification)
        }
        return toResponse(notification)
    }

    @Transactional
    fun markAllAsRead(userId: UUID) {
        val pageable = PageRequest.of(0, Int.MAX_VALUE - 1, Sort.by(Sort.Direction.DESC, "createdAt"))
        val unread = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
            .content.filter { !it.isRead }
        val now = Instant.now()
        unread.forEach {
            it.isRead = true
            it.readAt = now
        }
        notificationRepository.saveAll(unread)
    }

    // ---------------------------------------------------------------
    // Write (called internally by other services when their event happens)
    // ---------------------------------------------------------------

    /**
     * Creates a notification for a user. Called from OrderService, PaymentService, and
     * ProductService at the exact moment their respective event occurs (order created/
     * confirmed/shipped/delivered/cancelled, payment succeeded/failed, wishlist price drop).
     * Deliberately fire-and-forget from the caller's perspective — a notification failure
     * should never roll back the business transaction that triggered it, so this method
     * does not throw for expected conditions and callers do not need to handle a result.
     */
    @Transactional
    fun notify(
        user: User,
        type: NotificationType,
        title: String,
        message: String,
        referenceType: String? = null,
        referenceId: UUID? = null
    ) {
        notificationRepository.save(
            Notification(
                user = user,
                type = type,
                title = title,
                message = message,
                referenceType = referenceType,
                referenceId = referenceId
            )
        )
    }

    private fun toResponse(notification: Notification): NotificationResponse =
        NotificationResponse(
            id = notification.id!!,
            type = notification.type,
            title = notification.title,
            message = notification.message,
            referenceType = notification.referenceType,
            referenceId = notification.referenceId,
            isRead = notification.isRead,
            readAt = notification.readAt,
            createdAt = notification.createdAt
        )
}
