package com.usedmarket.security

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

/**
 * Once this SecurityFilterChain bean exists, Spring Boot's auto-configured
 * "generated password" security (the random UUID printed to console at
 * startup in Phase 1/2) is fully replaced by JWT-based authentication below.
 */
@Configuration
@EnableMethodSecurity
class SecurityConfig(
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
    private val restAuthenticationEntryPoint: RestAuthenticationEntryPoint,
    private val restAccessDeniedHandler: RestAccessDeniedHandler,
    @Value("\${app.cors.allowed-origins}") private val allowedOrigins: String
) {

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun authenticationManager(config: AuthenticationConfiguration): AuthenticationManager =
        config.authenticationManager

    @Bean
    fun securityFilterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .cors { it.configurationSource(corsConfigurationSource()) }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .exceptionHandling { ex ->
                ex.authenticationEntryPoint(restAuthenticationEntryPoint)
                ex.accessDeniedHandler(restAccessDeniedHandler)
            }
            .authorizeHttpRequests { auth ->
                auth
                    // Public: auth endpoints, health check
                    .requestMatchers("/api/auth/**", "/api/health", "/api/webhooks/**").permitAll()
                    // Public: browsing the catalog does not require login
                    .requestMatchers(
                        HttpMethod.GET,
                        "/api/products/**", "/api/categories/**", "/api/brands/**"
                    ).permitAll()
                    // Admin-only management endpoints
                    .requestMatchers("/api/admin/**").hasRole("ADMIN")
                    // Staff + Admin operational endpoints
                    .requestMatchers("/api/staff/**").hasAnyRole("STAFF", "ADMIN")
                    // Everything else requires a valid access token
                    .anyRequest().authenticated()
            }
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)

        return http.build()
    }

    private fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration().apply {
            allowedOrigins = this@SecurityConfig.allowedOrigins.split(",").map { it.trim() }
            allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
            allowedHeaders = listOf("*")
            allowCredentials = true
        }
        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }
}
