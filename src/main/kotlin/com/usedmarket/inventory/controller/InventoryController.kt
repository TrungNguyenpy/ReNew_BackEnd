package com.usedmarket.inventory.controller

import com.usedmarket.inventory.dto.InventoryAdjustRequest
import com.usedmarket.inventory.dto.InventoryHistoryResponse
import com.usedmarket.inventory.dto.InventoryResponse
import com.usedmarket.inventory.service.InventoryService
import com.usedmarket.security.CustomUserDetails
import jakarta.validation.Valid
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

/**
 * Inventory numbers are internal operational data (spec section 7) — every
 * endpoint here is STAFF/ADMIN only, unlike Product/Category/Brand which
 * expose public read access.
 */
@RestController
@RequestMapping("/api/products/{productId}/inventory")
@PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
class InventoryController(
    private val inventoryService: InventoryService
) {

    @GetMapping
    fun get(@PathVariable productId: UUID): InventoryResponse =
        inventoryService.getByProductId(productId)

    @GetMapping("/history")
    fun getHistory(@PathVariable productId: UUID): List<InventoryHistoryResponse> =
        inventoryService.getHistory(productId)

    @PostMapping("/adjust")
    fun adjust(
        @PathVariable productId: UUID,
        @Valid @RequestBody request: InventoryAdjustRequest,
        @AuthenticationPrincipal principal: CustomUserDetails
    ): InventoryResponse = inventoryService.adjust(productId, request, principal.user)
}
