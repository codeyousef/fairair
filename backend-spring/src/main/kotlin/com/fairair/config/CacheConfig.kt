package com.fairair.config

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Duration

/**
 * Cache configuration using Caffeine.
 *
 * Provides caching for:
 * - Routes and stations: 24-hour TTL
 * - Search results by searchId: 5-minute TTL (for booking validation)
 * - Route searches: 5-minute TTL (for sharing results across users)
 *
 * Uses Caffeine directly without Spring's @Cacheable abstraction
 * to match the Quarkus implementation's manual cache management.
 */
@Configuration
class CacheConfig(
    private val fairairProperties: FairairProperties
) {
    companion object {
        const val ROUTES_CACHE = "routes"
        const val STATIONS_CACHE = "stations"
        const val SEARCHES_CACHE = "searches"
        const val ROUTE_SEARCHES_CACHE = "route-searches"
    }

    @Bean
    fun routesCache(): Cache<String, Any> {
        return buildCache(Duration.ofSeconds(fairairProperties.cache.routesTtl))
    }

    @Bean
    fun stationsCache(): Cache<String, Any> {
        return buildCache(Duration.ofSeconds(fairairProperties.cache.routesTtl))
    }

    @Bean
    fun searchesCache(): Cache<String, Any> {
        return buildCache(Duration.ofSeconds(fairairProperties.cache.searchTtl))
    }
    
    /**
     * Cache for route-based search results.
     * Key: "origin-destination-date-adults-children-infants"
     * 
     * This cache enables sharing search results across different users
     * who search for the same route, reducing backend load and improving
     * response times.
     */
    @Bean
    fun routeSearchesCache(): Cache<String, Any> {
        return buildCache(
            ttl = Duration.ofSeconds(fairairProperties.cache.searchTtl),
            maxSize = 5000 // Larger cache for route combinations
        )
    }

    private fun buildCache(ttl: Duration, maxSize: Long = 1000): Cache<String, Any> {
        return Caffeine.newBuilder()
            .expireAfterWrite(ttl)
            .maximumSize(maxSize)
            .recordStats()
            .build()
    }
}
