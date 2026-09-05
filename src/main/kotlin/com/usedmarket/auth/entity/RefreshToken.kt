package com.usedmarket.auth.entity

import com.usedmarket.common.entity.BaseEntity
import com.usedmarket.user.entity.User
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import java.time.Instant

/**
 * Server-side record of an issued refresh token. The raw token is never
 * stored — only its SHA-256 hash — so a database leak does not expose
 * usable tokens. Storing it (rather than using a self-contained JWT refresh
 * token) is what makes logout / reset-password revocation possible.
 */
@Entity
@Table(name = "refresh_tokens")
class RefreshToken(

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User,

    @Column(name = "token_hash", nullable = false, unique = true, length = 128)
    var tokenHash: String,

    @Column(name = "expires_at", nullable = false)
    var expiresAt: Instant,

    @Column(nullable = false)
    var revoked: Boolean = false

) : BaseEntity()
