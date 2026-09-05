package com.usedmarket.product.dto

import jakarta.validation.Valid
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import java.math.BigDecimal
import java.util.UUID

data class ConditionScoreItemRequest(
    @field:NotBlank(message = "Criterion is required")
    val criterion: String,

    @field:Min(value = 0, message = "Score must be between 0 and 100")
    @field:Max(value = 100, message = "Score must be between 0 and 100")
    val score: Int,

    val note: String? = null
)

data class ConditionScoreItemResponse(
    val id: UUID,
    val criterion: String,
    val score: Int,
    val note: String?
)

data class ConditionScoreUpdateRequest(
    @field:Valid
    val items: List<ConditionScoreItemRequest>
)

data class ConditionScoreResponse(
    val productId: UUID,
    /** Average of all item scores, null if no breakdown has been recorded yet. */
    val overallScore: BigDecimal?,
    val items: List<ConditionScoreItemResponse>
)
