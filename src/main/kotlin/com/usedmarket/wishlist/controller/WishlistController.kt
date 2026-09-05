package com.usedmarket.wishlist.controller

import com.usedmarket.security.CustomUserDetails
import com.usedmarket.wishlist.dto.WishlistResponse
import com.usedmarket.wishlist.service.WishlistService
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.UUID

@RestController
@RequestMapping("/api/wishlist")
class WishlistController(
    private val wishlistService: WishlistService
) {

    @GetMapping
    fun getWishlist(@AuthenticationPrincipal principal: CustomUserDetails): WishlistResponse =
        wishlistService.getWishlist(principal.user.id!!)

    @PostMapping("/{productId}")
    fun addItem(
        @PathVariable productId: UUID,
        @AuthenticationPrincipal principal: CustomUserDetails
    ): WishlistResponse = wishlistService.addItem(principal.user.id!!, productId)

    @DeleteMapping("/{productId}")
    fun removeItem(
        @PathVariable productId: UUID,
        @AuthenticationPrincipal principal: CustomUserDetails
    ): WishlistResponse = wishlistService.removeItem(principal.user.id!!, productId)
}
