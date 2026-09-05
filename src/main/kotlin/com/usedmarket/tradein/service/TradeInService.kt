package com.usedmarket.tradein.service

import com.usedmarket.catalog.repository.CategoryRepository
import com.usedmarket.common.exception.BadRequestException
import com.usedmarket.common.exception.ForbiddenException
import com.usedmarket.common.exception.InvalidOrderStateException
import com.usedmarket.common.exception.ResourceNotFoundException
import com.usedmarket.media.CloudinaryService
import com.usedmarket.tradein.dto.TradeInCreateRequest
import com.usedmarket.tradein.dto.TradeInOfferRequest
import com.usedmarket.tradein.dto.TradeInResponse
import com.usedmarket.tradein.dto.TradeInStatusUpdateRequest
import com.usedmarket.tradein.entity.MediaType
import com.usedmarket.tradein.entity.TradeInItem
import com.usedmarket.tradein.entity.TradeInRequest
import com.usedmarket.tradein.entity.TradeInStatus
import com.usedmarket.tradein.mapper.TradeInMapper
import com.usedmarket.tradein.repository.TradeInItemRepository
import com.usedmarket.tradein.repository.TradeInRequestRepository
import com.usedmarket.user.entity.RoleName
import com.usedmarket.user.entity.User
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.util.UUID

@Service
class TradeInService(
    private val tradeInRequestRepository: TradeInRequestRepository,
    private val tradeInItemRepository: TradeInItemRepository,
    private val categoryRepository: CategoryRepository,
    private val cloudinaryService: CloudinaryService,
    private val tradeInMapper: TradeInMapper
) {

    private val allowedTransitions: Map<TradeInStatus, Set<TradeInStatus>> = mapOf(
        TradeInStatus.PENDING to setOf(TradeInStatus.INSPECTING, TradeInStatus.REJECTED),
        TradeInStatus.INSPECTING to setOf(TradeInStatus.OFFERED, TradeInStatus.REJECTED),
        TradeInStatus.OFFERED to setOf(TradeInStatus.CUSTOMER_ACCEPTED, TradeInStatus.REJECTED),
        TradeInStatus.CUSTOMER_ACCEPTED to setOf(TradeInStatus.PURCHASED, TradeInStatus.REJECTED),
        TradeInStatus.PURCHASED to emptySet(),
        TradeInStatus.REJECTED to emptySet()
    )

    // ---------------------------------------------------------------
    // Create / read
    // ---------------------------------------------------------------

    @Transactional
    fun create(request: TradeInCreateRequest, customer: User): TradeInResponse {
        val category = request.categoryId?.let {
            categoryRepository.findById(it)
                .orElseThrow { ResourceNotFoundException("Category not found with id: $it") }
        }

        val tradeIn = TradeInRequest(
            customer = customer,
            productName = request.productName,
            brand = request.brand,
            model = request.model,
            category = category,
            purchaseYear = request.purchaseYear,
            usageDuration = request.usageDuration,
            condition = request.condition,
            description = request.description,
            expectedPrice = request.expectedPrice,
            contactPhone = request.contactPhone,
            contactEmail = request.contactEmail,
            status = TradeInStatus.PENDING
        )
        tradeInRequestRepository.save(tradeIn)

        return tradeInMapper.toResponse(tradeIn, emptyList())
    }

    fun getMyRequests(customerId: UUID, page: Int, size: Int): Page<TradeInResponse> {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        return tradeInRequestRepository.findByCustomerId(customerId, pageable).map { request ->
            tradeInMapper.toResponse(request, tradeInItemRepository.findByTradeInRequestIdOrderByDisplayOrderAsc(request.id!!))
        }
    }

    fun getForManagement(status: TradeInStatus?, page: Int, size: Int): Page<TradeInResponse> {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        val requests = if (status != null) {
            tradeInRequestRepository.findByStatus(status, pageable)
        } else {
            tradeInRequestRepository.findAll(pageable)
        }
        return requests.map { request ->
            tradeInMapper.toResponse(request, tradeInItemRepository.findByTradeInRequestIdOrderByDisplayOrderAsc(request.id!!))
        }
    }

    fun getById(id: UUID, requester: User): TradeInResponse {
        val tradeIn = findGuarded(id, requester)
        val items = tradeInItemRepository.findByTradeInRequestIdOrderByDisplayOrderAsc(id)
        return tradeInMapper.toResponse(tradeIn, items)
    }

    // ---------------------------------------------------------------
    // Attachments (owner only, while still negotiable)
    // ---------------------------------------------------------------

    @Transactional
    fun addItem(id: UUID, file: MultipartFile, mediaType: MediaType, requester: User): TradeInResponse {
        val tradeIn = findGuarded(id, requester)
        if (tradeIn.customer.id != requester.id) {
            throw ForbiddenException("You can only attach media to your own trade-in request")
        }
        if (tradeIn.status !in setOf(TradeInStatus.PENDING, TradeInStatus.INSPECTING)) {
            throw BadRequestException("Cannot add media once the request has moved past inspection")
        }

        val folder = "trade-ins/$id"
        val uploadResult = cloudinaryService.upload(file, folder)
        tradeInItemRepository.save(
            TradeInItem(
                tradeInRequest = tradeIn,
                mediaType = mediaType,
                mediaUrl = uploadResult.url,
                cloudinaryPublicId = uploadResult.publicId
            )
        )

        val items = tradeInItemRepository.findByTradeInRequestIdOrderByDisplayOrderAsc(id)
        return tradeInMapper.toResponse(tradeIn, items)
    }

    // ---------------------------------------------------------------
    // Staff workflow
    // ---------------------------------------------------------------

    /** Generic staff transition — only for PENDING→INSPECTING or any state→REJECTED. */
    @Transactional
    fun updateStatus(id: UUID, request: TradeInStatusUpdateRequest, staffUser: User): TradeInResponse {
        if (request.status !in setOf(TradeInStatus.INSPECTING, TradeInStatus.REJECTED)) {
            throw BadRequestException(
                "Use the dedicated offer/accept/decline/complete endpoints for this transition"
            )
        }
        val tradeIn = tradeInRequestRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("Trade-in request not found with id: $id") }
        applyTransition(tradeIn, request.status, staffUser, request.inspectionNote)

        val items = tradeInItemRepository.findByTradeInRequestIdOrderByDisplayOrderAsc(id)
        return tradeInMapper.toResponse(tradeIn, items)
    }

    /** STAFF/ADMIN sets a price after inspecting the item — INSPECTING → OFFERED. */
    @Transactional
    fun makeOffer(id: UUID, request: TradeInOfferRequest, staffUser: User): TradeInResponse {
        val tradeIn = tradeInRequestRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("Trade-in request not found with id: $id") }

        tradeIn.offeredPrice = request.offeredPrice
        applyTransition(tradeIn, TradeInStatus.OFFERED, staffUser, request.inspectionNote)

        val items = tradeInItemRepository.findByTradeInRequestIdOrderByDisplayOrderAsc(id)
        return tradeInMapper.toResponse(tradeIn, items)
    }

    /** Customer accepts the staff's offer — OFFERED → CUSTOMER_ACCEPTED. */
    @Transactional
    fun accept(id: UUID, customer: User): TradeInResponse {
        val tradeIn = findGuarded(id, customer)
        if (tradeIn.customer.id != customer.id) {
            throw ForbiddenException("This trade-in request does not belong to you")
        }
        applyTransition(tradeIn, TradeInStatus.CUSTOMER_ACCEPTED, customer, note = null)

        val items = tradeInItemRepository.findByTradeInRequestIdOrderByDisplayOrderAsc(id)
        return tradeInMapper.toResponse(tradeIn, items)
    }

    /** Customer declines the staff's offer — OFFERED → REJECTED. */
    @Transactional
    fun decline(id: UUID, customer: User): TradeInResponse {
        val tradeIn = findGuarded(id, customer)
        if (tradeIn.customer.id != customer.id) {
            throw ForbiddenException("This trade-in request does not belong to you")
        }
        applyTransition(tradeIn, TradeInStatus.REJECTED, customer, note = "Declined by customer")

        val items = tradeInItemRepository.findByTradeInRequestIdOrderByDisplayOrderAsc(id)
        return tradeInMapper.toResponse(tradeIn, items)
    }

    /**
     * STAFF/ADMIN confirms the item has been physically received and paid for —
     * CUSTOMER_ACCEPTED → PURCHASED. This is the final step of the trade-in itself;
     * turning the item into a sellable listing is a separate, deliberate action by
     * staff via the existing product-creation API (Phase 4), not automated here.
     */
    @Transactional
    fun complete(id: UUID, staffUser: User): TradeInResponse {
        val tradeIn = tradeInRequestRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("Trade-in request not found with id: $id") }
        applyTransition(tradeIn, TradeInStatus.PURCHASED, staffUser, note = null)

        val items = tradeInItemRepository.findByTradeInRequestIdOrderByDisplayOrderAsc(id)
        return tradeInMapper.toResponse(tradeIn, items)
    }

    // ---------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------

    private fun applyTransition(tradeIn: TradeInRequest, newStatus: TradeInStatus, actingUser: User, note: String?) {
        val allowed = allowedTransitions[tradeIn.status].orEmpty()
        if (newStatus !in allowed) {
            throw InvalidOrderStateException("Cannot move trade-in request from ${tradeIn.status} to $newStatus")
        }
        tradeIn.status = newStatus
        if (note != null) tradeIn.inspectionNote = note
        if (actingUser.role == RoleName.STAFF || actingUser.role == RoleName.ADMIN) {
            tradeIn.inspectedBy = actingUser
        }
        tradeInRequestRepository.save(tradeIn)
    }

    /** Customers may only access their own trade-in requests; STAFF/ADMIN may access any. */
    private fun findGuarded(id: UUID, requester: User): TradeInRequest {
        val tradeIn = tradeInRequestRepository.findById(id)
            .orElseThrow { ResourceNotFoundException("Trade-in request not found with id: $id") }
        val isOwner = tradeIn.customer.id == requester.id
        val isStaffOrAdmin = requester.role == RoleName.STAFF || requester.role == RoleName.ADMIN
        if (!isOwner && !isStaffOrAdmin) {
            throw ResourceNotFoundException("Trade-in request not found with id: $id")
        }
        return tradeIn
    }
}
