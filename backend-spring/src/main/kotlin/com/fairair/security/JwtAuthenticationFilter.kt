package com.fairair.security

import org.slf4j.LoggerFactory
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

/**
 * JWT Authentication Web Filter for Spring WebFlux.
 * Validates JWT tokens and sets up the security context.
 */
@Component
class JwtAuthenticationFilter(
    private val jwtTokenProvider: JwtTokenProvider
) : WebFilter {

    private val log = LoggerFactory.getLogger(JwtAuthenticationFilter::class.java)

    companion object {
        private const val BEARER_PREFIX = "Bearer "
        
        /**
         * Paths that don't require authentication
         */
        private val PUBLIC_PATHS = setOf(
            "/health",
            "/health/live",
            "/health/ready",
            "/actuator/health",
            "/api/v1/auth/login",
            "/api/v1/auth/register",
            "/api/v1/auth/forgot-password",
            "/api/v1/auth/reset-password",
            "/api/v1/auth/refresh",
            "/api/v1/auth/logout",
            "/api/v1/config/stations",
            "/api/v1/config/routes",
            "/api/v1/config/route-map",
            "/api/v1/search",
            "/api/v1/search/low-fares",
            "/api/v1/booking",
            "/api/v1/checkin",
            "/api/v1/manage",
            "/api/v1/membership/plans",
            "/api/v1/seats",
            "/api/v1/meals",
            "/api/v1/chat/message"
        )
        
        /**
         * Path prefixes that don't require authentication
         */
        private val PUBLIC_PATH_PREFIXES = setOf(
            "/actuator/",
            "/api/v1/config/destinations/",
            "/api/v1/config/",
            "/api/v1/location/",
            "/api/v1/booking/",
            "/api/v1/checkin/",
            "/api/v1/manage/",
            "/api/v1/seats/",
            "/api/v1/meals/",
            "/api/v1/content/",
            // Admin endpoints (admin has its own auth)
            "/api/admin/",
            "/api/admin",
            // B2B endpoints (B2B has its own auth)
            "/api/b2b/",
            "/api/b2b",
            // Public content API
            "/api/content/",
            "/api/content",
            // Public group booking and charter requests
            "/api/group-bookings/",
            "/api/group-bookings",
            "/api/charter/",
            "/api/charter",
            // Pilot AI chat
            "/api/v1/chat/",
            "/api/v1/chat"
        )
        
        /**
         * Paths that are public but can have optional authentication
         */
        private val OPTIONAL_AUTH_PATHS = setOf(
            "/api/v1/booking"
        )
        
        /**
         * Path prefixes that require authentication
         */
        private val PROTECTED_PATH_PREFIXES = setOf(
            "/api/v1/booking/user"
        )
    }

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        val path = exchange.request.uri.path
        
        // Skip authentication for public paths
        if (isPublicPath(path)) {
            // For optional auth paths, still try to parse token if present
            if (isOptionalAuthPath(path)) {
                return tryOptionalAuth(exchange, chain)
            }
            return chain.filter(exchange)
        }

        val authHeader = exchange.request.headers.getFirst(HttpHeaders.AUTHORIZATION)
        
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            log.debug("No valid Authorization header for path: $path")
            return handleUnauthorized(exchange, "Missing or invalid Authorization header")
        }

        val token = authHeader.substring(BEARER_PREFIX.length)
        
        return when (val result = jwtTokenProvider.validateToken(token)) {
            is TokenValidationResult.Valid -> {
                if (result.tokenType != "access") {
                    log.debug("Invalid token type for authentication: ${result.tokenType}")
                    return handleUnauthorized(exchange, "Invalid token type")
                }
                
                log.debug("Authenticated user: ${result.userId}")
                
                val authorities = listOf(SimpleGrantedAuthority("ROLE_USER"))
                val authentication = UsernamePasswordAuthenticationToken(
                    result.userId,
                    null,
                    authorities
                )
                
                chain.filter(exchange)
                    .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication))
            }
            is TokenValidationResult.Expired -> {
                handleUnauthorized(exchange, "Token expired")
            }
            is TokenValidationResult.Invalid -> {
                handleUnauthorized(exchange, "Invalid token")
            }
        }
    }
    
    /**
     * Try to authenticate if a token is present, but don't fail if not.
     */
    private fun tryOptionalAuth(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        val authHeader = exchange.request.headers.getFirst(HttpHeaders.AUTHORIZATION)
        
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            // No token provided - continue without auth
            return chain.filter(exchange)
        }
        
        val token = authHeader.substring(BEARER_PREFIX.length)
        
        return when (val result = jwtTokenProvider.validateToken(token)) {
            is TokenValidationResult.Valid -> {
                if (result.tokenType == "access") {
                    log.debug("Optional auth - authenticated user: ${result.userId}")
                    
                    val authorities = listOf(SimpleGrantedAuthority("ROLE_USER"))
                    val authentication = UsernamePasswordAuthenticationToken(
                        result.userId,
                        null,
                        authorities
                    )
                    
                    chain.filter(exchange)
                        .contextWrite(ReactiveSecurityContextHolder.withAuthentication(authentication))
                } else {
                    // Wrong token type - continue without auth
                    chain.filter(exchange)
                }
            }
            else -> {
                // Invalid or expired token on optional path - continue without auth
                log.debug("Optional auth - token invalid or expired, continuing without auth")
                chain.filter(exchange)
            }
        }
    }

    private fun isPublicPath(path: String): Boolean {
        // Check if path requires authentication first
        if (PROTECTED_PATH_PREFIXES.any { path.startsWith(it) }) {
            return false
        }
        
        if (PUBLIC_PATHS.contains(path)) {
            return true
        }
        return PUBLIC_PATH_PREFIXES.any { path.startsWith(it) }
    }
    
    private fun isOptionalAuthPath(path: String): Boolean {
        // Booking retrieval by PNR is public
        if (path.startsWith("/api/v1/booking/") && !path.startsWith("/api/v1/booking/user")) {
            return true
        }
        return OPTIONAL_AUTH_PATHS.contains(path)
    }

    private fun handleUnauthorized(exchange: ServerWebExchange, message: String): Mono<Void> {
        log.debug("Unauthorized: $message for path ${exchange.request.uri.path}")
        exchange.response.statusCode = HttpStatus.UNAUTHORIZED
        exchange.response.headers.add("Content-Type", "application/json")
        
        val body = """{"error": "Unauthorized", "message": "$message"}"""
        val buffer = exchange.response.bufferFactory().wrap(body.toByteArray())
        
        return exchange.response.writeWith(Mono.just(buffer))
    }
}
