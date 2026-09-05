package com.usedmarket.product.controller

import com.usedmarket.product.dto.ConditionScoreResponse
import com.usedmarket.product.dto.ConditionScoreUpdateRequest
import com.usedmarket.product.service.ConditionScoreService
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
@RequestMapping("/api/products/{productId}/condition-score")
class ConditionScoreController(
    private val conditionScoreService: ConditionScoreService
) {

    @GetMapping
    fun get(@PathVariable productId: UUID): ConditionScoreResponse =
        conditionScoreService.getByProductId(productId)

    @PutMapping
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    fun update(
        @PathVariable productId: UUID,
        @Valid @RequestBody request: ConditionScoreUpdateRequest
    ): ConditionScoreResponse = conditionScoreService.update(productId, request)
}
