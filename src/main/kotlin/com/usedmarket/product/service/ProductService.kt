package com.usedmarket.product.service

import com.usedmarket.catalog.repository.BrandRepository
import com.usedmarket.catalog.repository.CategoryRepository
import com.usedmarket.common.exception.DuplicateResourceException
import com.usedmarket.common.exception.ResourceNotFoundException
import com.usedmarket.inventory.service.InventoryService
import com.usedmarket.media.CloudinaryService
import com.usedmarket.product.dto.ProductCreateRequest
import com.usedmarket.product.dto.ProductImageResponse
import com.usedmarket.product.dto.ProductResponse
import com.usedmarket.product.dto.ProductSummaryResponse
import com.usedmarket.product.dto.ProductUpdateRequest
import com.usedmarket.product.dto.ProductVisibilityRequest
import com.usedmarket.product.entity.ConditionGrade
import com.usedmarket.product.entity.ImageType
import com.usedmarket.product.entity.Product
import com.usedmarket.product.entity.ProductImage
import com.usedmarket.product.entity.ProductSpecification
import com.usedmarket.product.mapper.ProductMapper
import com.usedmarket.product.repository.ProductImageRepository
import com.usedmarket.product.repository.ProductRepository
import com.usedmarket.product.repository.ProductSpecificationRepository
import com.usedmarket.user.entity.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.math.BigDecimal
import java.util.UUID

@Service
class ProductService(
    private val productRepository: ProductRepository,
    private val categoryRepository: CategoryRepository,
    private val brandRepository: BrandRepository,
    private val productImageRepository: ProductImageRepository,
    private val productSpecificationRepository: ProductSpecificationRepository,
    private val cloudinaryService: CloudinaryService,
    private val inventoryService: InventoryService,
    private val productMapper: ProductMapper
) {

    private val allowedSortFields = mapOf(
        "price" to "price",
        "createdAt" to "createdAt",
        "name" to "name"
    )

    // ---------------------------------------------------------------
    // Read
    // ---------------------------------------------------------------

    fun search(
        keyword: String?,
        categoryId: UUID?,
        brandId: UUID?,
        minPrice: BigDecimal?,
        maxPrice: BigDecimal?,
        condition: ConditionGrade?,
        page: Int,
        size: Int,
        sortBy: String?,
        sortDir: String?
    ): Page<ProductSummaryResponse> {
        val field = allowedSortFields[sortBy] ?: "createdAt"
        val direction = if (sortDir?.equals("asc", ignoreCase = true) == true) Sort.Direction.ASC else Sort.Direction.DESC
        val pageable: Pageable = PageRequest.of(page, size, Sort.by(direction, field))

        val products = productRepository.search(
            keyword?.takeIf { it.isNotBlank() },
            categoryId, brandId, minPrice, maxPrice, condition, pageable
        )

        return products.map { product ->
            val primaryImageUrl = productImageRepository.findByProductIdAndIsPrimaryTrue(product.id!!)?.imageUrl
            productMapper.toSummaryResponse(product, primaryImageUrl)
        }
    }

    /** STAFF/ADMIN listing that includes hidden/inactive products for management purposes. */
    fun getAllForManagement(page: Int, size: Int): Page<ProductSummaryResponse> {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        return productRepository.findAll(pageable).map { product ->
            val primaryImageUrl = productImageRepository.findByProductIdAndIsPrimaryTrue(product.id!!)?.imageUrl
            productMapper.toSummaryResponse(product, primaryImageUrl)
        }
    }

    fun getById(id: UUID): ProductResponse {
        val product = findEntityById(id)
        guardVisibility(product)
        return buildDetailResponse(product)
    }

    fun getBySlug(slug: String): ProductResponse {
        val product = productRepository.findBySlug(slug)
            .orElseThrow { ResourceNotFoundException("Product not found with slug: $slug") }
        guardVisibility(product)
        return buildDetailResponse(product)
    }

    // ---------------------------------------------------------------
    // Write (STAFF/ADMIN)
    // ---------------------------------------------------------------

    @Transactional
    fun create(request: ProductCreateRequest, actingUser: User): ProductResponse {
        if (productRepository.existsBySlug(request.slug)) {
            throw DuplicateResourceException("A product with slug '${request.slug}' already exists")
        }
        val category = categoryRepository.findById(request.categoryId)
            .orElseThrow { ResourceNotFoundException("Category not found with id: ${request.categoryId}") }
        val brand = brandRepository.findById(request.brandId)
            .orElseThrow { ResourceNotFoundException("Brand not found with id: ${request.brandId}") }

        val product = Product(
            name = request.name,
            slug = request.slug,
            description = request.description,
            category = category,
            brand = brand,
            model = request.model,
            price = request.price,
            originalPrice = request.originalPrice,
            stockQuantity = request.stockQuantity,
            condition = request.condition,
            manufactureYear = request.manufactureYear,
            purchaseYear = request.purchaseYear,
            usageDuration = request.usageDuration,
            cosmeticCondition = request.cosmeticCondition,
            functionalCondition = request.functionalCondition,
            knownDefects = request.knownDefects,
            repairHistory = request.repairHistory,
            accessoriesIncluded = request.accessoriesIncluded
        )
        productRepository.save(product)

        // Phase 6: every new product gets its authoritative Inventory ledger row
        // right away, seeded from the same stockQuantity given at creation time.
        inventoryService.initializeForProduct(product, request.stockQuantity, actingUser)

        val specs = request.specifications.map {
            ProductSpecification(
                product = product,
                specKey = it.specKey,
                specValue = it.specValue,
                displayOrder = it.displayOrder
            )
        }
        if (specs.isNotEmpty()) productSpecificationRepository.saveAll(specs)

        return productMapper.toResponse(product, emptyList(), specs)
    }

    @Transactional
    fun update(id: UUID, request: ProductUpdateRequest): ProductResponse {
        val product = findEntityById(id)

        if (request.slug != product.slug && productRepository.existsBySlug(request.slug)) {
            throw DuplicateResourceException("A product with slug '${request.slug}' already exists")
        }
        val category = categoryRepository.findById(request.categoryId)
            .orElseThrow { ResourceNotFoundException("Category not found with id: ${request.categoryId}") }
        val brand = brandRepository.findById(request.brandId)
            .orElseThrow { ResourceNotFoundException("Brand not found with id: ${request.brandId}") }

        product.name = request.name
        product.slug = request.slug
        product.description = request.description
        product.category = category
        product.brand = brand
        product.model = request.model
        product.price = request.price
        product.originalPrice = request.originalPrice
        product.stockQuantity = request.stockQuantity
        product.condition = request.condition
        product.manufactureYear = request.manufactureYear
        product.purchaseYear = request.purchaseYear
        product.usageDuration = request.usageDuration
        product.cosmeticCondition = request.cosmeticCondition
        product.functionalCondition = request.functionalCondition
        product.knownDefects = request.knownDefects
        product.repairHistory = request.repairHistory
        product.accessoriesIncluded = request.accessoriesIncluded
        product.isActive = request.isActive
        product.isHidden = request.isHidden
        productRepository.save(product)

        // Full replace of specifications to keep PUT semantics simple and predictable.
        productSpecificationRepository.deleteByProductId(id)
        val specs = request.specifications.map {
            ProductSpecification(
                product = product,
                specKey = it.specKey,
                specValue = it.specValue,
                displayOrder = it.displayOrder
            )
        }
        if (specs.isNotEmpty()) productSpecificationRepository.saveAll(specs)

        val images = productImageRepository.findByProductIdOrderByDisplayOrderAsc(id)
        return productMapper.toResponse(product, images, specs)
    }

    @Transactional
    fun setVisibility(id: UUID, request: ProductVisibilityRequest): ProductResponse {
        val product = findEntityById(id)
        product.isHidden = request.isHidden
        productRepository.save(product)
        return buildDetailResponse(product)
    }

    @Transactional
    fun delete(id: UUID) {
        val product = findEntityById(id)
        // Cascade rules configured at the database level (Phase 2 migrations) handle
        // dependent rows: product_images/specifications cascade-delete, order_items
        // keep their historical snapshot via ON DELETE SET NULL.
        productRepository.delete(product)
    }

    // ---------------------------------------------------------------
    // Images (STAFF/ADMIN)
    // ---------------------------------------------------------------

    @Transactional
    fun addImage(id: UUID, file: MultipartFile, imageType: ImageType): ProductImageResponse {
        val product = findEntityById(id)
        val uploadResult = cloudinaryService.upload(file, "products/$id")

        val isFirstImage = productImageRepository.findByProductIdOrderByDisplayOrderAsc(id).isEmpty()
        val image = ProductImage(
            product = product,
            imageUrl = uploadResult.url,
            cloudinaryPublicId = uploadResult.publicId,
            imageType = imageType,
            isPrimary = isFirstImage
        )
        productImageRepository.save(image)
        return productMapper.toImageResponse(image)
    }

    @Transactional
    fun deleteImage(id: UUID, imageId: UUID) {
        val image = productImageRepository.findById(imageId)
            .orElseThrow { ResourceNotFoundException("Image not found with id: $imageId") }
        if (image.product.id != id) {
            throw ResourceNotFoundException("Image not found with id: $imageId")
        }

        image.cloudinaryPublicId?.let { cloudinaryService.delete(it) }
        val wasPrimary = image.isPrimary
        productImageRepository.delete(image)

        if (wasPrimary) {
            val remaining = productImageRepository.findByProductIdOrderByDisplayOrderAsc(id)
            remaining.firstOrNull()?.let {
                it.isPrimary = true
                productImageRepository.save(it)
            }
        }
    }

    @Transactional
    fun setPrimaryImage(id: UUID, imageId: UUID): ProductImageResponse {
        val target = productImageRepository.findById(imageId)
            .orElseThrow { ResourceNotFoundException("Image not found with id: $imageId") }
        if (target.product.id != id) {
            throw ResourceNotFoundException("Image not found with id: $imageId")
        }

        val allImages = productImageRepository.findByProductIdOrderByDisplayOrderAsc(id)
        allImages.forEach { it.isPrimary = (it.id == imageId) }
        productImageRepository.saveAll(allImages)

        return productMapper.toImageResponse(target.apply { isPrimary = true })
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private fun buildDetailResponse(product: Product): ProductResponse {
        val images = productImageRepository.findByProductIdOrderByDisplayOrderAsc(product.id!!)
        val specs = productSpecificationRepository.findByProductIdOrderByDisplayOrderAsc(product.id!!)
        return productMapper.toResponse(product, images, specs)
    }

    private fun findEntityById(id: UUID): Product =
        productRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("Product not found with id: $id") }

    /** Hidden/inactive listings are only visible to STAFF/ADMIN, never to public/customer requests. */
    private fun guardVisibility(product: Product) {
        if ((product.isHidden || !product.isActive) && !isStaffOrAdmin()) {
            throw ResourceNotFoundException("Product not found with id: ${product.id}")
        }
    }

    private fun isStaffOrAdmin(): Boolean {
        val authentication = SecurityContextHolder.getContext().authentication ?: return false
        return authentication.authorities.any {
            it.authority == "ROLE_STAFF" || it.authority == "ROLE_ADMIN"
        }
    }
}
