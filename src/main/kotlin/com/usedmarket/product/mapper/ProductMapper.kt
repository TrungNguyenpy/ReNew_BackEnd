package com.usedmarket.product.mapper

import com.usedmarket.product.dto.ProductImageResponse
import com.usedmarket.product.dto.ProductResponse
import com.usedmarket.product.dto.ProductSpecificationResponse
import com.usedmarket.product.dto.ProductSummaryResponse
import com.usedmarket.product.entity.Product
import com.usedmarket.product.entity.ProductImage
import com.usedmarket.product.entity.ProductSpecification
import org.springframework.stereotype.Component

@Component
class ProductMapper {

    fun toImageResponse(image: ProductImage): ProductImageResponse =
        ProductImageResponse(
            id = image.id!!,
            imageUrl = image.imageUrl,
            imageType = image.imageType,
            displayOrder = image.displayOrder,
            isPrimary = image.isPrimary
        )

    fun toSpecificationResponse(spec: ProductSpecification): ProductSpecificationResponse =
        ProductSpecificationResponse(
            id = spec.id!!,
            specKey = spec.specKey,
            specValue = spec.specValue,
            displayOrder = spec.displayOrder
        )

    fun toSummaryResponse(product: Product, primaryImageUrl: String?): ProductSummaryResponse =
        ProductSummaryResponse(
            id = product.id!!,
            name = product.name,
            slug = product.slug,
            price = product.price,
            originalPrice = product.originalPrice,
            condition = product.condition,
            conditionScore = product.conditionScore,
            primaryImageUrl = primaryImageUrl,
            categoryName = product.category.name,
            brandName = product.brand.name,
            isActive = product.isActive,
            isHidden = product.isHidden
        )

    fun toResponse(
        product: Product,
        images: List<ProductImage>,
        specifications: List<ProductSpecification>
    ): ProductResponse =
        ProductResponse(
            id = product.id!!,
            name = product.name,
            slug = product.slug,
            description = product.description,
            categoryId = product.category.id!!,
            categoryName = product.category.name,
            brandId = product.brand.id!!,
            brandName = product.brand.name,
            model = product.model,
            price = product.price,
            originalPrice = product.originalPrice,
            stockQuantity = product.stockQuantity,
            condition = product.condition,
            conditionScore = product.conditionScore,
            manufactureYear = product.manufactureYear,
            purchaseYear = product.purchaseYear,
            usageDuration = product.usageDuration,
            cosmeticCondition = product.cosmeticCondition,
            functionalCondition = product.functionalCondition,
            knownDefects = product.knownDefects,
            repairHistory = product.repairHistory,
            accessoriesIncluded = product.accessoriesIncluded,
            isActive = product.isActive,
            isHidden = product.isHidden,
            images = images.sortedBy { it.displayOrder }.map(::toImageResponse),
            specifications = specifications.sortedBy { it.displayOrder }.map(::toSpecificationResponse),
            createdAt = product.createdAt,
            updatedAt = product.updatedAt
        )
}
