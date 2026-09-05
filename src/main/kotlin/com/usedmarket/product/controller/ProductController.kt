package com.usedmarket.product.controller

import com.usedmarket.product.dto.ProductCreateRequest
import com.usedmarket.product.dto.ProductImageResponse
import com.usedmarket.product.dto.ProductResponse
import com.usedmarket.product.dto.ProductSummaryResponse
import com.usedmarket.product.dto.ProductUpdateRequest
import com.usedmarket.product.dto.ProductVisibilityRequest
import com.usedmarket.product.entity.ConditionGrade
import com.usedmarket.product.entity.ImageType
import com.usedmarket.product.service.ProductService
import com.usedmarket.security.CustomUserDetails
import jakarta.validation.Valid
import org.springframework.data.domain.Page
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.math.BigDecimal
import java.util.UUID

@RestController
@RequestMapping("/api/products")
class ProductController(
    private val productService: ProductService
) {

    /** Public catalog browsing with search/filter/sort (spec section 3). */
    @GetMapping
    fun search(
        @RequestParam(required = false) keyword: String?,
        @RequestParam(required = false) categoryId: UUID?,
        @RequestParam(required = false) brandId: UUID?,
        @RequestParam(required = false) minPrice: BigDecimal?,
        @RequestParam(required = false) maxPrice: BigDecimal?,
        @RequestParam(required = false) condition: ConditionGrade?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
        @RequestParam(required = false) sortBy: String?,
        @RequestParam(required = false) sortDir: String?
    ): Page<ProductSummaryResponse> =
        productService.search(keyword, categoryId, brandId, minPrice, maxPrice, condition, page, size, sortBy, sortDir)

    /** STAFF/ADMIN management listing — includes hidden/inactive products. */
    @GetMapping("/manage")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    fun getAllForManagement(
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int
    ): Page<ProductSummaryResponse> = productService.getAllForManagement(page, size)

    @GetMapping("/{id}")
    fun getById(@PathVariable id: UUID): ProductResponse = productService.getById(id)

    @GetMapping("/slug/{slug}")
    fun getBySlug(@PathVariable slug: String): ProductResponse = productService.getBySlug(slug)

    @PostMapping
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    fun create(
        @Valid @RequestBody request: ProductCreateRequest,
        @AuthenticationPrincipal principal: CustomUserDetails
    ): ResponseEntity<ProductResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(productService.create(request, principal.user))

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    fun update(@PathVariable id: UUID, @Valid @RequestBody request: ProductUpdateRequest): ProductResponse =
        productService.update(id, request)

    @PatchMapping("/{id}/visibility")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    fun setVisibility(
        @PathVariable id: UUID,
        @RequestBody request: ProductVisibilityRequest
    ): ProductResponse = productService.setVisibility(id, request)

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    fun delete(@PathVariable id: UUID): ResponseEntity<Void> {
        productService.delete(id)
        return ResponseEntity.noContent().build()
    }

    // ---------------------------------------------------------------
    // Images
    // ---------------------------------------------------------------

    @PostMapping("/{id}/images", consumes = ["multipart/form-data"])
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    fun addImage(
        @PathVariable id: UUID,
        @RequestPart file: MultipartFile,
        @RequestParam imageType: ImageType
    ): ResponseEntity<ProductImageResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(productService.addImage(id, file, imageType))

    @DeleteMapping("/{id}/images/{imageId}")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    fun deleteImage(@PathVariable id: UUID, @PathVariable imageId: UUID): ResponseEntity<Void> {
        productService.deleteImage(id, imageId)
        return ResponseEntity.noContent().build()
    }

    @PatchMapping("/{id}/images/{imageId}/primary")
    @PreAuthorize("hasAnyRole('STAFF', 'ADMIN')")
    fun setPrimaryImage(@PathVariable id: UUID, @PathVariable imageId: UUID): ProductImageResponse =
        productService.setPrimaryImage(id, imageId)
}
