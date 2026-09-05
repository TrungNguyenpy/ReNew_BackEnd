package com.usedmarket.review.controller

import com.usedmarket.review.dto.ReviewCreateRequest
import com.usedmarket.review.dto.ReviewReplyRequest
import com.usedmarket.review.dto.ReviewResponse
import com.usedmarket.review.dto.ReviewSummaryResponse
import com.usedmarket.review.service.ReviewService
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
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

@RestController
@RequestMapping("/api/products/{productId}/reviews")
class ReviewController(
    private val reviewService: ReviewService
) {

    @GetMapping
    fun getByProduct(
        @PathVariable productId: UUID,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): Page<ReviewResponse> = reviewService.getByProduct(productId, page, size)

    @GetMapping("/summary")
    fun getSummary(@PathVariable productId: UUID): ReviewSummaryResponse = reviewService.getSummary(productId)

    @PostMapping
    fun create(
        @PathVariable productId: UUID,
        @Valid @RequestBody request: ReviewCreateRequest,
        @AuthenticationPrincipal principal: CustomUserDetails
    ): ResponseEntity<ReviewResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(reviewService.create(productId, request, principal.user))

    @PostMapping("/{reviewId}/images", consumes = ["multipart/form-data"])
    fun addImage(
        @PathVariable productId: UUID,
        @PathVariable reviewId: UUID,
        @RequestPart file: MultipartFile,
        @AuthenticationPrincipal principal: CustomUserDetails
    ): ReviewResponse = reviewService.addImage(reviewId, file, principal.user)

    @PatchMapping("/{reviewId}/reply")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    fun reply(
        @PathVariable productId: UUID,
        @PathVariable reviewId: UUID,
        @Valid @RequestBody request: ReviewReplyRequest
    ): ReviewResponse = reviewService.reply(reviewId, request)
}
