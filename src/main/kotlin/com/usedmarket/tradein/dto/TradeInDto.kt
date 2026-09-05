package com.usedmarket.tradein.dto

import com.usedmarket.product.entity.ConditionGrade
import com.usedmarket.tradein.entity.MediaType
import com.usedmarket.tradein.entity.TradeInStatus
import jakarta.validation.constraints.DecimalMin
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.math.BigDecimal
import java.time.Instant
import java.util.UUID

data class TradeInCreateRequest(
    @field:NotBlank(message = "Product name is required")
    val productName: String,

    val brand: String? = null,
    val model: String? = null,
    val categoryId: UUID? = null,
    val purchaseYear: Int? = null,
    val usageDuration: String? = null,
    val condition: ConditionGrade? = null,
    val description: String? = null,

    @field:DecimalMin(value = "0.0", inclusive = true, message = "Expected price must not be negative")
    val expectedPrice: BigDecimal? = null,

    @field:NotBlank(message = "Contact phone is required")
    val contactPhone: String,

    val contactEmail: String? = null
)

data class TradeInOfferRequest(
    @field:NotNull(message = "Offered price is required")
    @field:DecimalMin(value = "0.0", inclusive = true, message = "Offered price must not be negative")
    val offeredPrice: BigDecimal,

    val inspectionNote: String? = null
)

data class TradeInStatusUpdateRequest(
    @field:NotNull(message = "Status is required")
    val status: TradeInStatus,

    val inspectionNote: String? = null
)

data class TradeInItemResponse(
    val id: UUID,
    val mediaType: MediaType,
    val mediaUrl: String,
    val displayOrder: Int
)

data class TradeInResponse(
    val id: UUID,
    val customerId: UUID,
    val customerName: String,
    val productName: String,
    val brand: String?,
    val model: String?,
    val categoryId: UUID?,
    val categoryName: String?,
    val purchaseYear: Int?,
    val usageDuration: String?,
    val condition: ConditionGrade?,
    val description: String?,
    val expectedPrice: BigDecimal?,
    val offeredPrice: BigDecimal?,
    val contactPhone: String,
    val contactEmail: String?,
    val status: TradeInStatus,
    val inspectedById: UUID?,
    val inspectedByName: String?,
    val inspectionNote: String?,
    val items: List<TradeInItemResponse>,
    val createdAt: Instant?
)
