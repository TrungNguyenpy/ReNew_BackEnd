package com.usedmarket.product.entity

import com.usedmarket.common.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "product_images")
class ProductImage(

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    var product: Product,

    /** Cloudinary secure_url. */
    @Column(name = "image_url", nullable = false, length = 500)
    var imageUrl: String,

    /** Cloudinary public_id, needed to delete/replace the asset later. */
    @Column(name = "cloudinary_public_id", length = 255)
    var cloudinaryPublicId: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(name = "image_type", nullable = false, length = 20)
    var imageType: ImageType,

    @Column(name = "display_order", nullable = false)
    var displayOrder: Int = 0,

    @Column(name = "is_primary", nullable = false)
    var isPrimary: Boolean = false

) : BaseEntity()
