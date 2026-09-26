package com.usedmarket.admin.dto

import com.usedmarket.order.entity.OrderStatus
import java.math.BigDecimal
import java.util.UUID

data class DashboardSummaryResponse(
    val totalRevenue: BigDecimal,
    val revenueToday: BigDecimal,
    val revenueThisMonth: BigDecimal,

    val totalOrders: Long,
    val pendingOrders: Long,
    val completedOrders: Long,
    val cancelledOrders: Long,

    val totalCustomers: Long,
    val totalProducts: Long,
    val lowStockProducts: Long
)

data class RevenuePointResponse(
    /** "2026-09-13" for daily, "2026-09" for monthly. */
    val period: String,
    val revenue: BigDecimal,
    val orderCount: Long
)

data class OrderStatusCountResponse(
    val status: OrderStatus,
    val count: Long
)

data class BestSellingProductResponse(
    val productId: UUID?,
    val productName: String,
    val totalQuantitySold: Long,
    val totalRevenue: BigDecimal
)

data class CategoryRevenueResponse(
    val categoryId: UUID?,
    val categoryName: String,
    val totalRevenue: BigDecimal
)
