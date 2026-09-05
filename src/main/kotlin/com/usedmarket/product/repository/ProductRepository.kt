package com.usedmarket.product.repository

import com.usedmarket.product.entity.ConditionGrade
import com.usedmarket.product.entity.Product
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.math.BigDecimal
import java.util.Optional
import java.util.UUID

interface ProductRepository : JpaRepository<Product, UUID> {

    fun findBySlug(slug: String): Optional<Product>

    fun existsBySlug(slug: String): Boolean

    fun existsByCategoryId(categoryId: UUID): Boolean

    fun existsByBrandId(brandId: UUID): Boolean

    /**
     * Combined search/filter query backing the product listing page (spec section 3:
     * search, filter by category/brand/price range/condition, only active & visible items).
     * Each filter parameter is optional — pass null to skip that condition.
     */
    @Query(
        """
        SELECT p FROM Product p
        WHERE p.isActive = true
          AND p.isHidden = false
          AND (:keyword IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :keyword, '%')))
          AND (:categoryId IS NULL OR p.category.id = :categoryId)
          AND (:brandId IS NULL OR p.brand.id = :brandId)
          AND (:minPrice IS NULL OR p.price >= :minPrice)
          AND (:maxPrice IS NULL OR p.price <= :maxPrice)
          AND (:condition IS NULL OR p.condition = :condition)
        """
    )
    fun search(
        @Param("keyword") keyword: String?,
        @Param("categoryId") categoryId: UUID?,
        @Param("brandId") brandId: UUID?,
        @Param("minPrice") minPrice: BigDecimal?,
        @Param("maxPrice") maxPrice: BigDecimal?,
        @Param("condition") condition: ConditionGrade?,
        pageable: Pageable
    ): Page<Product>
}
