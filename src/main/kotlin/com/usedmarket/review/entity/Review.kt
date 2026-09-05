package com.usedmarket.review.entity

import com.usedmarket.common.entity.BaseEntity
import com.usedmarket.order.entity.Order
import com.usedmarket.product.entity.Product
import com.usedmarket.user.entity.User
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table

/**
 * A customer review, always tied to the Order it was purchased in
 * (spec section 3: "After successful purchase") — this is what lets the
 * service layer verify the reviewer actually bought the item before
 * allowing the review to be created.
 */
@Entity
@Table(name = "reviews")
class Review(

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    var product: Product,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    var customer: User,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    var order: Order,

    /** Overall star rating, 1-5. */
    @Column(nullable = false)
    var rating: Int,

    @Column(columnDefinition = "TEXT")
    var comment: String? = null,

    /** Optional 1-5 sub-ratings (spec section 3: review condition/delivery/packaging separately). */
    @Column(name = "product_condition_rating")
    var productConditionRating: Int? = null,

    @Column(name = "delivery_rating")
    var deliveryRating: Int? = null,

    @Column(name = "packaging_rating")
    var packagingRating: Int? = null,

    /** Staff/admin reply shown publicly under the review. */
    @Column(name = "seller_reply", columnDefinition = "TEXT")
    var sellerReply: String? = null

) : BaseEntity() {

    @OneToMany(mappedBy = "review", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
    var images: MutableList<ReviewImage> = mutableListOf()
}
