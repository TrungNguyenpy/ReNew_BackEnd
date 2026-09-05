package com.usedmarket.review.service

import com.usedmarket.common.exception.BadRequestException
import com.usedmarket.common.exception.DuplicateResourceException
import com.usedmarket.common.exception.ForbiddenException
import com.usedmarket.common.exception.ResourceNotFoundException
import com.usedmarket.media.CloudinaryService
import com.usedmarket.order.entity.OrderStatus
import com.usedmarket.order.repository.OrderItemRepository
import com.usedmarket.order.repository.OrderRepository
import com.usedmarket.product.repository.ProductRepository
import com.usedmarket.review.dto.ReviewCreateRequest
import com.usedmarket.review.dto.ReviewReplyRequest
import com.usedmarket.review.dto.ReviewResponse
import com.usedmarket.review.dto.ReviewSummaryResponse
import com.usedmarket.review.entity.Review
import com.usedmarket.review.entity.ReviewImage
import com.usedmarket.review.mapper.ReviewMapper
import com.usedmarket.review.repository.ReviewImageRepository
import com.usedmarket.review.repository.ReviewRepository
import com.usedmarket.user.entity.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

@Service
class ReviewService(
    private val reviewRepository: ReviewRepository,
    private val reviewImageRepository: ReviewImageRepository,
    private val orderRepository: OrderRepository,
    private val orderItemRepository: OrderItemRepository,
    private val productRepository: ProductRepository,
    private val cloudinaryService: CloudinaryService,
    private val reviewMapper: ReviewMapper
) {

    fun getByProduct(productId: UUID, page: Int, size: Int): Page<ReviewResponse> {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        return reviewRepository.findByProductId(productId, pageable).map { review ->
            reviewMapper.toResponse(review, reviewImageRepository.findByReviewId(review.id!!))
        }
    }

    fun getSummary(productId: UUID): ReviewSummaryResponse {
        val allReviews = reviewRepository.findByProductId(productId, Pageable.unpaged()).content
        val average = if (allReviews.isNotEmpty()) allReviews.map { it.rating }.average() else null
        return ReviewSummaryResponse(productId = productId, averageRating = average, totalReviews = allReviews.size.toLong())
    }

    @Transactional
    fun create(productId: UUID, request: ReviewCreateRequest, customer: User): ReviewResponse {
        val product = productRepository.findById(productId)
            .orElseThrow { ResourceNotFoundException("Product not found with id: $productId") }

        val order = orderRepository.findById(request.orderId)
            .orElseThrow { ResourceNotFoundException("Order not found with id: ${request.orderId}") }

        if (order.customer.id != customer.id) {
            throw ForbiddenException("This order does not belong to you")
        }
        if (order.status != OrderStatus.DELIVERED) {
            throw BadRequestException("You can only review a product after it has been delivered")
        }

        val orderItems = orderItemRepository.findByOrderId(order.id!!)
        val purchasedThisProduct = orderItems.any { it.product?.id == productId }
        if (!purchasedThisProduct) {
            throw BadRequestException("This order does not contain the product you are trying to review")
        }

        if (reviewRepository.existsByOrderIdAndProductId(order.id!!, productId)) {
            throw DuplicateResourceException("You have already reviewed this product for this order")
        }

        val review = Review(
            product = product,
            customer = customer,
            order = order,
            rating = request.rating,
            comment = request.comment,
            productConditionRating = request.productConditionRating,
            deliveryRating = request.deliveryRating,
            packagingRating = request.packagingRating
        )
        reviewRepository.save(review)

        return reviewMapper.toResponse(review, emptyList())
    }

    @Transactional
    fun addImage(reviewId: UUID, file: MultipartFile, requester: User): ReviewResponse {
        val review = reviewRepository.findById(reviewId)
            .orElseThrow { ResourceNotFoundException("Review not found with id: $reviewId") }
        if (review.customer.id != requester.id) {
            throw ForbiddenException("You can only add images to your own review")
        }

        val uploadResult = cloudinaryService.upload(file, "reviews/$reviewId")
        reviewImageRepository.save(
            ReviewImage(review = review, imageUrl = uploadResult.url, cloudinaryPublicId = uploadResult.publicId)
        )

        return reviewMapper.toResponse(review, reviewImageRepository.findByReviewId(reviewId))
    }

    /** STAFF/ADMIN — publicly visible reply shown under the review. */
    @Transactional
    fun reply(reviewId: UUID, request: ReviewReplyRequest): ReviewResponse {
        val review = reviewRepository.findById(reviewId)
            .orElseThrow { ResourceNotFoundException("Review not found with id: $reviewId") }
        review.sellerReply = request.sellerReply
        reviewRepository.save(review)
        return reviewMapper.toResponse(review, reviewImageRepository.findByReviewId(reviewId))
    }
}
