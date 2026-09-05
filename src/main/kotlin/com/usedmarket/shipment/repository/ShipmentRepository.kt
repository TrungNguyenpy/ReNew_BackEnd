package com.usedmarket.shipment.repository

import com.usedmarket.shipment.entity.Shipment
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional
import java.util.UUID

interface ShipmentRepository : JpaRepository<Shipment, UUID> {

    fun findByOrderId(orderId: UUID): Optional<Shipment>

    fun findByTrackingNumber(trackingNumber: String): Optional<Shipment>
}
