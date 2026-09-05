package com.usedmarket.auth.controller

import com.usedmarket.auth.dto.AuthResponse
import com.usedmarket.auth.dto.ForgotPasswordRequest
import com.usedmarket.auth.dto.LoginRequest
import com.usedmarket.auth.dto.RefreshTokenRequest
import com.usedmarket.auth.dto.RegisterRequest
import com.usedmarket.auth.dto.ResetPasswordRequest
import com.usedmarket.auth.service.AuthService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authService: AuthService
) {

    @PostMapping("/register")
    fun register(@Valid @RequestBody request: RegisterRequest): ResponseEntity<AuthResponse> =
        ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request))

    @PostMapping("/login")
    fun login(@Valid @RequestBody request: LoginRequest): ResponseEntity<AuthResponse> =
        ResponseEntity.ok(authService.login(request))

    @PostMapping("/refresh")
    fun refresh(@Valid @RequestBody request: RefreshTokenRequest): ResponseEntity<AuthResponse> =
        ResponseEntity.ok(authService.refresh(request))

    @PostMapping("/logout")
    fun logout(@Valid @RequestBody request: RefreshTokenRequest): ResponseEntity<Void> {
        authService.logout(request)
        return ResponseEntity.noContent().build()
    }

    @PostMapping("/forgot-password")
    fun forgotPassword(@Valid @RequestBody request: ForgotPasswordRequest): ResponseEntity<Void> {
        authService.forgotPassword(request)
        return ResponseEntity.ok().build()
    }

    @PostMapping("/reset-password")
    fun resetPassword(@Valid @RequestBody request: ResetPasswordRequest): ResponseEntity<Void> {
        authService.resetPassword(request)
        return ResponseEntity.ok().build()
    }
}
