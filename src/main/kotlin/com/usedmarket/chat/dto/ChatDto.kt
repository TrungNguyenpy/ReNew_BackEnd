package com.usedmarket.chat.dto

import com.usedmarket.chat.entity.ChatRoomStatus
import jakarta.validation.constraints.NotBlank
import java.time.Instant
import java.util.UUID

data class ChatRoomCreateRequest(
    val productId: UUID? = null,
    val initialMessage: String? = null
)

data class ChatMessageCreateRequest(
    @field:NotBlank(message = "Message content is required")
    val content: String
)

data class ChatMessageResponse(
    val id: UUID,
    val senderId: UUID,
    val senderName: String,
    val senderRole: String,
    val content: String,
    val isRead: Boolean,
    val createdAt: Instant?
)

data class ChatRoomResponse(
    val id: UUID,
    val customerId: UUID,
    val customerName: String,
    val staffId: UUID?,
    val staffName: String?,
    val productId: UUID?,
    val productName: String?,
    val status: ChatRoomStatus,
    val createdAt: Instant?
)
