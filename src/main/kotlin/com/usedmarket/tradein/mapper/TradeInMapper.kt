package com.usedmarket.tradein.mapper

import com.usedmarket.tradein.dto.TradeInItemResponse
import com.usedmarket.tradein.dto.TradeInResponse
import com.usedmarket.tradein.entity.TradeInItem
import com.usedmarket.tradein.entity.TradeInRequest
import org.springframework.stereotype.Component

@Component
class TradeInMapper {

    fun toItemResponse(item: TradeInItem): TradeInItemResponse =
        TradeInItemResponse(
            id = item.id!!,
            mediaType = item.mediaType,
            mediaUrl = item.mediaUrl,
            displayOrder = item.displayOrder
        )

    fun toResponse(request: TradeInRequest, items: List<TradeInItem>): TradeInResponse =
        TradeInResponse(
            id = request.id!!,
            customerId = request.customer.id!!,
            customerName = request.customer.fullName,
            productName = request.productName,
            brand = request.brand,
            model = request.model,
            categoryId = request.category?.id,
            categoryName = request.category?.name,
            purchaseYear = request.purchaseYear,
            usageDuration = request.usageDuration,
            condition = request.condition,
            description = request.description,
            expectedPrice = request.expectedPrice,
            offeredPrice = request.offeredPrice,
            contactPhone = request.contactPhone,
            contactEmail = request.contactEmail,
            status = request.status,
            inspectedById = request.inspectedBy?.id,
            inspectedByName = request.inspectedBy?.fullName,
            inspectionNote = request.inspectionNote,
            items = items.sortedBy { it.displayOrder }.map(::toItemResponse),
            createdAt = request.createdAt
        )
}
