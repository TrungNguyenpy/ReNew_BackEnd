package com.usedmarket.product.controller

import com.usedmarket.product.dto.WarrantyRequest
import com.usedmarket.product.dto.WarrantyResponse
import com.usedmarket.product.service.WarrantyService
import jakarta.validation.Valid
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/products/{productId}/warranty")
class WarrantyController(
    private val warrantyService: WarrantyService
) {

    @GetMapping
    fun get(@PathVariable productId: UUID): WarrantyResponse =
        warrantyService.getByProductId(productId)

    @PutMapping
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    fun upsert(
        @PathVariable productId: UUID,
        @Valid @RequestBody request: WarrantyRequest
    ): WarrantyResponse = warrantyService.upsert(productId, request)
}
