package com.usedmarket.chat.repository

import com.usedmarket.chat.entity.ChatMessage
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ChatMessageRepository : JpaRepository<ChatMessage, UUID> {

    fun findByChatRoomIdOrderByCreatedAtAsc(chatRoomId: UUID): List<ChatMessage>

    fun countByChatRoomIdAndIsReadFalse(chatRoomId: UUID): Long
}
