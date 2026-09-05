package com.usedmarket.catalog.entity

import com.usedmarket.common.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table

@Entity
@Table(name = "brands")
class Brand(

    @Column(nullable = false, unique = true, length = 150)
    var name: String,

    @Column(nullable = false, unique = true, length = 150)
    var slug: String,

    @Column(name = "logo_url")
    var logoUrl: String? = null,

    @Column(length = 1000)
    var description: String? = null,

    @Column(nullable = false)
    var isActive: Boolean = true

) : BaseEntity()
