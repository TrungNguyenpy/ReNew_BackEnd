package com.usedmarket.user.entity

import com.usedmarket.common.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table

@Entity
@Table(name = "addresses")
class Address(

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User,

    @Column(name = "recipient_name", nullable = false, length = 150)
    var recipientName: String,

    @Column(name = "recipient_phone", nullable = false, length = 20)
    var recipientPhone: String,

    /** Street address / house number line. */
    @Column(name = "address_line", nullable = false, length = 255)
    var addressLine: String,

    @Column(nullable = false, length = 100)
    var ward: String,

    @Column(nullable = false, length = 100)
    var district: String,

    @Column(nullable = false, length = 100)
    var province: String,

    @Column(nullable = false)
    var isDefault: Boolean = false,

    @Column(length = 255)
    var note: String? = null

) : BaseEntity()
