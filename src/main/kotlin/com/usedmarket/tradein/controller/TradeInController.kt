package com.usedmarket.tradein.controller

import com.usedmarket.security.CustomUserDetails
import com.usedmarket.tradein.dto.TradeInCreateRequest
import com.usedmarket.tradein.dto.TradeInOfferRequest
import com.usedmarket.tradein.dto.TradeInResponse
import com.usedmarket.tradein.dto.TradeInStatusUpdateRequest
import com.usedmarket.tradein.entity.MediaType
import com.usedmarket.tradein.entity.TradeInStatus
import com.usedmarket.tradein.service.TradeInService
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
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

@RestController
@RequestMapping("/api/trade-ins")
class TradeInController(
    private val tradeInService: TradeInService
) {

    @PostMapping
    fun create(
        @Valid @RequestBody request: TradeInCreateRequest,
        @AuthenticationPrincipal principal: CustomUserDetails
    ): ResponseEntity<TradeInResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(tradeInService.create(request, principal.user))

    @GetMapping
    fun getMyRequests(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @AuthenticationPrincipal principal: CustomUserDetails
    ): Page<TradeInResponse> = tradeInService.getMyRequests(principal.user.id!!, page, size)

    @GetMapping("/manage")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    fun getForManagement(
        @RequestParam(required = false) status: TradeInStatus?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): Page<TradeInResponse> = tradeInService.getForManagement(status, page, size)

    @GetMapping("/{id}")
    fun getById(@PathVariable id: UUID, @AuthenticationPrincipal principal: CustomUserDetails): TradeInResponse =
        tradeInService.getById(id, principal.user)

    @PostMapping("/{id}/items", consumes = ["multipart/form-data"])
    fun addItem(
        @PathVariable id: UUID,
        @RequestPart file: MultipartFile,
        @RequestParam mediaType: MediaType,
        @AuthenticationPrincipal principal: CustomUserDetails
    ): TradeInResponse = tradeInService.addItem(id, file, mediaType, principal.user)

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    fun updateStatus(
        @PathVariable id: UUID,
        @Valid @RequestBody request: TradeInStatusUpdateRequest,
        @AuthenticationPrincipal principal: CustomUserDetails
    ): TradeInResponse = tradeInService.updateStatus(id, request, principal.user)

    @PostMapping("/{id}/offer")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    fun makeOffer(
        @PathVariable id: UUID,
        @Valid @RequestBody request: TradeInOfferRequest,
        @AuthenticationPrincipal principal: CustomUserDetails
    ): TradeInResponse = tradeInService.makeOffer(id, request, principal.user)

    @PostMapping("/{id}/accept")
    fun accept(@PathVariable id: UUID, @AuthenticationPrincipal principal: CustomUserDetails): TradeInResponse =
        tradeInService.accept(id, principal.user)

    @PostMapping("/{id}/decline")
    fun decline(@PathVariable id: UUID, @AuthenticationPrincipal principal: CustomUserDetails): TradeInResponse =
        tradeInService.decline(id, principal.user)

    @PatchMapping("/{id}/complete")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    fun complete(@PathVariable id: UUID, @AuthenticationPrincipal principal: CustomUserDetails): TradeInResponse =
        tradeInService.complete(id, principal.user)
}
