package com.usedmarket.chat.entity

import com.usedmarket.common.entity.BaseEntity
import com.usedmarket.user.entity.User
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "chat_messages")
class ChatMessage(

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chat_room_id", nullable = false)
    var chatRoom: ChatRoom,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sender_id", nullable = false)
    var sender: User,

    @Column(nullable = false, columnDefinition = "TEXT")
    var content: String,

    /** Cloudinary URL if the customer/staff attached an image to the message. */
    @Column(name = "attachment_url", length = 500)
    var attachmentUrl: String? = null,

    @Column(name = "is_read", nullable = false)
    var isRead: Boolean = false

) : BaseEntity()
