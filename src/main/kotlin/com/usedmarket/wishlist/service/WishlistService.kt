package com.usedmarket.wishlist.service

import com.usedmarket.common.exception.BadRequestException
import com.usedmarket.common.exception.DuplicateResourceException
import com.usedmarket.common.exception.ResourceNotFoundException
import com.usedmarket.inventory.repository.InventoryRepository
import com.usedmarket.product.repository.ProductImageRepository
import com.usedmarket.product.repository.ProductRepository
import com.usedmarket.user.repository.UserRepository
import com.usedmarket.wishlist.dto.WishlistResponse
import com.usedmarket.wishlist.entity.Wishlist
import com.usedmarket.wishlist.entity.WishlistItem
import com.usedmarket.wishlist.mapper.WishlistMapper
import com.usedmarket.wishlist.repository.WishlistItemRepository
import com.usedmarket.wishlist.repository.WishlistRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.UUID

@Service
class WishlistService(
    private val wishlistRepository: WishlistRepository,
    private val wishlistItemRepository: WishlistItemRepository,
    private val productRepository: ProductRepository,
    private val productImageRepository: ProductImageRepository,
    private val inventoryRepository: InventoryRepository,
    private val userRepository: UserRepository,
    private val wishlistMapper: WishlistMapper
) {

    fun getWishlist(userId: UUID): WishlistResponse {
        val wishlist = getOrCreateWishlist(userId)
        return buildResponse(wishlist)
    }

    @Transactional
    fun addItem(userId: UUID, productId: UUID): WishlistResponse {
        val wishlist = getOrCreateWishlist(userId)
        val product = productRepository.findById(productId)
            .orElseThrow { ResourceNotFoundException("Product not found with id: $productId") }

        if (!product.isActive || product.isHidden) {
            throw BadRequestException("This product is no longer available")
        }
        if (wishlistItemRepository.findByWishlistIdAndProductId(wishlist.id!!, productId).isPresent) {
            throw DuplicateResourceException("This product is already in your wishlist")
        }

        wishlistItemRepository.save(
            WishlistItem(wishlist = wishlist, product = product, priceAtAddTime = product.price)
        )
        return buildResponse(wishlist)
    }

    @Transactional
    fun removeItem(userId: UUID, productId: UUID): WishlistResponse {
        val wishlist = getOrCreateWishlist(userId)
        wishlistItemRepository.deleteByWishlistIdAndProductId(wishlist.id!!, productId)
        return buildResponse(wishlist)
    }

    private fun getOrCreateWishlist(userId: UUID): Wishlist =
        wishlistRepository.findByUserId(userId).orElseGet {
            val user = userRepository.findById(userId)
                .orElseThrow { ResourceNotFoundException("User not found with id: $userId") }
            wishlistRepository.save(Wishlist(user = user))
        }

    private fun buildResponse(wishlist: Wishlist): WishlistResponse {
        val items = wishlistItemRepository.findByWishlistId(wishlist.id!!).map { item ->
            val primaryImageUrl = productImageRepository.findByProductIdAndIsPrimaryTrue(item.product.id!!)?.imageUrl
            val availableStock = inventoryRepository.findByProductId(item.product.id!!)
                .map { it.availableStock }
                .orElse(item.product.stockQuantity)
            wishlistMapper.toItemResponse(item, primaryImageUrl, availableStock)
        }
        return wishlistMapper.toResponse(wishlist, items)
    }
}
