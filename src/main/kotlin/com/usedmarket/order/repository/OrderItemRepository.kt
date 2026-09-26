package com.usedmarket.order.repository

import com.usedmarket.order.entity.OrderItem
import com.usedmarket.order.entity.OrderStatus
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.UUID

interface OrderItemRepository : JpaRepository<OrderItem, UUID> {

    fun findByOrderId(orderId: UUID): List<OrderItem>

    /** Used to check "has this customer purchased this product" (required before allowing a Review). */
    fun existsByOrderCustomerIdAndProductId(customerId: UUID, productId: UUID): Boolean

    /**
     * Best-selling products, ranked by total units sold across revenue-realising orders.
     * Groups on the snapshot name so items whose Product row was later deleted still
     * appear correctly in historical reporting.
     * Each row: [productId (nullable), productName, totalQuantity, totalRevenue]
     */
    @Query(
        """
        SELECT oi.product.id, oi.productNameSnapshot, SUM(oi.quantity), SUM(oi.subtotal)
        FROM OrderItem oi
        WHERE oi.order.status IN :statuses
        GROUP BY oi.product.id, oi.productNameSnapshot
        ORDER BY SUM(oi.quantity) DESC
        """
    )
    fun findBestSelling(
        @Param("statuses") statuses: Collection<OrderStatus>,
        pageable: Pageable
    ): List<Array<Any?>>

    /**
     * Revenue grouped by product category, for the sales-by-category chart.
     * Each row: [categoryId, categoryName, totalRevenue]
     */
    @Query(
        """
        SELECT oi.product.category.id, oi.product.category.name, SUM(oi.subtotal)
        FROM OrderItem oi
        WHERE oi.order.status IN :statuses AND oi.product IS NOT NULL
        GROUP BY oi.product.category.id, oi.product.category.name
        ORDER BY SUM(oi.subtotal) DESC
        """
    )
    fun findRevenueByCategory(@Param("statuses") statuses: Collection<OrderStatus>): List<Array<Any?>>
}
