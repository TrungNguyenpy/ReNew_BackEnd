package com.usedmarket.product.dto

import com.usedmarket.product.entity.InspectionItemStatus
import com.usedmarket.product.entity.InspectionStatus
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import java.time.Instant
import java.util.UUID

data class InspectionItemRequest(
    @field:NotBlank(message = "Item name is required")
    val itemName: String,

    val status: InspectionItemStatus,

    val note: String? = null,

    val displayOrder: Int = 0
)

data class InspectionItemResponse(
    val id: UUID,
    val itemName: String,
    val status: InspectionItemStatus,
    val note: String?,
    val displayOrder: Int
)

data class InspectionCreateRequest(
    val inspectionDate: Instant? = null,

    val resultSummary: String? = null,

    val internalNotes: String? = null,

    val isPublic: Boolean = false,

    @field:NotEmpty(message = "At least one inspection item is required")
    @field:Valid
    val items: List<InspectionItemRequest>
)

/** Full detail — STAFF/ADMIN only, includes internal notes and inspector identity. */
data class InspectionResponse(
    val id: UUID,
    val productId: UUID,
    val inspectorId: UUID,
    val inspectorName: String,
    val status: InspectionStatus,
    val inspectionDate: Instant?,
    val inspectionScore: Int?,
    val resultSummary: String?,
    val internalNotes: String?,
    val isPublic: Boolean,
    val items: List<InspectionItemResponse>,
    val createdAt: Instant?
)

/** Redacted version shown to customers on the product page — no internal notes, no inspector identity. */
data class PublicInspectionResponse(
    val id: UUID,
    val productId: UUID,
    val inspectionDate: Instant?,
    val inspectionScore: Int?,
    val resultSummary: String?,
    val items: List<InspectionItemResponse>
)

data class InspectionPublishRequest(
    val isPublic: Boolean
)
