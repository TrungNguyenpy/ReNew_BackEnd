package com.usedmarket.product.service

import com.usedmarket.common.exception.ResourceNotFoundException
import com.usedmarket.product.dto.InspectionCreateRequest
import com.usedmarket.product.dto.InspectionResponse
import com.usedmarket.product.dto.PublicInspectionResponse
import com.usedmarket.product.entity.InspectionItem
import com.usedmarket.product.entity.InspectionItemStatus
import com.usedmarket.product.entity.InspectionStatus
import com.usedmarket.product.entity.ProductInspection
import com.usedmarket.product.mapper.InspectionMapper
import com.usedmarket.product.repository.InspectionItemRepository
import com.usedmarket.product.repository.ProductInspectionRepository
import com.usedmarket.product.repository.ProductRepository
import com.usedmarket.user.entity.User
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

@Service
class InspectionService(
    private val productRepository: ProductRepository,
    private val productInspectionRepository: ProductInspectionRepository,
    private val inspectionItemRepository: InspectionItemRepository,
    private val inspectionMapper: InspectionMapper
) {

    /** Public product-page report — the latest inspection explicitly marked isPublic. */
    fun getPublicReport(productId: UUID): PublicInspectionResponse {
        val inspection = productInspectionRepository.findFirstByProductIdAndIsPublicTrueOrderByCreatedAtDesc(productId)
            ?: throw ResourceNotFoundException("No public inspection report available for this product")
        val items = inspectionItemRepository.findByInspectionIdOrderByDisplayOrderAsc(inspection.id!!)
        return inspectionMapper.toPublicResponse(inspection, items)
    }

    /** STAFF/ADMIN listing — every inspection pass for this product, full detail. */
    fun getAllForProduct(productId: UUID): List<InspectionResponse> {
        val inspections = productInspectionRepository.findByProductIdOrderByCreatedAtDesc(productId)
        return inspections.map { inspection ->
            val items = inspectionItemRepository.findByInspectionIdOrderByDisplayOrderAsc(inspection.id!!)
            inspectionMapper.toResponse(inspection, items)
        }
    }

    @Transactional
    fun create(productId: UUID, request: InspectionCreateRequest, inspector: User): InspectionResponse {
        val product = productRepository.findById(productId)
            .orElseThrow { ResourceNotFoundException("Product not found with id: $productId") }

        val inspection = ProductInspection(
            product = product,
            inspector = inspector,
            status = InspectionStatus.COMPLETED,
            inspectionDate = request.inspectionDate ?: Instant.now(),
            resultSummary = request.resultSummary,
            internalNotes = request.internalNotes,
            isPublic = request.isPublic
        )
        productInspectionRepository.save(inspection)

        val items = request.items.map {
            InspectionItem(
                inspection = inspection,
                itemName = it.itemName,
                status = it.status,
                note = it.note,
                displayOrder = it.displayOrder
            )
        }
        val savedItems = inspectionItemRepository.saveAll(items)

        // PASS = 100 points, WARNING = 50, FAIL = 0 — averaged into an overall 0-100 inspection score.
        inspection.inspectionScore = computeScore(savedItems)
        productInspectionRepository.save(inspection)

        return inspectionMapper.toResponse(inspection, savedItems)
    }

    @Transactional
    fun setPublic(productId: UUID, inspectionId: UUID, isPublic: Boolean): InspectionResponse {
        val inspection = productInspectionRepository.findById(inspectionId)
            .orElseThrow { ResourceNotFoundException("Inspection not found with id: $inspectionId") }
        if (inspection.product.id != productId) {
            throw ResourceNotFoundException("Inspection not found with id: $inspectionId")
        }

        inspection.isPublic = isPublic
        productInspectionRepository.save(inspection)

        val items = inspectionItemRepository.findByInspectionIdOrderByDisplayOrderAsc(inspectionId)
        return inspectionMapper.toResponse(inspection, items)
    }

    private fun computeScore(items: List<InspectionItem>): Int {
        if (items.isEmpty()) return 0
        val total = items.map {
            when (it.status) {
                InspectionItemStatus.PASS -> 100
                InspectionItemStatus.WARNING -> 50
                InspectionItemStatus.FAIL -> 0
            }
        }.sum()
        return total / items.size
    }
}
