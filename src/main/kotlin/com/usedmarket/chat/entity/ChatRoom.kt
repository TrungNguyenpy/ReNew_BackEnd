package com.usedmarket.chat.entity

import com.usedmarket.common.entity.BaseEntity
import com.usedmarket.product.entity.Product
import com.usedmarket.user.entity.User
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "chat_rooms")
class ChatRoom(

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    var customer: User,

    /** Staff member currently handling this conversation; null until claimed. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_id")
    var staff: User? = null,

    /** Optional: the product this conversation is about (spec section 3: ask about availability/condition/warranty...). */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    var product: Product? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var status: ChatRoomStatus = ChatRoomStatus.OPEN

) : BaseEntity()
