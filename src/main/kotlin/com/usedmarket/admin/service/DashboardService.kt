package com.usedmarket.admin.service

import com.usedmarket.admin.dto.BestSellingProductResponse
import com.usedmarket.admin.dto.CategoryRevenueResponse
import com.usedmarket.admin.dto.DashboardSummaryResponse
import com.usedmarket.admin.dto.OrderStatusCountResponse
import com.usedmarket.admin.dto.RevenuePointResponse
import com.usedmarket.common.exception.BadRequestException
import com.usedmarket.order.entity.OrderStatus
import com.usedmarket.order.repository.OrderItemRepository
import com.usedmarket.order.repository.OrderRepository
import com.usedmarket.product.repository.ProductRepository
import com.usedmarket.user.entity.RoleName
import com.usedmarket.user.repository.UserRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID

@Service
class DashboardService(
    private val orderRepository: OrderRepository,
    private val orderItemRepository: OrderItemRepository,
    private val productRepository: ProductRepository,
    private val userRepository: UserRepository,
    @Value("\${app.dashboard.low-stock-threshold:3}") private val lowStockThreshold: Int,
    @Value("\${app.dashboard.timezone:Asia/Ho_Chi_Minh}") private val reportingZoneId: String
) {

    /**
     * Statuses that count as realised revenue. CANCELLED/REFUNDED are excluded, and
     * PENDING is too — an unconfirmed order is not money in the door yet.
     */
    private val revenueStatuses = listOf(
        OrderStatus.CONFIRMED, OrderStatus.PROCESSING, OrderStatus.PACKED,
        OrderStatus.SHIPPED, OrderStatus.DELIVERED
    )

    private val zone: ZoneId get() = ZoneId.of(reportingZoneId)

    fun getSummary(): DashboardSummaryResponse {
        val today = LocalDate.now(zone)
        val startOfToday = today.atStartOfDay(zone).toInstant()
        val startOfTomorrow = today.plusDays(1).atStartOfDay(zone).toInstant()
        val startOfMonth = today.withDayOfMonth(1).atStartOfDay(zone).toInstant()
        val startOfNextMonth = today.withDayOfMonth(1).plusMonths(1).atStartOfDay(zone).toInstant()

        return DashboardSummaryResponse(
            totalRevenue = orderRepository.sumRevenue(revenueStatuses, null, null),
            revenueToday = orderRepository.sumRevenue(revenueStatuses, startOfToday, startOfTomorrow),
            revenueThisMonth = orderRepository.sumRevenue(revenueStatuses, startOfMonth, startOfNextMonth),

            totalOrders = orderRepository.count(),
            pendingOrders = orderRepository.countByStatus(OrderStatus.PENDING),
            completedOrders = orderRepository.countByStatus(OrderStatus.DELIVERED),
            cancelledOrders = orderRepository.countByStatus(OrderStatus.CANCELLED),

            totalCustomers = userRepository.countByRole(RoleName.CUSTOMER),
            totalProducts = productRepository.count(),
            lowStockProducts = productRepository.countByStockQuantityLessThanEqual(lowStockThreshold)
        )
    }

    /**
     * Revenue over time. Rows are fetched raw and grouped here in Kotlin rather than with
     * SQL date functions, because DATE_TRUNC/FORMATDATETIME differ between PostgreSQL and
     * H2 — doing it in code keeps one implementation working on both.
     */
    fun getRevenueChart(period: String, limit: Int): List<RevenuePointResponse> {
        val normalized = period.lowercase()
        if (normalized !in setOf("daily", "monthly")) {
            throw BadRequestException("Period must be either 'daily' or 'monthly'")
        }

        val today = LocalDate.now(zone)
        val from = if (normalized == "daily") {
            today.minusDays((limit - 1).toLong()).atStartOfDay(zone).toInstant()
        } else {
            today.withDayOfMonth(1).minusMonths((limit - 1).toLong()).atStartOfDay(zone).toInstant()
        }

        val orders = orderRepository.findForRevenueChart(revenueStatuses, from)
        val formatter = if (normalized == "daily") {
            DateTimeFormatter.ofPattern("yyyy-MM-dd")
        } else {
            DateTimeFormatter.ofPattern("yyyy-MM")
        }

        val grouped = orders.groupBy { order ->
            val date = LocalDate.ofInstant(order.createdAt ?: Instant.now(), zone)
            date.format(formatter)
        }

        // Emit a continuous series including empty periods, so charts don't show gaps.
        val periods = if (normalized == "daily") {
            (0 until limit).map { today.minusDays(it.toLong()).format(formatter) }.reversed()
        } else {
            val thisMonth = YearMonth.from(today)
            (0 until limit).map { thisMonth.minusMonths(it.toLong()).atDay(1).format(formatter) }.reversed()
        }

        return periods.map { key ->
            val ordersInPeriod = grouped[key].orEmpty()
            RevenuePointResponse(
                period = key,
                revenue = ordersInPeriod.fold(BigDecimal.ZERO) { acc, o -> acc.add(o.totalAmount) },
                orderCount = ordersInPeriod.size.toLong()
            )
        }
    }

    fun getOrdersByStatus(): List<OrderStatusCountResponse> =
        OrderStatus.entries.map { status ->
            OrderStatusCountResponse(status = status, count = orderRepository.countByStatus(status))
        }

    fun getBestSelling(limit: Int): List<BestSellingProductResponse> =
        orderItemRepository.findBestSelling(revenueStatuses, PageRequest.of(0, limit)).map { row ->
            BestSellingProductResponse(
                productId = row[0] as UUID?,
                productName = row[1] as String,
                totalQuantitySold = (row[2] as Number).toLong(),
                totalRevenue = row[3] as BigDecimal
            )
        }

    fun getRevenueByCategory(): List<CategoryRevenueResponse> =
        orderItemRepository.findRevenueByCategory(revenueStatuses).map { row ->
            CategoryRevenueResponse(
                categoryId = row[0] as UUID?,
                categoryName = row[1] as String,
                totalRevenue = row[2] as BigDecimal
            )
        }
}
