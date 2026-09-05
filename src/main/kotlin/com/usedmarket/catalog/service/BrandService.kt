package com.usedmarket.catalog.service

import com.usedmarket.catalog.dto.BrandRequest
import com.usedmarket.catalog.dto.BrandResponse
import com.usedmarket.catalog.entity.Brand
import com.usedmarket.catalog.mapper.BrandMapper
import com.usedmarket.catalog.repository.BrandRepository
import com.usedmarket.common.exception.BadRequestException
import com.usedmarket.common.exception.DuplicateResourceException
import com.usedmarket.common.exception.ResourceNotFoundException
import com.usedmarket.product.repository.ProductRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class BrandService(
    private val brandRepository: BrandRepository,
    private val productRepository: ProductRepository,
    private val brandMapper: BrandMapper
) {

    fun getAll(): List<BrandResponse> =
        brandRepository.findAll().map(brandMapper::toResponse)

    fun getById(id: UUID): BrandResponse =
        brandMapper.toResponse(findEntityById(id))

    fun getBySlug(slug: String): BrandResponse {
        val brand = brandRepository.findBySlug(slug)
            .orElseThrow { ResourceNotFoundException("Brand not found with slug: $slug") }
        return brandMapper.toResponse(brand)
    }

    @Transactional
    fun create(request: BrandRequest): BrandResponse {
        if (brandRepository.existsBySlug(request.slug)) {
            throw DuplicateResourceException("A brand with slug '${request.slug}' already exists")
        }

        val brand = Brand(
            name = request.name,
            slug = request.slug,
            logoUrl = request.logoUrl,
            description = request.description,
            isActive = request.isActive
        )
        return brandMapper.toResponse(brandRepository.save(brand))
    }

    @Transactional
    fun update(id: UUID, request: BrandRequest): BrandResponse {
        val brand = findEntityById(id)

        if (request.slug != brand.slug && brandRepository.existsBySlug(request.slug)) {
            throw DuplicateResourceException("A brand with slug '${request.slug}' already exists")
        }

        brand.name = request.name
        brand.slug = request.slug
        brand.logoUrl = request.logoUrl
        brand.description = request.description
        brand.isActive = request.isActive

        return brandMapper.toResponse(brandRepository.save(brand))
    }

    @Transactional
    fun delete(id: UUID) {
        val brand = findEntityById(id)

        if (productRepository.existsByBrandId(id)) {
            throw BadRequestException("Cannot delete a brand that still has products")
        }

        brandRepository.delete(brand)
    }

    private fun findEntityById(id: UUID): Brand =
        brandRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("Brand not found with id: $id") }
}
