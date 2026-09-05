package com.usedmarket.auth.repository

import com.usedmarket.auth.entity.PasswordResetToken
import org.springframework.data.jpa.repository.JpaRepository
import java.util.Optional
import java.util.UUID

interface PasswordResetTokenRepository : JpaRepository<PasswordResetToken, UUID> {

    fun findByTokenHash(tokenHash: String): Optional<PasswordResetToken>
}
