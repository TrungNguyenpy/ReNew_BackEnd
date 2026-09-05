package com.usedmarket.wishlist.entity

import com.usedmarket.common.entity.BaseEntity
import com.usedmarket.product.entity.Product
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.math.BigDecimal

@Entity
@Table(name = "wishlist_items")
class WishlistItem(

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "wishlist_id", nullable = false)
    var wishlist: Wishlist,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    var product: Product,

    /**
     * Product price at the moment it was added to the wishlist.
     * The notification job (spec section 13: "Wishlist product price decreases")
     * compares this against the current Product.price to detect drops.
     */
    @Column(name = "price_at_add_time", nullable = false, precision = 12, scale = 2)
    var priceAtAddTime: BigDecimal

) : BaseEntity()
