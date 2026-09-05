package com.usedmarket.review.mapper

import com.usedmarket.review.dto.ReviewImageResponse
import com.usedmarket.review.dto.ReviewResponse
import com.usedmarket.review.entity.Review
import com.usedmarket.review.entity.ReviewImage
import org.springframework.stereotype.Component

@Component
class ReviewMapper {

    fun toImageResponse(image: ReviewImage): ReviewImageResponse =
        ReviewImageResponse(id = image.id!!, imageUrl = image.imageUrl)

    fun toResponse(review: Review, images: List<ReviewImage>): ReviewResponse =
        ReviewResponse(
            id = review.id!!,
            productId = review.product.id!!,
            customerId = review.customer.id!!,
            customerName = review.customer.fullName,
            orderId = review.order.id!!,
            rating = review.rating,
            comment = review.comment,
            productConditionRating = review.productConditionRating,
            deliveryRating = review.deliveryRating,
            packagingRating = review.packagingRating,
            sellerReply = review.sellerReply,
            images = images.map(::toImageResponse),
            createdAt = review.createdAt
        )
}
