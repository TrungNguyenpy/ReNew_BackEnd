package com.usedmarket.catalog.controller

import com.usedmarket.catalog.dto.BrandRequest
import com.usedmarket.catalog.dto.BrandResponse
import com.usedmarket.catalog.service.BrandService
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
@RequestMapping("/api/brands")
class BrandController(
    private val brandService: BrandService
) {

    @GetMapping
    fun getAll(): List<BrandResponse> = brandService.getAll()

    @GetMapping("/{id}")
    fun getById(@PathVariable id: UUID): BrandResponse = brandService.getById(id)

    @GetMapping("/slug/{slug}")
    fun getBySlug(@PathVariable slug: String): BrandResponse = brandService.getBySlug(slug)

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    fun create(@Valid @RequestBody request: BrandRequest): ResponseEntity<BrandResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(brandService.create(request))

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    fun update(@PathVariable id: UUID, @Valid @RequestBody request: BrandRequest): BrandResponse =
        brandService.update(id, request)

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    fun delete(@PathVariable id: UUID): ResponseEntity<Void> {
        brandService.delete(id)
        return ResponseEntity.noContent().build()
    }
}
