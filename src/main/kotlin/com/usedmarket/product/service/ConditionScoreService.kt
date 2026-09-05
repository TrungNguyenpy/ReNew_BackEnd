package com.usedmarket.product.service

import com.usedmarket.common.exception.ResourceNotFoundException
import com.usedmarket.product.dto.ConditionScoreResponse
import com.usedmarket.product.dto.ConditionScoreUpdateRequest
import com.usedmarket.product.entity.ConditionScoreItem
import com.usedmarket.product.mapper.ConditionScoreMapper
import com.usedmarket.product.repository.ConditionScoreItemRepository
import com.usedmarket.product.repository.ProductRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.util.UUID

@Service
class ConditionScoreService(
    private val productRepository: ProductRepository,
    private val conditionScoreItemRepository: ConditionScoreItemRepository,
    private val conditionScoreMapper: ConditionScoreMapper
) {

    fun getByProductId(productId: UUID): ConditionScoreResponse {
        val product = productRepository.findById(productId)
            .orElseThrow { ResourceNotFoundException("Product not found with id: $productId") }
        val items = conditionScoreItemRepository.findByProductId(productId)
        return conditionScoreMapper.toResponse(productId, product.conditionScore, items)
    }

    /** Full replace of the score breakdown; recomputes and persists Product.conditionScore as the average. */
    @Transactional
    fun update(productId: UUID, request: ConditionScoreUpdateRequest): ConditionScoreResponse {
        val product = productRepository.findById(productId)
            .orElseThrow { ResourceNotFoundException("Product not found with id: $productId") }

        conditionScoreItemRepository.deleteByProductId(productId)

        val items = request.items.map {
            ConditionScoreItem(
                product = product,
                criterion = it.criterion,
                score = it.score,
                note = it.note
            )
        }
        val saved = if (items.isNotEmpty()) conditionScoreItemRepository.saveAll(items) else emptyList()

        val average = if (saved.isNotEmpty()) {
            BigDecimal(saved.sumOf { it.score }).divide(BigDecimal(saved.size), 2, RoundingMode.HALF_UP)
        } else {
            null
        }
        product.conditionScore = average
        productRepository.save(product)

        return conditionScoreMapper.toResponse(productId, average, saved)
    }
}
