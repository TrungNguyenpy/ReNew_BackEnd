package com.usedmarket.catalog.entity

import com.usedmarket.common.entity.BaseEntity
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.OneToMany
import jakarta.persistence.Table

@Entity
@Table(name = "categories")
class Category(

    @Column(nullable = false, length = 150)
    var name: String,

    /** URL-friendly unique identifier, e.g. "smartphone", used in product listing routes. */
    @Column(nullable = false, unique = true, length = 150)
    var slug: String,

    @Column(length = 1000)
    var description: String? = null,

    @Column(name = "image_url")
    var imageUrl: String? = null,

    /** Root categories (e.g. "Electronics", "Home Appliances") have parent = null. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    var parent: Category? = null,

    @Column(nullable = false)
    var isActive: Boolean = true,

    @Column(name = "display_order", nullable = false)
    var displayOrder: Int = 0

) : BaseEntity() {

    @OneToMany(
        mappedBy = "parent",
        fetch = FetchType.LAZY,
        cascade = [CascadeType.PERSIST, CascadeType.MERGE]
    )
    var children: MutableList<Category> = mutableListOf()
}
