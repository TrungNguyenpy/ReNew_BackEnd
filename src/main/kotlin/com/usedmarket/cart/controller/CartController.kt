package com.usedmarket.cart.controller

import com.usedmarket.cart.dto.AddCartItemRequest
import com.usedmarket.cart.dto.ApplyCouponRequest
import com.usedmarket.cart.dto.CartResponse
import com.usedmarket.cart.dto.UpdateCartItemRequest
import com.usedmarket.cart.service.CartService
import com.usedmarket.security.CustomUserDetails
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

/**
 * No @PreAuthorize role checks needed here beyond plain authentication:
 * every method scopes strictly to principal.user.id, so a customer can only
 * ever read/modify their own cart — there is no "other user's cart" endpoint
 * to guard against.
 */
@RestController
@RequestMapping("/api/cart")
class CartController(
    private val cartService: CartService
) {

    @GetMapping
    fun getCart(@AuthenticationPrincipal principal: CustomUserDetails): CartResponse =
        cartService.getCart(principal.user.id!!)

    @PostMapping("/items")
    fun addItem(
        @Valid @RequestBody request: AddCartItemRequest,
        @AuthenticationPrincipal principal: CustomUserDetails
    ): CartResponse = cartService.addItem(principal.user.id!!, request)

    @PutMapping("/items/{productId}")
    fun updateItemQuantity(
        @PathVariable productId: UUID,
        @Valid @RequestBody request: UpdateCartItemRequest,
        @AuthenticationPrincipal principal: CustomUserDetails
    ): CartResponse = cartService.updateItemQuantity(principal.user.id!!, productId, request)

    @DeleteMapping("/items/{productId}")
    fun removeItem(
        @PathVariable productId: UUID,
        @AuthenticationPrincipal principal: CustomUserDetails
    ): CartResponse = cartService.removeItem(principal.user.id!!, productId)

    @DeleteMapping
    fun clearCart(@AuthenticationPrincipal principal: CustomUserDetails): ResponseEntity<Void> {
        cartService.clearCart(principal.user.id!!)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/coupon")
    fun previewCoupon(
        @Valid @RequestBody request: ApplyCouponRequest,
        @AuthenticationPrincipal principal: CustomUserDetails
    ): CartResponse = cartService.previewCoupon(principal.user.id!!, request)
}
