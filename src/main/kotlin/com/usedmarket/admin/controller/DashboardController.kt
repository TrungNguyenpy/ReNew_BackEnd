package com.usedmarket.admin.controller

import com.usedmarket.admin.dto.BestSellingProductResponse
import com.usedmarket.admin.dto.CategoryRevenueResponse
import com.usedmarket.admin.dto.DashboardSummaryResponse
import com.usedmarket.admin.dto.OrderStatusCountResponse
import com.usedmarket.admin.dto.RevenuePointResponse
import com.usedmarket.admin.service.DashboardService
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * Dashboard and revenue reporting is an ADMIN-only capability per spec section 2 —
 * STAFF handle day-to-day operations but do not see business-wide financials.
 * The /api/admin/ prefix is additionally locked to ADMIN in SecurityConfig,
 * so this is guarded at both the URL and the method level.
 */
@RestController
@RequestMapping("/api/admin/dashboard")
@PreAuthorize("hasRole('ADMIN')")
class DashboardController(
    private val dashboardService: DashboardService
) {

    @GetMapping("/summary")
    fun getSummary(): DashboardSummaryResponse = dashboardService.getSummary()

    @GetMapping("/revenue-chart")
    fun getRevenueChart(
        @RequestParam(defaultValue = "daily") period: String,
        @RequestParam(defaultValue = "30") limit: Int
    ): List<RevenuePointResponse> = dashboardService.getRevenueChart(period, limit)

    @GetMapping("/orders-by-status")
    fun getOrdersByStatus(): List<OrderStatusCountResponse> = dashboardService.getOrdersByStatus()

    @GetMapping("/best-selling")
    fun getBestSelling(@RequestParam(defaultValue = "10") limit: Int): List<BestSellingProductResponse> =
        dashboardService.getBestSelling(limit)

    @GetMapping("/revenue-by-category")
    fun getRevenueByCategory(): List<CategoryRevenueResponse> = dashboardService.getRevenueByCategory()
}