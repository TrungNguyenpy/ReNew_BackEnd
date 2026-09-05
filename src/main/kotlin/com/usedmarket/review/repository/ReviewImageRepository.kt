package com.usedmarket.review.repository

import com.usedmarket.review.entity.ReviewImage
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface ReviewImageRepository : JpaRepository<ReviewImage, UUID> {

    fun findByReviewId(reviewId: UUID): List<ReviewImage>
}
