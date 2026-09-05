package com.usedmarket.product.mapper

import com.usedmarket.product.dto.WarrantyResponse
import com.usedmarket.product.entity.Warranty
import org.springframework.stereotype.Component

@Component
class WarrantyMapper {

    fun toResponse(warranty: Warranty): WarrantyResponse =
        WarrantyResponse(
            id = warranty.id!!,
            productId = warranty.product.id!!,
            warrantyType = warranty.warrantyType,
            durationMonths = warranty.durationMonths,
            startDate = warranty.startDate,
            endDate = warranty.endDate,
            policy = warranty.policy
        )
}
