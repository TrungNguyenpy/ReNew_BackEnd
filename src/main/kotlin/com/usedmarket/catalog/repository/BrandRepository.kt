package com.usedmarket.catalog.repository

import com.usedmarket.catalog.entity.Brand
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional
import java.util.UUID

interface BrandRepository : JpaRepository<Brand, UUID> {

    fun findBySlug(slug: String): Optional<Brand>

    fun existsBySlug(slug: String): Boolean
}
