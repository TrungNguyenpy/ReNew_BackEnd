package com.usedmarket.chat.repository

import com.usedmarket.chat.entity.ChatRoom
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ChatRoomRepository : JpaRepository<ChatRoom, UUID> {

    fun findByCustomerId(customerId: UUID): List<ChatRoom>

    fun findByStaffId(staffId: UUID): List<ChatRoom>
}
