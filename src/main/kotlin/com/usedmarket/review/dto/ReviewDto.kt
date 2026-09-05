package com.usedmarket.review.dto

import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.Instant
import java.util.UUID

data class ReviewCreateRequest(
    @field:NotNull(message = "Order is required")
    val orderId: UUID,

    @field:Min(value = 1, message = "Rating must be between 1 and 5")
    @field:Max(value = 5, message = "Rating must be between 1 and 5")
    val rating: Int,

    val comment: String? = null,

    @field:Min(value = 1, message = "Rating must be between 1 and 5")
    @field:Max(value = 5, message = "Rating must be between 1 and 5")
    val productConditionRating: Int? = null,

    @field:Min(value = 1, message = "Rating must be between 1 and 5")
    @field:Max(value = 5, message = "Rating must be between 1 and 5")
    val deliveryRating: Int? = null,

    @field:Min(value = 1, message = "Rating must be between 1 and 5")
    @field:Max(value = 5, message = "Rating must be between 1 and 5")
    val packagingRating: Int? = null
)

data class ReviewReplyRequest(
    @field:NotBlank(message = "Reply is required")
    val sellerReply: String
)

data class ReviewImageResponse(
    val id: UUID,
    val imageUrl: String
)

data class ReviewResponse(
    val id: UUID,
    val productId: UUID,
    val customerId: UUID,
    val customerName: String,
    val orderId: UUID,
    val rating: Int,
    val comment: String?,
    val productConditionRating: Int?,
    val deliveryRating: Int?,
    val packagingRating: Int?,
    val sellerReply: String?,
    val images: List<ReviewImageResponse>,
    val createdAt: Instant?
)

data class ReviewSummaryResponse(
    val productId: UUID,
    val averageRating: Double?,
    val totalReviews: Long
)
