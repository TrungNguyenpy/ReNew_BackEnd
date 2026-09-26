package com.usedmarket.order.repository

import com.usedmarket.order.entity.Order
import com.usedmarket.order.entity.OrderStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.math.BigDecimal
import java.time.Instant
import java.util.Optional
import java.util.UUID

interface OrderRepository : JpaRepository<Order, UUID> {

    fun findByOrderNumber(orderNumber: String): Optional<Order>

    fun existsByOrderNumber(orderNumber: String): Boolean

    fun findByCustomerId(customerId: UUID, pageable: Pageable): Page<Order>

    fun findByCustomerIdAndStatus(customerId: UUID, status: OrderStatus, pageable: Pageable): Page<Order>

    fun findByStatus(status: OrderStatus, pageable: Pageable): Page<Order>

    fun countByStatus(status: OrderStatus): Long

    /**
     * Sums revenue over orders whose status counts as realised revenue.
     * COALESCE keeps the result 0 rather than null when no orders match.
     */
    @Query(
        """
        SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o
        WHERE o.status IN :statuses
          AND (:from IS NULL OR o.createdAt >= :from)
          AND (:to IS NULL OR o.createdAt < :to)
        """
    )
    fun sumRevenue(
        @Param("statuses") statuses: Collection<OrderStatus>,
        @Param("from") from: Instant?,
        @Param("to") to: Instant?
    ): BigDecimal

    /** Raw rows for the revenue chart; grouping by day/month happens in Kotlin so the
     *  query stays portable between PostgreSQL (dev/prod) and H2 (tests). */
    @Query(
        """
        SELECT o FROM Order o
        WHERE o.status IN :statuses
          AND o.createdAt >= :from
        ORDER BY o.createdAt ASC
        """
    )
    fun findForRevenueChart(
        @Param("statuses") statuses: Collection<OrderStatus>,
        @Param("from") from: Instant
    ): List<Order>
}
