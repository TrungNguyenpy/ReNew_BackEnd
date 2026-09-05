package com.usedmarket.cart.service

import com.usedmarket.cart.dto.AddCartItemRequest
import com.usedmarket.cart.dto.ApplyCouponRequest
import com.usedmarket.cart.dto.CartResponse
import com.usedmarket.cart.dto.UpdateCartItemRequest
import com.usedmarket.cart.entity.Cart
import com.usedmarket.cart.entity.CartItem
import com.usedmarket.cart.mapper.CartMapper
import com.usedmarket.cart.repository.CartItemRepository
import com.usedmarket.cart.repository.CartRepository
import com.usedmarket.common.exception.BadRequestException
import com.usedmarket.common.exception.InsufficientStockException
import com.usedmarket.common.exception.ResourceNotFoundException
import com.usedmarket.coupon.entity.Coupon
import com.usedmarket.coupon.entity.DiscountType
import com.usedmarket.coupon.repository.CouponRepository
import com.usedmarket.coupon.repository.CouponUsageRepository
import com.usedmarket.inventory.repository.InventoryRepository
import com.usedmarket.product.repository.ProductImageRepository
import com.usedmarket.product.repository.ProductRepository
import com.usedmarket.user.repository.UserRepository
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.util.UUID

@Service
class CartService(
    private val cartRepository: CartRepository,
    private val cartItemRepository: CartItemRepository,
    private val productRepository: ProductRepository,
    private val productImageRepository: ProductImageRepository,
    private val inventoryRepository: InventoryRepository,
    private val couponRepository: CouponRepository,
    private val couponUsageRepository: CouponUsageRepository,
    private val userRepository: UserRepository,
    private val cartMapper: CartMapper,
    @Value("\${app.shipping.flat-fee}") private val flatShippingFee: BigDecimal,
    @Value("\${app.shipping.free-threshold}") private val freeShippingThreshold: BigDecimal
) {

    // ---------------------------------------------------------------
    // Read
    // ---------------------------------------------------------------

    fun getCart(userId: UUID): CartResponse = buildResponse(getOrCreateCart(userId), appliedCoupon = null)

    // ---------------------------------------------------------------
    // Write
    // ---------------------------------------------------------------

    @Transactional
    fun addItem(userId: UUID, request: AddCartItemRequest): CartResponse {
        val cart = getOrCreateCart(userId)
        val product = productRepository.findById(request.productId)
            .orElseThrow { ResourceNotFoundException("Product not found with id: ${request.productId}") }

        if (!product.isActive || product.isHidden) {
            throw BadRequestException("This product is no longer available")
        }

        val availableStock = inventoryRepository.findByProductId(product.id!!)
            .map { it.availableStock }
            .orElse(product.stockQuantity)

        val existing = cartItemRepository.findByCartIdAndProductId(cart.id!!, product.id!!)

        val newQuantity = existing.map { it.quantity + request.quantity }.orElse(request.quantity)
        if (newQuantity > availableStock) {
            throw InsufficientStockException("Only $availableStock unit(s) of this product are available")
        }

        if (existing.isPresent) {
            val item = existing.get()
            item.quantity = newQuantity
            cartItemRepository.save(item)
        } else {
            cartItemRepository.save(CartItem(cart = cart, product = product, quantity = request.quantity))
        }

        return buildResponse(cart, appliedCoupon = null)
    }

    @Transactional
    fun updateItemQuantity(userId: UUID, productId: UUID, request: UpdateCartItemRequest): CartResponse {
        val cart = getOrCreateCart(userId)
        val item = cartItemRepository.findByCartIdAndProductId(cart.id!!, productId)
            .orElseThrow { ResourceNotFoundException("This product is not in your cart") }

        val availableStock = inventoryRepository.findByProductId(productId)
            .map { it.availableStock }
            .orElse(item.product.stockQuantity)

        if (request.quantity > availableStock) {
            throw InsufficientStockException("Only $availableStock unit(s) of this product are available")
        }

        item.quantity = request.quantity
        cartItemRepository.save(item)

        return buildResponse(cart, appliedCoupon = null)
    }

    @Transactional
    fun removeItem(userId: UUID, productId: UUID): CartResponse {
        val cart = getOrCreateCart(userId)
        cartItemRepository.deleteByCartIdAndProductId(cart.id!!, productId)
        return buildResponse(cart, appliedCoupon = null)
    }

    @Transactional
    fun clearCart(userId: UUID) {
        val cart = getOrCreateCart(userId)
        cartItemRepository.findByCartId(cart.id!!).forEach { cartItemRepository.delete(it) }
    }

    /**
     * Validates a coupon code and returns the cart totals as they WOULD be if applied.
     * Deliberately does not create a CouponUsage row — actual redemption is committed
     * only when an Order is placed (Phase 8), so previewing a coupon never consumes
     * a customer's limited usage count.
     */
    fun previewCoupon(userId: UUID, request: ApplyCouponRequest): CartResponse {
        val cart = getOrCreateCart(userId)
        val coupon = couponRepository.findByCode(request.couponCode.trim().uppercase())
            .orElseThrow { ResourceNotFoundException("Coupon not found: ${request.couponCode}") }

        val subtotal = calculateSubtotal(cart)
        validateCoupon(coupon, userId, subtotal)

        return buildResponse(cart, appliedCoupon = coupon)
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    /** Self-healing: creates the Cart on first access if one doesn't exist yet (mirrors the Inventory backfill pattern from Phase 6). */
    private fun getOrCreateCart(userId: UUID): Cart =
        cartRepository.findByUserId(userId).orElseGet {
            val user = userRepository.findById(userId)
                .orElseThrow { ResourceNotFoundException("User not found with id: $userId") }
            cartRepository.save(Cart(user = user))
        }

    private fun calculateSubtotal(cart: Cart): BigDecimal {
        val items = cartItemRepository.findByCartId(cart.id!!)
        return items.fold(BigDecimal.ZERO) { acc, item -> acc.add(item.product.price.multiply(BigDecimal(item.quantity))) }
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

    private fun buildResponse(cart: Cart, appliedCoupon: Coupon?): CartResponse {
        val items = cartItemRepository.findByCartId(cart.id!!)
        val itemResponses = items.map { item ->
            val primaryImageUrl = productImageRepository.findByProductIdAndIsPrimaryTrue(item.product.id!!)?.imageUrl
            val availableStock = inventoryRepository.findByProductId(item.product.id!!)
                .map { it.availableStock }
                .orElse(item.product.stockQuantity)
            cartMapper.toItemResponse(item, primaryImageUrl, availableStock)
        }

        val subtotal = items.fold(BigDecimal.ZERO) { acc, item -> acc.add(item.product.price.multiply(BigDecimal(item.quantity))) }
        var shippingFee = calculateShippingFee(subtotal)
        var discountAmount = BigDecimal.ZERO

        if (appliedCoupon != null) {
            discountAmount = calculateDiscount(appliedCoupon, subtotal)
            if (appliedCoupon.discountType == DiscountType.FREE_SHIPPING) {
                shippingFee = BigDecimal.ZERO
            }
        }

        return cartMapper.toResponse(
            cart, itemResponses, subtotal, shippingFee, discountAmount,
            appliedCoupon?.code
        )
    }
}
