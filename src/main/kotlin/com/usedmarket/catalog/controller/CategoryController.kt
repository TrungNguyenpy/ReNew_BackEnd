package com.usedmarket.catalog.controller

import com.usedmarket.catalog.dto.CategoryRequest
import com.usedmarket.catalog.dto.CategoryResponse
import com.usedmarket.catalog.service.CategoryService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/categories")
class CategoryController(
    private val categoryService: CategoryService
) {

    @GetMapping
    fun getAll(): List<CategoryResponse> = categoryService.getAll()

    @GetMapping("/tree")
    fun getTree(): List<CategoryResponse> = categoryService.getTree()

    @GetMapping("/{id}")
    fun getById(@PathVariable id: UUID): CategoryResponse = categoryService.getById(id)

    @GetMapping("/slug/{slug}")
    fun getBySlug(@PathVariable slug: String): CategoryResponse = categoryService.getBySlug(slug)

    /** Category management is ADMIN-only per spec section 2 (not delegated to STAFF). */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    fun create(@Valid @RequestBody request: CategoryRequest): ResponseEntity<CategoryResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(categoryService.create(request))

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    fun update(@PathVariable id: UUID, @Valid @RequestBody request: CategoryRequest): CategoryResponse =
        categoryService.update(id, request)

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    fun delete(@PathVariable id: UUID): ResponseEntity<Void> {
        categoryService.delete(id)
        return ResponseEntity.noContent().build()
    }
}
