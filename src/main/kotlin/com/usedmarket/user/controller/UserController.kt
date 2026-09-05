package com.usedmarket.user.controller

import com.usedmarket.security.CustomUserDetails
import com.usedmarket.user.dto.UserProfileResponse
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/users")
class UserController {

    /**
     * Returns the currently authenticated user's profile.
     * Falls under SecurityConfig's `anyRequest().authenticated()` rule, so a
     * valid access token is required — this is the first real endpoint that
     * exercises the full JWT authentication pipeline end-to-end.
     */
    @GetMapping("/me")
    fun getCurrentUser(@AuthenticationPrincipal principal: CustomUserDetails): UserProfileResponse {
        val user = principal.user
        return UserProfileResponse(
            id = user.id!!,
            email = user.email,
            fullName = user.fullName,
            phone = user.phone,
            role = user.role.name,
            avatarUrl = user.avatarUrl,
            emailVerified = user.emailVerified
        )
    }
}
