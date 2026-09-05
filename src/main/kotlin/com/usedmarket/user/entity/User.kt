package com.usedmarket.user.entity

import com.usedmarket.common.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table

@Entity
@Table(
    name = "users",
    uniqueConstraints = [
        jakarta.persistence.UniqueConstraint(name = "uk_users_email", columnNames = ["email"])
    ]
)
class User(

    @Column(nullable = false, unique = true, length = 255)
    var email: String,

    /** BCrypt-hashed password. Never store plaintext. */
    @Column(name = "password_hash", nullable = false)
    var passwordHash: String,

    @Column(name = "full_name", nullable = false, length = 150)
    var fullName: String,

    @Column(length = 20)
    var phone: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    var role: RoleName = RoleName.CUSTOMER,

    /** Soft-disable a user account without deleting it (e.g. banned, self-deactivated). */
    @Column(nullable = false)
    var enabled: Boolean = true,

    /** Set to true once the user has verified their email address. */
    @Column(name = "email_verified", nullable = false)
    var emailVerified: Boolean = false,

    @Column(name = "avatar_url")
    var avatarUrl: String? = null

) : BaseEntity()
