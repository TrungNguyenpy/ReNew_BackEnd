package com.usedmarket.cart.entity

import com.usedmarket.common.entity.BaseEntity
import com.usedmarket.product.entity.Product
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "cart_items")
class CartItem(

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cart_id", nullable = false)
    var cart: Cart,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    var product: Product,

    /**
     * Most used items are unique (Product.stockQuantity = 1), so quantity
     * will typically be 1. Kept as a field to also support stackable used
     * items (e.g. several identical used earbuds) without a schema change.
     */
    @Column(nullable = false)
    var quantity: Int = 1

) : BaseEntity()
