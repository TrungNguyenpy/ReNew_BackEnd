package com.usedmarket.product.dto

import com.usedmarket.product.entity.ConditionGrade
import jakarta.validation.Valid
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Size
import java.math.BigDecimal
import java.util.UUID

data class ProductUpdateRequest(
    @field:NotBlank(message = "Name is required")
    @field:Size(max = 255, message = "Name must be at most 255 characters")
    val name: String,

    @field:NotBlank(message = "Slug is required")
    @field:Size(max = 255, message = "Slug must be at most 255 characters")
    val slug: String,

    val description: String? = null,

    @field:NotNull(message = "Category is required")
    val categoryId: UUID,

    @field:NotNull(message = "Brand is required")
    val brandId: UUID,

    @field:Size(max = 150, message = "Model must be at most 150 characters")
    val model: String? = null,

    @field:NotNull(message = "Price is required")
    @field:DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than 0")
    val price: BigDecimal,

    @field:DecimalMin(value = "0.0", inclusive = true, message = "Original price must not be negative")
    val originalPrice: BigDecimal? = null,

    @field:Min(value = 0, message = "Stock quantity must not be negative")
    val stockQuantity: Int = 0,

    @field:NotNull(message = "Condition is required")
    val condition: ConditionGrade,

    val manufactureYear: Int? = null,
    val purchaseYear: Int? = null,
    val usageDuration: String? = null,
    val cosmeticCondition: String? = null,
    val functionalCondition: String? = null,
    val knownDefects: String? = null,
    val repairHistory: String? = null,
    val accessoriesIncluded: String? = null,

    val isActive: Boolean = true,
    val isHidden: Boolean = false,

    @field:Valid
    val specifications: List<ProductSpecificationRequest> = emptyList()
)
