package com.usedmarket.chat.mapper

import com.usedmarket.chat.dto.ChatMessageResponse
import com.usedmarket.chat.dto.ChatRoomResponse
import com.usedmarket.chat.entity.ChatMessage
import com.usedmarket.chat.entity.ChatRoom
import org.springframework.stereotype.Component

@Component
class ChatMapper {

    fun toMessageResponse(message: ChatMessage): ChatMessageResponse =
        ChatMessageResponse(
            id = message.id!!,
            senderId = message.sender.id!!,
            senderName = message.sender.fullName,
            senderRole = message.sender.role.name,
            content = message.content,
            isRead = message.isRead,
            createdAt = message.createdAt
        )

    fun toRoomResponse(room: ChatRoom): ChatRoomResponse =
        ChatRoomResponse(
            id = room.id!!,
            customerId = room.customer.id!!,
            customerName = room.customer.fullName,
            staffId = room.staff?.id,
            staffName = room.staff?.fullName,
            productId = room.product?.id,
            productName = room.product?.name,
            status = room.status,
            createdAt = room.createdAt
        )
}
