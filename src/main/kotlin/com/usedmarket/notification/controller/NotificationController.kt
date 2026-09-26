package com.usedmarket.notification.controller

import com.usedmarket.notification.dto.NotificationResponse
import com.usedmarket.notification.service.NotificationService
import com.usedmarket.security.CustomUserDetails
import org.springframework.data.domain.Page
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/notifications")
class NotificationController(
    private val notificationService: NotificationService
) {

    @GetMapping
    fun getMyNotifications(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @AuthenticationPrincipal principal: CustomUserDetails
    ): Page<NotificationResponse> = notificationService.getMyNotifications(principal.user.id!!, page, size)

    @GetMapping("/unread-count")
    fun getUnreadCount(@AuthenticationPrincipal principal: CustomUserDetails): Map<String, Long> =
        mapOf("unreadCount" to notificationService.getUnreadCount(principal.user.id!!))

    @PatchMapping("/{id}/read")
    fun markAsRead(
        @PathVariable id: UUID,
        @AuthenticationPrincipal principal: CustomUserDetails
    ): NotificationResponse = notificationService.markAsRead(id, principal.user)

    @PatchMapping("/read-all")
    fun markAllAsRead(@AuthenticationPrincipal principal: CustomUserDetails): ResponseEntity<Void> {
        notificationService.markAllAsRead(principal.user.id!!)
        return ResponseEntity.noContent().build()
    }
}
