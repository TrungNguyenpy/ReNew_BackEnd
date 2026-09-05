package com.usedmarket.catalog.service

import com.usedmarket.catalog.dto.CategoryRequest
import com.usedmarket.catalog.dto.CategoryResponse
import com.usedmarket.catalog.entity.Category
import com.usedmarket.catalog.mapper.CategoryMapper
import com.usedmarket.catalog.repository.CategoryRepository
import com.usedmarket.common.exception.BadRequestException
import com.usedmarket.common.exception.DuplicateResourceException
import com.usedmarket.common.exception.ResourceNotFoundException
import com.usedmarket.product.repository.ProductRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class CategoryService(
    private val categoryRepository: CategoryRepository,
    private val productRepository: ProductRepository,
    private val categoryMapper: CategoryMapper
) {

    fun getAll(): List<CategoryResponse> =
        categoryRepository.findAll().map(categoryMapper::toResponse)

    /** Builds the full nested tree starting from root categories (spec section 11). */
    fun getTree(): List<CategoryResponse> {
        val all = categoryRepository.findAll()
        val byParentId = all.filter { it.parent != null }.groupBy { it.parent!!.id }

        fun buildNode(category: Category): CategoryResponse {
            val childNodes = byParentId[category.id].orEmpty().map(::buildNode)
            return categoryMapper.toResponse(category).copy(children = childNodes)
        }

        return all.filter { it.parent == null }.map(::buildNode)
    }

    fun getById(id: UUID): CategoryResponse =
        categoryMapper.toResponse(findEntityById(id))

    fun getBySlug(slug: String): CategoryResponse {
        val category = categoryRepository.findBySlug(slug)
            .orElseThrow { ResourceNotFoundException("Category not found with slug: $slug") }
        return categoryMapper.toResponse(category)
    }

    @Transactional
    fun create(request: CategoryRequest): CategoryResponse {
        if (categoryRepository.existsBySlug(request.slug)) {
            throw DuplicateResourceException("A category with slug '${request.slug}' already exists")
        }

        val parent = request.parentId?.let { findEntityById(it) }

        val category = Category(
            name = request.name,
            slug = request.slug,
            description = request.description,
            imageUrl = request.imageUrl,
            parent = parent,
            isActive = request.isActive,
            displayOrder = request.displayOrder
        )
        return categoryMapper.toResponse(categoryRepository.save(category))
    }

    @Transactional
    fun update(id: UUID, request: CategoryRequest): CategoryResponse {
        val category = findEntityById(id)

        if (request.slug != category.slug && categoryRepository.existsBySlug(request.slug)) {
            throw DuplicateResourceException("A category with slug '${request.slug}' already exists")
        }
        if (request.parentId == id) {
            throw BadRequestException("A category cannot be its own parent")
        }

        val parent = request.parentId?.let { findEntityById(it) }

        category.name = request.name
        category.slug = request.slug
        category.description = request.description
        category.imageUrl = request.imageUrl
        category.parent = parent
        category.isActive = request.isActive
        category.displayOrder = request.displayOrder

        return categoryMapper.toResponse(categoryRepository.save(category))
    }

    @Transactional
    fun delete(id: UUID) {
        val category = findEntityById(id)

        if (category.children.isNotEmpty()) {
            throw BadRequestException("Cannot delete a category that still has subcategories")
        }
        if (productRepository.existsByCategoryId(id)) {
            throw BadRequestException("Cannot delete a category that still has products")
        }

        categoryRepository.delete(category)
    }

    private fun findEntityById(id: UUID): Category =
        categoryRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("Category not found with id: $id") }
}
