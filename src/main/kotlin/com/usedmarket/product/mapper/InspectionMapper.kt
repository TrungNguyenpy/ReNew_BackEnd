package com.usedmarket.product.mapper

import com.usedmarket.product.dto.InspectionItemResponse
import com.usedmarket.product.dto.InspectionResponse
import com.usedmarket.product.dto.PublicInspectionResponse
import com.usedmarket.product.entity.InspectionItem
import com.usedmarket.product.entity.ProductInspection
import org.springframework.stereotype.Component

@Component
class InspectionMapper {

    fun toItemResponse(item: InspectionItem): InspectionItemResponse =
        InspectionItemResponse(
            id = item.id!!,
            itemName = item.itemName,
            status = item.status,
            note = item.note,
            displayOrder = item.displayOrder
        )

    fun toResponse(inspection: ProductInspection, items: List<InspectionItem>): InspectionResponse =
        InspectionResponse(
            id = inspection.id!!,
            productId = inspection.product.id!!,
            inspectorId = inspection.inspector.id!!,
            inspectorName = inspection.inspector.fullName,
            status = inspection.status,
            inspectionDate = inspection.inspectionDate,
            inspectionScore = inspection.inspectionScore,
            resultSummary = inspection.resultSummary,
            internalNotes = inspection.internalNotes,
            isPublic = inspection.isPublic,
            items = items.sortedBy { it.displayOrder }.map(::toItemResponse),
            createdAt = inspection.createdAt
        )

    fun toPublicResponse(inspection: ProductInspection, items: List<InspectionItem>): PublicInspectionResponse =
        PublicInspectionResponse(
            id = inspection.id!!,
            productId = inspection.product.id!!,
            inspectionDate = inspection.inspectionDate,
            inspectionScore = inspection.inspectionScore,
            resultSummary = inspection.resultSummary,
            items = items.sortedBy { it.displayOrder }.map(::toItemResponse)
        )
}
