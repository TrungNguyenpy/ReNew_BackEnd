package com.usedmarket.tradein.repository

import com.usedmarket.tradein.entity.TradeInRequest
import com.usedmarket.tradein.entity.TradeInStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface TradeInRequestRepository : JpaRepository<TradeInRequest, UUID> {

    fun findByCustomerId(customerId: UUID, pageable: Pageable): Page<TradeInRequest>

    fun findByStatus(status: TradeInStatus, pageable: Pageable): Page<TradeInRequest>
}
