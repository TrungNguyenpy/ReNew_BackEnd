package com.usedmarket.product.service

import com.usedmarket.common.exception.ResourceNotFoundException
import com.usedmarket.product.dto.WarrantyRequest
import com.usedmarket.product.dto.WarrantyResponse
import com.usedmarket.product.entity.Warranty
import com.usedmarket.product.mapper.WarrantyMapper
import com.usedmarket.product.repository.ProductRepository
import com.usedmarket.product.repository.WarrantyRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class WarrantyService(
    private val productRepository: ProductRepository,
    private val warrantyRepository: WarrantyRepository,
    private val warrantyMapper: WarrantyMapper
) {

    fun getByProductId(productId: UUID): WarrantyResponse {
        val warranty = warrantyRepository.findByProductId(productId)
            .orElseThrow { ResourceNotFoundException("No warranty information set for this product") }
        return warrantyMapper.toResponse(warranty)
    }

    /** Creates the warranty record on first call, updates it on every subsequent call (1-1 with Product). */
    @Transactional
    fun upsert(productId: UUID, request: WarrantyRequest): WarrantyResponse {
        val existing = warrantyRepository.findByProductId(productId)

        val warranty = if (existing.isPresent) {
            existing.get().apply {
                warrantyType = request.warrantyType
                durationMonths = request.durationMonths
                startDate = request.startDate
                endDate = request.endDate
                policy = request.policy
            }
        } else {
            val product = productRepository.findById(productId)
                .orElseThrow { ResourceNotFoundException("Product not found with id: $productId") }
            Warranty(
                product = product,
                warrantyType = request.warrantyType,
                durationMonths = request.durationMonths,
                startDate = request.startDate,
                endDate = request.endDate,
                policy = request.policy
            )
        }

        return warrantyMapper.toResponse(warrantyRepository.save(warranty))
    }
}
