package com.usedmarket.product.controller

import com.usedmarket.product.dto.InspectionCreateRequest
import com.usedmarket.product.dto.InspectionPublishRequest
import com.usedmarket.product.dto.InspectionResponse
import com.usedmarket.product.dto.PublicInspectionResponse
import com.usedmarket.product.service.InspectionService
import com.usedmarket.security.CustomUserDetails
import jakarta.validation.Valid
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
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/products/{productId}/inspection")
class InspectionController(
    private val inspectionService: InspectionService
) {

    /** Public product-page report (spec section 5: "Customer can view a public version"). */
    @GetMapping
    fun getPublicReport(@PathVariable productId: UUID): PublicInspectionResponse =
        inspectionService.getPublicReport(productId)

    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    fun getAllForProduct(@PathVariable productId: UUID): List<InspectionResponse> =
        inspectionService.getAllForProduct(productId)

    @PostMapping
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    fun create(
        @PathVariable productId: UUID,
        @Valid @RequestBody request: InspectionCreateRequest,
        @AuthenticationPrincipal principal: CustomUserDetails
    ): ResponseEntity<InspectionResponse> =
        ResponseEntity.status(HttpStatus.CREATED)
            .body(inspectionService.create(productId, request, principal.user))

    @PatchMapping("/{inspectionId}/publish")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    fun setPublic(
        @PathVariable productId: UUID,
        @PathVariable inspectionId: UUID,
        @RequestBody request: InspectionPublishRequest
    ): InspectionResponse = inspectionService.setPublic(productId, inspectionId, request.isPublic)
}
