package com.usedmarket.tradein.entity

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
@Table(name = "trade_in_items")
class TradeInItem(

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "trade_in_request_id", nullable = false)
    var tradeInRequest: TradeInRequest,

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false, length = 10)
    var mediaType: MediaType,

    @Column(name = "media_url", nullable = false, length = 500)
    var mediaUrl: String,

    @Column(name = "cloudinary_public_id", length = 255)
    var cloudinaryPublicId: String? = null,

    @Column(name = "display_order", nullable = false)
    var displayOrder: Int = 0

) : BaseEntity()
