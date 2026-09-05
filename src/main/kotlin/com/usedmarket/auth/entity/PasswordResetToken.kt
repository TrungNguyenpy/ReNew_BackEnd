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

@Entity
@Table(name = "password_reset_tokens")
class PasswordResetToken(

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    var user: User,

    @Column(name = "token_hash", nullable = false, unique = true, length = 128)
    var tokenHash: String,

    @Column(name = "expires_at", nullable = false)
    var expiresAt: Instant,

    @Column(nullable = false)
    var used: Boolean = false

) : BaseEntity()
