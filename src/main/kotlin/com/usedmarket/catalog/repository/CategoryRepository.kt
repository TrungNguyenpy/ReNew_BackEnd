package com.usedmarket.catalog.repository

import com.usedmarket.catalog.entity.Category
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional
import java.util.UUID

interface CategoryRepository : JpaRepository<Category, UUID> {

    fun findBySlug(slug: String): Optional<Category>

    fun existsBySlug(slug: String): Boolean

    fun findByParentIsNull(): List<Category>

    fun findByParentId(parentId: UUID): List<Category>
}
