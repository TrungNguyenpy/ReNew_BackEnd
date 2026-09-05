package com.usedmarket.product.repository

import com.usedmarket.product.entity.InspectionItem
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface InspectionItemRepository : JpaRepository<InspectionItem, UUID> {

    fun findByInspectionIdOrderByDisplayOrderAsc(inspectionId: UUID): List<InspectionItem>
}
