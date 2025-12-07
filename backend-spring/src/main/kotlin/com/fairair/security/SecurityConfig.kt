package com.fairair.security

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.SecurityWebFiltersOrder
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.web.server.SecurityWebFilterChain
import org.springframework.security.web.server.context.NoOpServerSecurityContextRepository
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.reactive.CorsConfigurationSource
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource

/**
 * Spring Security configuration for WebFlux.
 * Configures JWT authentication and authorization.
 */
@Configuration
@EnableWebFluxSecurity
class SecurityConfig(
    private val jwtAuthenticationFilter: JwtAuthenticationFilter,
    private val securityProperties: SecurityProperties
) {

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()
        configuration.allowedOrigins = securityProperties.cors.allowedOrigins
        configuration.allowedMethods = securityProperties.cors.allowedMethods
        configuration.allowedHeaders = listOf("*")
        configuration.allowCredentials = true
        configuration.maxAge = securityProperties.cors.maxAge
        
        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }

    @Bean
    fun securityWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        return http
            // Enable CORS with our configuration
            .cors { it.configurationSource(corsConfigurationSource()) }
            
            // Disable CSRF for stateless JWT authentication
            .csrf { it.disable() }
            
            // Disable form login and HTTP basic (we use JWT)
            .formLogin { it.disable() }
            .httpBasic { it.disable() }
            
            // Use stateless session management
            .securityContextRepository(NoOpServerSecurityContextRepository.getInstance())
            
            // Add JWT authentication filter
            .addFilterAt(jwtAuthenticationFilter, SecurityWebFiltersOrder.AUTHENTICATION)
            
            // Configure authorization
            .authorizeExchange { exchanges ->
                exchanges
                    // Public endpoints - no authentication required
                    .pathMatchers("/health", "/health/**").permitAll()
                    .pathMatchers("/actuator/**").permitAll()
                    .pathMatchers("/api/v1/auth/**").permitAll()
                    .pathMatchers("/api/v1/config/**").permitAll()
                    .pathMatchers("/api/v1/location/**").permitAll()
                    .pathMatchers("/api/v1/search", "/api/v1/search/**").permitAll()
                    .pathMatchers("/api/v1/booking", "/api/v1/booking/**").permitAll()
                    .pathMatchers("/api/v1/checkin", "/api/v1/checkin/**").permitAll()
                    .pathMatchers("/api/v1/manage", "/api/v1/manage/**").permitAll()
                    .pathMatchers("/api/v1/seats", "/api/v1/seats/**").permitAll()
                    .pathMatchers("/api/v1/meals", "/api/v1/meals/**").permitAll()
                    .pathMatchers("/api/v1/content", "/api/v1/content/**").permitAll()
                    .pathMatchers("/api/v1/chat", "/api/v1/chat/**").permitAll() // Pilot AI chat
                    .pathMatchers(HttpMethod.GET, "/api/v1/membership/plans").permitAll()
                    
                    // Admin endpoints (admin auth is handled separately)
                    .pathMatchers("/api/admin/auth/**").permitAll()
                    .pathMatchers("/api/admin/**").permitAll() // TODO: Add proper admin auth
                    
                    // B2B endpoints (B2B auth is handled separately)
                    .pathMatchers("/api/b2b/auth/**").permitAll()
                    .pathMatchers("/api/b2b/**").permitAll() // TODO: Add proper B2B auth
                    
                    // Public content API
                    .pathMatchers("/api/content/**").permitAll()
                    
                    // Public group booking and charter request endpoints
                    .pathMatchers("/api/group-bookings/**").permitAll()
                    .pathMatchers("/api/charter/**").permitAll()
                    
                    // Allow OPTIONS for CORS preflight
                    .pathMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                    
                    // All other requests require authentication
                    .anyExchange().authenticated()
            }
            .build()
    }
}
