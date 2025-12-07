package com.fairair.config

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Configuration properties for the fairair application.
 * Maps to the fairair.* prefix in application.yml.
 */
@ConfigurationProperties(prefix = "fairair")
data class FairairProperties(
    /**
     * Provider mode: "mock" or "real"
     * - mock: Uses MockNavitaireClient with simulated data
     * - real: Uses RealNavitaireClient connecting to actual Navitaire services
     */
    val provider: String = "mock",

    /**
     * Cache configuration settings
     */
    val cache: CacheProperties = CacheProperties(),

    /**
     * Mock provider simulation settings
     */
    val mock: MockProperties = MockProperties(),

    /**
     * External API timeout settings
     */
    val timeout: TimeoutProperties = TimeoutProperties(),

    /**
     * AI assistant configuration
     */
    val ai: AiProperties = AiProperties()
)

/**
 * Cache TTL configuration
 */
data class CacheProperties(
    /**
     * TTL for routes and stations cache in seconds (default: 24 hours)
     */
    val routesTtl: Long = 86400,

    /**
     * TTL for search results cache in seconds (default: 5 minutes)
     */
    val searchTtl: Long = 300
)

/**
 * Mock provider delay configuration for realistic latency simulation
 */
data class MockProperties(
    /**
     * Minimum delay in milliseconds for mock responses
     */
    val minDelay: Long = 500,

    /**
     * Maximum delay in milliseconds for mock responses
     */
    val maxDelay: Long = 1500
)

/**
 * Timeout configuration for external API calls
 */
data class TimeoutProperties(
    /**
     * Connection timeout in milliseconds (default: 5 seconds)
     */
    val connectMs: Long = 5000,

    /**
     * Read timeout in milliseconds (default: 30 seconds)
     */
    val readMs: Long = 30000,

    /**
     * Write timeout in milliseconds (default: 10 seconds)
     */
    val writeMs: Long = 10000,

    /**
     * Overall request timeout in milliseconds (default: 60 seconds)
     */
    val requestMs: Long = 60000
)

/**
 * AI assistant configuration for Vertex AI / Llama 3.1
 */
data class AiProperties(
    /**
     * Whether AI features are enabled
     */
    val enabled: Boolean = true,

    /**
     * Google Cloud project ID for Vertex AI
     */
    val projectId: String = "",

    /**
     * Google Cloud region for Vertex AI (e.g., "us-central1")
     */
    val location: String = "us-central1",

    /**
     * Model ID to use (e.g., "llama-3.1-70b-instruct-maas")
     */
    val model: String = "llama-3.1-70b-instruct-maas",

    /**
     * Maximum tokens in the response
     */
    val maxTokens: Int = 4096,

    /**
     * Temperature for response generation (0.0 - 1.0)
     */
    val temperature: Double = 0.7,

    /**
     * Session timeout in seconds (default: 30 minutes)
     */
    val sessionTimeoutSeconds: Long = 1800
)
