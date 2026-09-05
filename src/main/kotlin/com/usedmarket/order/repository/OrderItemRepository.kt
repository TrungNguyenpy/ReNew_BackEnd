package com.usedmarket.order.repository

import com.usedmarket.order.entity.OrderItem
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface OrderItemRepository : JpaRepository<OrderItem, UUID> {

    fun findByOrderId(orderId: UUID): List<OrderItem>

    /** Used to check "has this customer purchased this product" (required before allowing a Review). */
    fun existsByOrderCustomerIdAndProductId(customerId: UUID, productId: UUID): Boolean
}
