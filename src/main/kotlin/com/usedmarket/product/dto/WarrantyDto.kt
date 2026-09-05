package com.usedmarket.product.dto

import com.usedmarket.product.entity.WarrantyType
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import java.time.LocalDate
import java.util.UUID

data class WarrantyRequest(
    @field:NotNull(message = "Warranty type is required")
    val warrantyType: WarrantyType,

    @field:Min(value = 0, message = "Duration must not be negative")
    val durationMonths: Int? = null,

    val startDate: LocalDate? = null,

    val endDate: LocalDate? = null,

    val policy: String? = null
)

data class WarrantyResponse(
    val id: UUID,
    val productId: UUID,
    val warrantyType: WarrantyType,
    val durationMonths: Int?,
    val startDate: LocalDate?,
    val endDate: LocalDate?,
    val policy: String?
)
