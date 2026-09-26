package com.usedmarket.chat.controller

import com.usedmarket.chat.dto.ChatMessageCreateRequest
import com.usedmarket.chat.dto.ChatMessageResponse
import com.usedmarket.chat.dto.ChatRoomCreateRequest
import com.usedmarket.chat.dto.ChatRoomResponse
import com.usedmarket.chat.service.ChatService
import com.usedmarket.security.CustomUserDetails
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/chat/rooms")
class ChatController(
    private val chatService: ChatService
) {

    @PostMapping
    fun createRoom(
        @Valid @RequestBody request: ChatRoomCreateRequest,
        @AuthenticationPrincipal principal: CustomUserDetails
    ): ResponseEntity<ChatRoomResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(chatService.createOrGetRoom(principal.user, request))

    @GetMapping
    fun getMyRooms(@AuthenticationPrincipal principal: CustomUserDetails): List<ChatRoomResponse> =
        chatService.getMyRooms(principal.user.id!!)

    @GetMapping("/manage")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    fun getForManagement(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): Page<ChatRoomResponse> = chatService.getForManagement(page, size)

    @GetMapping("/{id}/messages")
    fun getMessages(
        @PathVariable id: UUID,
        @AuthenticationPrincipal principal: CustomUserDetails
    ): List<ChatMessageResponse> = chatService.getMessages(id, principal.user)

    @PostMapping("/{id}/messages")
    fun sendMessage(
        @PathVariable id: UUID,
        @Valid @RequestBody request: ChatMessageCreateRequest,
        @AuthenticationPrincipal principal: CustomUserDetails
    ): ResponseEntity<ChatMessageResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(chatService.sendMessage(id, request, principal.user))

    @PostMapping("/{id}/claim")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    fun claim(@PathVariable id: UUID, @AuthenticationPrincipal principal: CustomUserDetails): ChatRoomResponse =
        chatService.claim(id, principal.user)

    @PatchMapping("/{id}/close")
    fun close(@PathVariable id: UUID, @AuthenticationPrincipal principal: CustomUserDetails): ChatRoomResponse =
        chatService.close(id, principal.user)
}
