package com.usedmarket.product.mapper

import com.usedmarket.product.dto.ConditionScoreItemResponse
import com.usedmarket.product.dto.ConditionScoreResponse
import com.usedmarket.product.entity.ConditionScoreItem
import org.springframework.stereotype.Component
import java.math.BigDecimal
import java.util.UUID

@Component
class ConditionScoreMapper {

    fun toItemResponse(item: ConditionScoreItem): ConditionScoreItemResponse =
        ConditionScoreItemResponse(
            id = item.id!!,
            criterion = item.criterion,
            score = item.score,
            note = item.note
        )

    fun toResponse(productId: UUID, overallScore: BigDecimal?, items: List<ConditionScoreItem>): ConditionScoreResponse =
        ConditionScoreResponse(
            productId = productId,
            overallScore = overallScore,
            items = items.map(::toItemResponse)
        )
}
