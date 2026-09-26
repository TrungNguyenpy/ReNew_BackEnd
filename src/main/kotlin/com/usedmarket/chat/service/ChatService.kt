package com.usedmarket.chat.service

import com.usedmarket.chat.dto.ChatMessageCreateRequest
import com.usedmarket.chat.dto.ChatMessageResponse
import com.usedmarket.chat.dto.ChatRoomCreateRequest
import com.usedmarket.chat.dto.ChatRoomResponse
import com.usedmarket.chat.entity.ChatMessage
import com.usedmarket.chat.entity.ChatRoom
import com.usedmarket.chat.entity.ChatRoomStatus
import com.usedmarket.chat.mapper.ChatMapper
import com.usedmarket.chat.repository.ChatMessageRepository
import com.usedmarket.chat.repository.ChatRoomRepository
import com.usedmarket.common.exception.BadRequestException
import com.usedmarket.common.exception.ForbiddenException
import com.usedmarket.common.exception.ResourceNotFoundException
import com.usedmarket.product.repository.ProductRepository
import com.usedmarket.user.entity.RoleName
import com.usedmarket.user.entity.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class ChatService(
    private val chatRoomRepository: ChatRoomRepository,
    private val chatMessageRepository: ChatMessageRepository,
    private val productRepository: ProductRepository,
    private val chatMapper: ChatMapper
) {

    /** Reuses an existing OPEN room for the same customer + product combo instead of spawning duplicates. */
    @Transactional
    fun createOrGetRoom(customer: User, request: ChatRoomCreateRequest): ChatRoomResponse {
        val product = request.productId?.let {
            productRepository.findById(it)
                .orElseThrow { ResourceNotFoundException("Product not found with id: $it") }
        }

        val existing = chatRoomRepository.findByCustomerId(customer.id!!)
            .firstOrNull { it.status == ChatRoomStatus.OPEN && it.product?.id == request.productId }

        val room = existing ?: chatRoomRepository.save(
            ChatRoom(customer = customer, product = product, status = ChatRoomStatus.OPEN)
        )

        if (!request.initialMessage.isNullOrBlank()) {
            chatMessageRepository.save(ChatMessage(chatRoom = room, sender = customer, content = request.initialMessage))
        }

        return chatMapper.toRoomResponse(room)
    }

    fun getMyRooms(customerId: UUID): List<ChatRoomResponse> =
        chatRoomRepository.findByCustomerId(customerId).map(chatMapper::toRoomResponse)

    fun getForManagement(page: Int, size: Int): Page<ChatRoomResponse> {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        return chatRoomRepository.findAll(pageable).map(chatMapper::toRoomResponse)
    }

    @Transactional
    fun getMessages(roomId: UUID, requester: User): List<ChatMessageResponse> {
        val room = findGuarded(roomId, requester)
        val messages = chatMessageRepository.findByChatRoomIdOrderByCreatedAtAsc(room.id!!)

        val unreadFromOther = messages.filter { !it.isRead && it.sender.id != requester.id }
        if (unreadFromOther.isNotEmpty()) {
            unreadFromOther.forEach { it.isRead = true }
            chatMessageRepository.saveAll(unreadFromOther)
        }

        return messages.map(chatMapper::toMessageResponse)
    }

    @Transactional
    fun sendMessage(roomId: UUID, request: ChatMessageCreateRequest, sender: User): ChatMessageResponse {
        val room = chatRoomRepository.findById(roomId)
            .orElseThrow { ResourceNotFoundException("Chat room not found with id: $roomId") }

        if (room.status == ChatRoomStatus.CLOSED) {
            throw BadRequestException("This chat room is closed")
        }

        val isCustomerOwner = sender.id == room.customer.id
        val isStaffOrAdmin = sender.role == RoleName.STAFF || sender.role == RoleName.ADMIN

        if (!isCustomerOwner && !isStaffOrAdmin) {
            throw ForbiddenException("You do not have access to this chat room")
        }
        if (isStaffOrAdmin && room.staff != null && room.staff!!.id != sender.id) {
            throw ForbiddenException("This chat room is already being handled by another staff member")
        }

        if (isStaffOrAdmin && room.staff == null) {
            room.staff = sender
            chatRoomRepository.save(room)
        }

        val message = ChatMessage(chatRoom = room, sender = sender, content = request.content)
        chatMessageRepository.save(message)
        return chatMapper.toMessageResponse(message)
    }

    @Transactional
    fun claim(roomId: UUID, staffUser: User): ChatRoomResponse {
        val room = chatRoomRepository.findById(roomId)
            .orElseThrow { ResourceNotFoundException("Chat room not found with id: $roomId") }
        if (room.staff != null && room.staff!!.id != staffUser.id) {
            throw BadRequestException("This chat room is already claimed by another staff member")
        }
        room.staff = staffUser
        chatRoomRepository.save(room)
        return chatMapper.toRoomResponse(room)
    }

    @Transactional
    fun close(roomId: UUID, requester: User): ChatRoomResponse {
        val room = findGuarded(roomId, requester)
        room.status = ChatRoomStatus.CLOSED
        chatRoomRepository.save(room)
        return chatMapper.toRoomResponse(room)
    }

    private fun findGuarded(roomId: UUID, requester: User): ChatRoom {
        val room = chatRoomRepository.findById(roomId)
            .orElseThrow { ResourceNotFoundException("Chat room not found with id: $roomId") }
        val isOwner = room.customer.id == requester.id
        val isStaffOrAdmin = requester.role == RoleName.STAFF || requester.role == RoleName.ADMIN
        if (!isOwner && !isStaffOrAdmin) {
            throw ResourceNotFoundException("Chat room not found with id: $roomId")
        }
        return room
    }
}
