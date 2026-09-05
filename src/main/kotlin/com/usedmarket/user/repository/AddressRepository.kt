package com.usedmarket.user.repository

import com.usedmarket.user.entity.Address
import org.springframework.data.jpa.repository.JpaRepository
import java.util.UUID

interface AddressRepository : JpaRepository<Address, UUID> {

    fun findByUserId(userId: UUID): List<Address>

    fun findByUserIdAndIsDefaultTrue(userId: UUID): Address?
}
