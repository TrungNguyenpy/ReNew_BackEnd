package com.usedmarket.order.controller

import com.usedmarket.order.dto.CheckoutRequest
import com.usedmarket.order.dto.OrderResponse
import com.usedmarket.order.dto.OrderStatusHistoryResponse
import com.usedmarket.order.dto.OrderStatusUpdateRequest
import com.usedmarket.order.dto.OrderSummaryResponse
import com.usedmarket.order.entity.OrderStatus
import com.usedmarket.order.service.OrderService
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
@RequestMapping("/api/orders")
class OrderController(
    private val orderService: OrderService
) {

    @PostMapping
    fun checkout(
        @Valid @RequestBody request: CheckoutRequest,
        @AuthenticationPrincipal principal: CustomUserDetails
    ): ResponseEntity<OrderResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(orderService.checkout(principal.user.id!!, request))

    @GetMapping
    fun getMyOrders(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @AuthenticationPrincipal principal: CustomUserDetails
    ): Page<OrderSummaryResponse> = orderService.getMyOrders(principal.user.id!!, page, size)

    @GetMapping("/manage")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    fun getForManagement(
        @RequestParam(required = false) status: OrderStatus?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): Page<OrderSummaryResponse> = orderService.getForManagement(status, page, size)

    @GetMapping("/{id}")
    fun getById(@PathVariable id: UUID, @AuthenticationPrincipal principal: CustomUserDetails): OrderResponse =
        orderService.getById(id, principal.user)

    @GetMapping("/{id}/timeline")
    fun getTimeline(
        @PathVariable id: UUID,
        @AuthenticationPrincipal principal: CustomUserDetails
    ): List<OrderStatusHistoryResponse> = orderService.getTimeline(id, principal.user)

    @PostMapping("/{id}/cancel")
    fun cancel(@PathVariable id: UUID, @AuthenticationPrincipal principal: CustomUserDetails): OrderResponse =
        orderService.cancelMyOrder(id, principal.user)

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    fun updateStatus(
        @PathVariable id: UUID,
        @Valid @RequestBody request: OrderStatusUpdateRequest,
        @AuthenticationPrincipal principal: CustomUserDetails
    ): OrderResponse = orderService.updateStatus(id, request, principal.user)
}
