package com.usedmarket.security

import com.usedmarket.user.entity.User
import io.jsonwebtoken.Claims
import io.jsonwebtoken.JwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.nio.charset.StandardCharsets
import java.util.Date
import java.util.UUID

@Component
class JwtService(
    @Value("\${app.jwt.secret}") secret: String,
    @Value("\${app.jwt.access-token-expiration-ms}") private val accessTokenExpirationMs: Long
) {

    private val key = Keys.hmacShaKeyFor(secret.toByteArray(StandardCharsets.UTF_8))

    fun getAccessTokenExpirationMs(): Long = accessTokenExpirationMs

    fun generateAccessToken(user: User): String {
        val now = Date()
        val expiry = Date(now.time + accessTokenExpirationMs)
        return Jwts.builder()
            .subject(user.id.toString())
            .claim("email", user.email)
            .claim("role", user.role.name)
            .issuedAt(now)
            .expiration(expiry)
            .signWith(key)
            .compact()
    }

    /** Returns null (rather than throwing) when the token is missing, malformed, or expired. */
    fun parseClaimsOrNull(token: String): Claims? =
        try {
            Jwts.parser().verifyWith(key).build().parseSignedClaims(token).payload
        } catch (ex: JwtException) {
            null
        } catch (ex: IllegalArgumentException) {
            null
        }

    fun extractUserId(claims: Claims): UUID = UUID.fromString(claims.subject)

    fun extractEmail(claims: Claims): String = claims["email"] as String

    fun extractRole(claims: Claims): String = claims["role"] as String
}
