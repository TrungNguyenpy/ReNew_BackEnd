package com.usedmarket.review.entity

import com.usedmarket.common.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "review_images")
class ReviewImage(

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "review_id", nullable = false)
    var review: Review,

    @Column(name = "image_url", nullable = false, length = 500)
    var imageUrl: String,

    @Column(name = "cloudinary_public_id", length = 255)
    var cloudinaryPublicId: String? = null

) : BaseEntity()
