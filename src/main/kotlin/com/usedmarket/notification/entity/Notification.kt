package com.usedmarket.notification.entity

import com.usedmarket.common.entity.BaseEntity
import com.usedmarket.user.entity.User
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "notifications")
class Notification(

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    var type: NotificationType,

    @Column(nullable = false, length = 255)
    var title: String,

    @Column(nullable = false, columnDefinition = "TEXT")
    var message: String,

    /** Loosely points at the entity this notification is about (an Order, a Product...). */
    @Column(name = "reference_type", length = 50)
    var referenceType: String? = null,

    @Column(name = "reference_id")
    var referenceId: UUID? = null,

    @Column(name = "is_read", nullable = false)
    var isRead: Boolean = false,

    @Column(name = "read_at")
    var readAt: Instant? = null

) : BaseEntity()
