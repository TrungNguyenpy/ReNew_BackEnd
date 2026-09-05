package com.usedmarket.auth.repository

import com.usedmarket.auth.entity.RefreshToken
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.util.Optional
import java.util.UUID

interface RefreshTokenRepository : JpaRepository<RefreshToken, UUID> {

    fun findByTokenHash(tokenHash: String): Optional<RefreshToken>

    /** Invalidates every active session for a user — used after a password reset. */
    @Modifying
    @Query("UPDATE RefreshToken r SET r.revoked = true WHERE r.user.id = :userId AND r.revoked = false")
    fun revokeAllByUserId(@Param("userId") userId: UUID): Int
}
