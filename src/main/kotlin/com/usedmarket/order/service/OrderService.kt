package com.usedmarket.order.service

import com.usedmarket.cart.entity.Cart
import com.usedmarket.cart.repository.CartItemRepository
import com.usedmarket.cart.repository.CartRepository
import com.usedmarket.common.exception.BadRequestException
import com.usedmarket.common.exception.InvalidOrderStateException
import com.usedmarket.common.exception.ResourceNotFoundException
import com.usedmarket.coupon.entity.Coupon
import com.usedmarket.coupon.entity.CouponUsage
import com.usedmarket.coupon.entity.DiscountType
import com.usedmarket.coupon.repository.CouponRepository
import com.usedmarket.coupon.repository.CouponUsageRepository
import com.usedmarket.inventory.service.InventoryService
import com.usedmarket.order.dto.CheckoutRequest
import com.usedmarket.order.dto.OrderResponse
import com.usedmarket.order.dto.OrderStatusHistoryResponse
import com.usedmarket.order.dto.OrderStatusUpdateRequest
import com.usedmarket.order.dto.OrderSummaryResponse
import com.usedmarket.order.entity.Order
import com.usedmarket.order.entity.OrderItem
import com.usedmarket.order.entity.OrderStatus
import com.usedmarket.order.entity.OrderStatusHistory
import com.usedmarket.order.mapper.OrderMapper
import com.usedmarket.order.repository.OrderItemRepository
import com.usedmarket.order.repository.OrderRepository
import com.usedmarket.order.repository.OrderStatusHistoryRepository
import com.usedmarket.payment.entity.Payment
import com.usedmarket.payment.entity.PaymentMethod
import com.usedmarket.payment.entity.PaymentStatus
import com.usedmarket.payment.repository.PaymentRepository
import com.usedmarket.shipment.entity.Shipment
import com.usedmarket.shipment.repository.ShipmentRepository
import com.usedmarket.user.entity.RoleName
import com.usedmarket.user.entity.User
import com.usedmarket.user.repository.UserRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

@Service
class OrderService(
    private val orderRepository: OrderRepository,
    private val orderItemRepository: OrderItemRepository,
    private val orderStatusHistoryRepository: OrderStatusHistoryRepository,
    private val cartRepository: CartRepository,
    private val cartItemRepository: CartItemRepository,
    private val couponRepository: CouponRepository,
    private val couponUsageRepository: CouponUsageRepository,
    private val paymentRepository: PaymentRepository,
    private val shipmentRepository: ShipmentRepository,
    private val inventoryService: InventoryService,
    private val userRepository: UserRepository,
    private val orderMapper: OrderMapper,
    @Value("\${app.shipping.flat-fee}") private val flatShippingFee: BigDecimal,
    @Value("\${app.shipping.free-threshold}") private val freeShippingThreshold: BigDecimal
) {

    /** Terminal/next states allowed from each OrderStatus — guards against nonsensical jumps. */
    private val allowedTransitions: Map<OrderStatus, Set<OrderStatus>> = mapOf(
        OrderStatus.PENDING to setOf(OrderStatus.CONFIRMED, OrderStatus.CANCELLED),
        OrderStatus.CONFIRMED to setOf(OrderStatus.PROCESSING, OrderStatus.CANCELLED),
        OrderStatus.PROCESSING to setOf(OrderStatus.PACKED, OrderStatus.CANCELLED),
        OrderStatus.PACKED to setOf(OrderStatus.SHIPPED),
        OrderStatus.SHIPPED to setOf(OrderStatus.DELIVERED),
        OrderStatus.DELIVERED to setOf(OrderStatus.RETURN_REQUESTED),
        OrderStatus.RETURN_REQUESTED to setOf(OrderStatus.RETURNED),
        OrderStatus.RETURNED to setOf(OrderStatus.REFUNDED),
        OrderStatus.CANCELLED to emptySet(),
        OrderStatus.REFUNDED to emptySet()
    )

    // ---------------------------------------------------------------
    // Checkout
    // ---------------------------------------------------------------

    /**
     * Core concurrency guarantee (spec section 20): every item's stock is reserved via
     * InventoryService.reserveStock(), which takes a pessimistic row lock (SELECT ... FOR
     * UPDATE) on the Inventory row for the duration of THIS transaction. If two customers
     * check out the same stock=1 product at the same time, the second transaction blocks
     * until the first commits or rolls back, then re-reads the now-updated availableStock
     * and fails with InsufficientStockException — only one checkout can ever succeed.
     * Because everything here runs in a single @Transactional method, a failure on any
     * item (e.g. the 3rd of 3) rolls back the reservations already made for earlier items.
     */
    @Transactional
    fun checkout(userId: UUID, request: CheckoutRequest): OrderResponse {
        val cart = getOrCreateCart(userId)
        val cartItems = cartItemRepository.findByCartId(cart.id!!)
        if (cartItems.isEmpty()) {
            throw BadRequestException("Cannot checkout with an empty cart")
        }

        // Reserve stock for every line item up front — any failure rolls back the whole checkout.
        cartItems.forEach { inventoryService.reserveStock(it.product.id!!, it.quantity) }

        val subtotal = cartItems.fold(BigDecimal.ZERO) { acc, item ->
            acc.add(item.product.price.multiply(BigDecimal(item.quantity)))
        }

        var shippingFee = calculateShippingFee(subtotal)
        var discountAmount = BigDecimal.ZERO
        var appliedCoupon: Coupon? = null

        if (!request.couponCode.isNullOrBlank()) {
            val coupon = couponRepository.findByCode(request.couponCode.trim().uppercase())
                .orElseThrow { ResourceNotFoundException("Coupon not found: ${request.couponCode}") }
            validateCoupon(coupon, userId, subtotal)
            discountAmount = calculateDiscount(coupon, subtotal)
            if (coupon.discountType == DiscountType.FREE_SHIPPING) {
                shippingFee = BigDecimal.ZERO
            }
            appliedCoupon = coupon
        }

        var totalAmount = subtotal.subtract(discountAmount).add(shippingFee)
        if (totalAmount < BigDecimal.ZERO) totalAmount = BigDecimal.ZERO

        val customer = cart.user
        val order = Order(
            orderNumber = generateOrderNumber(),
            customer = customer,
            status = OrderStatus.PENDING,
            recipientName = request.recipientName,
            recipientPhone = request.recipientPhone,
            shippingAddressLine = request.shippingAddressLine,
            shippingWard = request.shippingWard,
            shippingDistrict = request.shippingDistrict,
            shippingProvince = request.shippingProvince,
            note = request.note,
            subtotal = subtotal,
            shippingFee = shippingFee,
            discountAmount = discountAmount,
            totalAmount = totalAmount,
            couponCode = appliedCoupon?.code,
            paymentMethod = request.paymentMethod,
            paymentStatus = PaymentStatus.PENDING
        )
        orderRepository.save(order)

        val orderItems = cartItems.map { cartItem ->
            OrderItem(
                order = order,
                product = cartItem.product,
                productNameSnapshot = cartItem.product.name,
                conditionSnapshot = cartItem.product.condition.name,
                unitPrice = cartItem.product.price,
                quantity = cartItem.quantity,
                subtotal = cartItem.product.price.multiply(BigDecimal(cartItem.quantity))
            )
        }
        orderItemRepository.saveAll(orderItems)

        orderStatusHistoryRepository.save(
            OrderStatusHistory(order = order, status = OrderStatus.PENDING, note = "Order created", changedBy = customer)
        )

        paymentRepository.save(
            Payment(order = order, method = request.paymentMethod, status = PaymentStatus.PENDING, amount = totalAmount)
        )
        shipmentRepository.save(Shipment(order = order))

        if (appliedCoupon != null) {
            appliedCoupon.currentUsage += 1
            couponRepository.save(appliedCoupon)
            couponUsageRepository.save(
                CouponUsage(coupon = appliedCoupon, user = customer, order = order, discountApplied = discountAmount)
            )
        }

        // Checkout succeeded — the cart is now consumed.
        cartItems.forEach { cartItemRepository.delete(it) }

        return orderMapper.toResponse(order, orderItems)
    }

    // ---------------------------------------------------------------
    // Read
    // ---------------------------------------------------------------

    fun getMyOrders(userId: UUID, page: Int, size: Int): Page<OrderSummaryResponse> {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        return orderRepository.findByCustomerId(userId, pageable).map { order ->
            orderMapper.toSummaryResponse(order, orderItemRepository.findByOrderId(order.id!!).size)
        }
    }

    fun getForManagement(status: OrderStatus?, page: Int, size: Int): Page<OrderSummaryResponse> {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        val orders = if (status != null) {
            orderRepository.findByStatus(status, pageable)
        } else {
            orderRepository.findAll(pageable)
        }
        return orders.map { order -> orderMapper.toSummaryResponse(order, orderItemRepository.findByOrderId(order.id!!).size) }
    }

    fun getById(orderId: UUID, requester: User): OrderResponse {
        val order = findOrderGuarded(orderId, requester)
        val items = orderItemRepository.findByOrderId(orderId)
        return orderMapper.toResponse(order, items)
    }

    fun getTimeline(orderId: UUID, requester: User): List<OrderStatusHistoryResponse> {
        val order = findOrderGuarded(orderId, requester)
        return orderStatusHistoryRepository.findByOrderIdOrderByCreatedAtAsc(order.id!!)
            .map(orderMapper::toHistoryResponse)
    }

    // ---------------------------------------------------------------
    // Status transitions
    // ---------------------------------------------------------------

    /** STAFF/ADMIN only — advances an order through the workflow. */
    @Transactional
    fun updateStatus(orderId: UUID, request: OrderStatusUpdateRequest, actingUser: User): OrderResponse {
        val order = orderRepository.findById(orderId)
            .orElseThrow { ResourceNotFoundException("Order not found with id: $orderId") }
        applyTransition(order, request.status, request.note, actingUser)
        val items = orderItemRepository.findByOrderId(orderId)
        return orderMapper.toResponse(order, items)
    }

    /** Customers may cancel their own PENDING/CONFIRMED order; STAFF/ADMIN may cancel any. */
    @Transactional
    fun cancelMyOrder(orderId: UUID, requester: User): OrderResponse {
        val order = findOrderGuarded(orderId, requester)
        applyTransition(order, OrderStatus.CANCELLED, "Cancelled by customer", requester)
        val items = orderItemRepository.findByOrderId(orderId)
        return orderMapper.toResponse(order, items)
    }

    /**
     * Called by PaymentService when a Stripe webhook reports a successful charge.
     * This is the only direction of coupling between Order and Payment: PaymentService
     * depends on OrderService, never the other way around, which avoids a circular
     * Spring bean dependency (OrderService already needs PaymentRepository directly
     * for the COD auto-paid-on-delivery case in applyTransition).
     */
    @Transactional
    fun markPaymentSucceeded(orderId: UUID) {
        val order = orderRepository.findById(orderId)
            .orElseThrow { ResourceNotFoundException("Order not found with id: $orderId") }
        order.paymentStatus = PaymentStatus.SUCCEEDED
        orderRepository.save(order)
        if (order.status == OrderStatus.PENDING) {
            applyTransition(order, OrderStatus.CONFIRMED, "Payment confirmed via Stripe", null)
        }
    }

    /** Called by PaymentService when a Stripe webhook reports a failed charge. */
    @Transactional
    fun markPaymentFailed(orderId: UUID) {
        val order = orderRepository.findById(orderId)
            .orElseThrow { ResourceNotFoundException("Order not found with id: $orderId") }
        order.paymentStatus = PaymentStatus.FAILED
        orderRepository.save(order)
        // Deliberately no automatic status transition — staff decides whether to retry
        // payment or cancel the order (see PATCH /api/orders/{id}/status).
    }

    private fun applyTransition(order: Order, newStatus: OrderStatus, note: String?, actingUser: User?) {
        val allowed = allowedTransitions[order.status].orEmpty()
        if (newStatus !in allowed) {
            throw InvalidOrderStateException("Cannot move order from ${order.status} to $newStatus")
        }

        val items = orderItemRepository.findByOrderId(order.id!!)
        val previousStatus = order.status

        when (newStatus) {
            OrderStatus.CONFIRMED -> items.forEach { item ->
                item.product?.let { product -> inventoryService.confirmSale(product.id!!, item.quantity, order.id!!, actingUser) }
            }
            OrderStatus.CANCELLED -> items.forEach { item ->
                item.product?.let { product ->
                    if (previousStatus == OrderStatus.PENDING) {
                        inventoryService.releaseStock(product.id!!, item.quantity)
                    } else {
                        inventoryService.returnSoldStock(product.id!!, item.quantity, order.id, actingUser)
                    }
                }
            }
            OrderStatus.RETURNED -> items.forEach { item ->
                item.product?.let { product -> inventoryService.returnSoldStock(product.id!!, item.quantity, order.id, actingUser) }
            }
            OrderStatus.DELIVERED -> {
                // COD is only ever "collected" at the doorstep — mark it paid the moment
                // delivery is confirmed. STRIPE orders are marked paid earlier, via the
                // webhook calling markPaymentSucceeded() once the charge succeeds.
                if (order.paymentMethod == PaymentMethod.COD) {
                    order.paymentStatus = PaymentStatus.SUCCEEDED
                    paymentRepository.findByOrderId(order.id!!).ifPresent { payment ->
                        payment.status = PaymentStatus.SUCCEEDED
                        payment.paidAt = Instant.now()
                        paymentRepository.save(payment)
                    }
                }
            }
            else -> { /* No inventory/payment side-effect for other transitions. */ }
        }

        order.status = newStatus
        orderRepository.save(order)
        orderStatusHistoryRepository.save(
            OrderStatusHistory(order = order, status = newStatus, note = note, changedBy = actingUser)
        )
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    /** Self-healing: creates the Cart on first access if one doesn't exist yet (mirrors CartService). */
    private fun getOrCreateCart(userId: UUID): Cart =
        cartRepository.findByUserId(userId).orElseGet {
            val user = userRepository.findById(userId)
                .orElseThrow { ResourceNotFoundException("User not found with id: $userId") }
            cartRepository.save(Cart(user = user))
        }

    /** Customers may only access their own orders; STAFF/ADMIN may access any. Hides existence via 404 otherwise. */
    private fun findOrderGuarded(orderId: UUID, requester: User): Order {
        val order = orderRepository.findById(orderId)
            .orElseThrow { ResourceNotFoundException("Order not found with id: $orderId") }
        val isOwner = order.customer.id == requester.id
        val isStaffOrAdmin = requester.role == RoleName.STAFF || requester.role == RoleName.ADMIN
        if (!isOwner && !isStaffOrAdmin) {
            throw ResourceNotFoundException("Order not found with id: $orderId")
        }
        return order
    }

    private fun calculateShippingFee(subtotal: BigDecimal): BigDecimal =
        if (subtotal >= freeShippingThreshold) BigDecimal.ZERO else flatShippingFee

    private fun validateCoupon(coupon: Coupon, userId: UUID, subtotal: BigDecimal) {
        val now = Instant.now()
        if (!coupon.isActive) throw BadRequestException("This coupon is no longer active")
        if (now.isBefore(coupon.startDate) || now.isAfter(coupon.endDate)) {
            throw BadRequestException("This coupon is not valid at this time")
        }
        coupon.minOrderValue?.let {
            if (subtotal < it) throw BadRequestException("Minimum order value for this coupon is $it")
        }
        coupon.maxUsage?.let {
            if (coupon.currentUsage >= it) throw BadRequestException("This coupon has reached its usage limit")
        }
        coupon.perUserLimit?.let {
            if (couponUsageRepository.countByCouponIdAndUserId(coupon.id!!, userId) >= it) {
                throw BadRequestException("You have already used this coupon the maximum number of times")
            }
        }
    }

    private fun calculateDiscount(coupon: Coupon, subtotal: BigDecimal): BigDecimal =
        when (coupon.discountType) {
            DiscountType.PERCENTAGE -> {
                val raw = subtotal.multiply(coupon.discountValue ?: BigDecimal.ZERO)
                    .divide(BigDecimal(100), 2, RoundingMode.HALF_UP)
                coupon.maxDiscountAmount?.let { cap -> raw.min(cap) } ?: raw
            }
            DiscountType.FIXED_AMOUNT -> (coupon.discountValue ?: BigDecimal.ZERO).min(subtotal)
            DiscountType.FREE_SHIPPING -> BigDecimal.ZERO
        }

    private fun generateOrderNumber(): String {
        val datePart = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"))
        val randomPart = UUID.randomUUID().toString().take(8).uppercase()
        return "ORD-$datePart-$randomPart"
    }
}
