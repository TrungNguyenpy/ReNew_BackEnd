package com.usedmarket.order.repository

import com.usedmarket.order.entity.Order
import com.usedmarket.order.entity.OrderStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional
import java.util.UUID

interface OrderRepository : JpaRepository<Order, UUID> {

    fun findByOrderNumber(orderNumber: String): Optional<Order>

    fun existsByOrderNumber(orderNumber: String): Boolean

    fun findByCustomerId(customerId: UUID, pageable: Pageable): Page<Order>

    fun findByCustomerIdAndStatus(customerId: UUID, status: OrderStatus, pageable: Pageable): Page<Order>

    fun findByStatus(status: OrderStatus, pageable: Pageable): Page<Order>
}
