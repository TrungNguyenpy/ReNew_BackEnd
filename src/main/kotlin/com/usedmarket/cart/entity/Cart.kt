package com.usedmarket.cart.entity

import com.usedmarket.common.entity.BaseEntity
import com.usedmarket.user.entity.User
import jakarta.persistence.CascadeType
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToMany
import jakarta.persistence.OneToOne
import jakarta.persistence.Table

@Entity
@Table(name = "carts")
class Cart(

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    var user: User

) : BaseEntity() {

    @OneToMany(mappedBy = "cart", fetch = FetchType.LAZY, cascade = [CascadeType.ALL], orphanRemoval = true)
    var items: MutableList<CartItem> = mutableListOf()
}
