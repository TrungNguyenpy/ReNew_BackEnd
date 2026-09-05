package com.usedmarket.security

import com.fasterxml.jackson.databind.ObjectMapper
import com.usedmarket.common.exception.ErrorResponse
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.AuthenticationEntryPoint
import org.springframework.stereotype.Component

/**
 * By default, when no httpBasic()/formLogin() is configured, Spring Security
 * falls back to returning 403 for unauthenticated requests, which is
 * incorrect REST semantics (401 = not authenticated, 403 = authenticated but
 * not authorized). This entry point fixes that for the whole API.
 */
@Component
class RestAuthenticationEntryPoint(
    private val objectMapper: ObjectMapper
) : AuthenticationEntryPoint {

    override fun commence(
        request: HttpServletRequest,
        response: HttpServletResponse,
        authException: AuthenticationException
    ) {
        response.status = HttpStatus.UNAUTHORIZED.value()
        response.contentType = MediaType.APPLICATION_JSON_VALUE
        val body = ErrorResponse(
            status = HttpStatus.UNAUTHORIZED.value(),
            message = "Authentication required"
        )
        response.writer.write(objectMapper.writeValueAsString(body))
    }
}
