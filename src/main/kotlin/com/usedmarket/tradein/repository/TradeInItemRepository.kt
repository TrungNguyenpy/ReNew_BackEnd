package com.usedmarket.tradein.repository

import com.usedmarket.tradein.entity.TradeInItem
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface TradeInItemRepository : JpaRepository<TradeInItem, UUID> {

    fun findByTradeInRequestIdOrderByDisplayOrderAsc(tradeInRequestId: UUID): List<TradeInItem>
}
