package com.usedmarket.auth.service

import com.usedmarket.auth.dto.AuthResponse
import com.usedmarket.auth.dto.ForgotPasswordRequest
import com.usedmarket.auth.dto.LoginRequest
import com.usedmarket.auth.dto.RefreshTokenRequest
import com.usedmarket.auth.dto.RegisterRequest
import com.usedmarket.auth.dto.ResetPasswordRequest
import com.usedmarket.auth.dto.UserSummaryDto
import com.usedmarket.auth.entity.PasswordResetToken
import com.usedmarket.auth.entity.RefreshToken
import com.usedmarket.auth.repository.PasswordResetTokenRepository
import com.usedmarket.auth.repository.RefreshTokenRepository
import com.usedmarket.cart.entity.Cart
import com.usedmarket.cart.repository.CartRepository
import com.usedmarket.common.exception.BadRequestException
import com.usedmarket.common.exception.DuplicateResourceException
import com.usedmarket.common.exception.ForbiddenException
import com.usedmarket.common.exception.InvalidCredentialsException
import com.usedmarket.common.exception.UnauthorizedException
import com.usedmarket.security.JwtService
import com.usedmarket.user.entity.User
import com.usedmarket.user.repository.UserRepository
import com.usedmarket.wishlist.entity.Wishlist
import com.usedmarket.wishlist.repository.WishlistRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Base64

@Service
class AuthService(
    private val userRepository: UserRepository,
    private val cartRepository: CartRepository,
    private val wishlistRepository: WishlistRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val passwordResetTokenRepository: PasswordResetTokenRepository,
    private val passwordEncoder: PasswordEncoder,
    private val jwtService: JwtService,
    @Value("\${app.jwt.refresh-token-expiration-ms}") private val refreshTokenExpirationMs: Long
) {

    private val logger = LoggerFactory.getLogger(AuthService::class.java)
    private val secureRandom = SecureRandom()

    @Transactional
    fun register(request: RegisterRequest): AuthResponse {
        val normalizedEmail = request.email.trim().lowercase()
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw DuplicateResourceException("Email already registered")
        }

        val user = User(
            email = normalizedEmail,
            passwordHash = passwordEncoder.encode(request.password),
            fullName = request.fullName.trim(),
            phone = request.phone
        )
        userRepository.save(user)

        // Every customer gets an empty Cart and Wishlist ready to use immediately.
        cartRepository.save(Cart(user = user))
        wishlistRepository.save(Wishlist(user = user))

        return issueTokens(user)
    }

    @Transactional
    fun login(request: LoginRequest): AuthResponse {
        val normalizedEmail = request.email.trim().lowercase()
        val user = userRepository.findByEmail(normalizedEmail)
            .orElseThrow { InvalidCredentialsException() }

        if (!passwordEncoder.matches(request.password, user.passwordHash)) {
            throw InvalidCredentialsException()
        }
        if (!user.enabled) {
            throw ForbiddenException("This account has been disabled")
        }

        return issueTokens(user)
    }

    @Transactional
    fun refresh(request: RefreshTokenRequest): AuthResponse {
        val tokenHash = hashToken(request.refreshToken)
        val stored = refreshTokenRepository.findByTokenHash(tokenHash)
            .orElseThrow { UnauthorizedException("Invalid refresh token") }

        if (stored.revoked || stored.expiresAt.isBefore(Instant.now())) {
            throw UnauthorizedException("Refresh token expired or revoked")
        }

        // Rotate: revoke the used token and issue a brand new pair.
        stored.revoked = true
        refreshTokenRepository.save(stored)

        return issueTokens(stored.user)
    }

    @Transactional
    fun logout(request: RefreshTokenRequest) {
        val tokenHash = hashToken(request.refreshToken)
        refreshTokenRepository.findByTokenHash(tokenHash).ifPresent {
            it.revoked = true
            refreshTokenRepository.save(it)
        }
    }

    @Transactional
    fun forgotPassword(request: ForgotPasswordRequest) {
        val normalizedEmail = request.email.trim().lowercase()
        val userOptional = userRepository.findByEmail(normalizedEmail)

        // Deliberately do not reveal whether the email exists in the system.
        if (userOptional.isEmpty) return
        val user = userOptional.get()

        val rawToken = generateSecureToken()
        val resetToken = PasswordResetToken(
            user = user,
            tokenHash = hashToken(rawToken),
            expiresAt = Instant.now().plus(1, ChronoUnit.HOURS)
        )
        passwordResetTokenRepository.save(resetToken)

        // No SMTP/email provider is configured yet (see build.gradle.kts / application.yml) —
        // in production this raw token would be sent as a reset link via email.
        // For now it is logged so the flow can be exercised end-to-end during development.
        logger.info("Password reset token generated for {}: {}", user.email, rawToken)
    }

    @Transactional
    fun resetPassword(request: ResetPasswordRequest) {
        val tokenHash = hashToken(request.token)
        val resetToken = passwordResetTokenRepository.findByTokenHash(tokenHash)
            .orElseThrow { BadRequestException("Invalid or expired reset token") }

        if (resetToken.used || resetToken.expiresAt.isBefore(Instant.now())) {
            throw BadRequestException("Invalid or expired reset token")
        }

        val user = resetToken.user
        user.passwordHash = passwordEncoder.encode(request.newPassword)
        userRepository.save(user)

        resetToken.used = true
        passwordResetTokenRepository.save(resetToken)

        // Force re-login on every device after a password reset.
        refreshTokenRepository.revokeAllByUserId(user.id!!)
    }

    private fun issueTokens(user: User): AuthResponse {
        val accessToken = jwtService.generateAccessToken(user)

        val rawRefreshToken = generateSecureToken()
        val refreshTokenEntity = RefreshToken(
            user = user,
            tokenHash = hashToken(rawRefreshToken),
            expiresAt = Instant.now().plusMillis(refreshTokenExpirationMs)
        )
        refreshTokenRepository.save(refreshTokenEntity)

        return AuthResponse(
            accessToken = accessToken,
            refreshToken = rawRefreshToken,
            expiresIn = jwtService.getAccessTokenExpirationMs() / 1000,
            user = UserSummaryDto(
                id = user.id!!,
                email = user.email,
                fullName = user.fullName,
                role = user.role.name
            )
        )
    }

    private fun generateSecureToken(): String {
        val bytes = ByteArray(32)
        secureRandom.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private fun hashToken(token: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(token.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
}
