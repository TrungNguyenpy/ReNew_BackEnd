package com.usedmarket.order.repository

import com.usedmarket.order.entity.OrderStatusHistory
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface OrderStatusHistoryRepository : JpaRepository<OrderStatusHistory, UUID> {

    fun findByOrderIdOrderByCreatedAtAsc(orderId: UUID): List<OrderStatusHistory>
}
