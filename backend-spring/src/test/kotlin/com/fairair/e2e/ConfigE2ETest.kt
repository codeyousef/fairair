package com.fairair.e2e

import com.fairair.E2ETestBase
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test

/**
 * E2E tests for App Initialization and Configuration user stories (13.1 - 13.3).
 * 
 * Tests cover:
 * - 13.1 Launch App (Config availability)
 * - 13.2 Use Cached Data (Route and station data)
 * - 13.3 Benefit from Route Search Caching
 * 
 * Also covers Config API for Language & Localization support (8.1 - 8.3).
 */
@DisplayName("Configuration & Initialization E2E Tests")
class ConfigE2ETest : E2ETestBase() {

    @Nested
    @DisplayName("13.1 & 13.2 App Launch - Route Network")
    inner class RouteNetwork {

        @Test
        @DisplayName("Should return route map with all valid routes")
        fun `get routes returns complete route map`() {
            webClient.get()
                .uri("/api/v1/config/routes")
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.routes").isMap
                .jsonPath("$.routes.JED").isArray
                .jsonPath("$.routes.RUH").isArray
        }

        @Test
        @DisplayName("Should include all major Saudi airports in routes")
        fun `routes include major airports`() {
            val response = webClient.get()
                .uri("/api/v1/config/routes")
                .exchange()
                .expectStatus().isOk
                .expectBody(String::class.java)
                .returnResult()
                .responseBody!!

            val parsed = objectMapper.readTree(response)
            val routes = parsed.get("routes")
            
            // Verify major Saudi airports are present as origins
            assert(routes.has("JED")) { "Should include Jeddah (JED)" }
            assert(routes.has("RUH")) { "Should include Riyadh (RUH)" }
        }

        @Test
        @DisplayName("Should return destinations for each origin")
        fun `each origin has valid destinations`() {
            val response = webClient.get()
                .uri("/api/v1/config/routes")
                .exchange()
                .expectStatus().isOk
                .expectBody(String::class.java)
                .returnResult()
                .responseBody!!

            val parsed = objectMapper.readTree(response)
            val routes = parsed.get("routes")
            
            routes.fieldNames().forEach { origin ->
                val destinations = routes.get(origin)
                assert(destinations.isArray) { "Destinations for $origin should be an array" }
                assert(destinations.size() > 0) { "Origin $origin should have at least one destination" }
            }
        }
    }

    @Nested
    @DisplayName("13.1 & 13.2 App Launch - Stations")
    inner class Stations {

        @Test
        @DisplayName("Should return all stations with complete info")
        fun `get stations returns airport details`() {
            webClient.get()
                .uri("/api/v1/config/stations")
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$").isArray
                .jsonPath("$[0].code").isNotEmpty
                .jsonPath("$[0].name").isNotEmpty
                .jsonPath("$[0].city").isNotEmpty
                .jsonPath("$[0].country").isNotEmpty
        }

        @Test
        @DisplayName("Should include major Saudi airports")
        fun `stations include major Saudi airports`() {
            val response = webClient.get()
                .uri("/api/v1/config/stations")
                .exchange()
                .expectStatus().isOk
                .expectBody(String::class.java)
                .returnResult()
                .responseBody!!

            val stations = objectMapper.readTree(response)
            
            val codes = stations.map { it.get("code").asText() }
            assert("JED" in codes) { "Should include Jeddah (JED)" }
            assert("RUH" in codes) { "Should include Riyadh (RUH)" }
        }

        @Test
        @DisplayName("Should return 3-letter IATA codes for all stations")
        fun `all station codes are valid IATA codes`() {
            val response = webClient.get()
                .uri("/api/v1/config/stations")
                .exchange()
                .expectStatus().isOk
                .expectBody(String::class.java)
                .returnResult()
                .responseBody!!

            val stations = objectMapper.readTree(response)
            
            stations.forEach { station ->
                val code = station.get("code").asText()
                assert(code.length == 3) { "Station code should be 3 characters: $code" }
                assert(code.all { it.isUpperCase() }) { "Station code should be uppercase: $code" }
            }
        }
    }

    @Nested
    @DisplayName("1.4 & 13.1 Filtered Destinations")
    inner class FilteredDestinations {

        @Test
        @DisplayName("Should return destinations for JED origin")
        fun `get destinations for JED`() {
            webClient.get()
                .uri("/api/v1/config/destinations/JED")
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$").isArray
                .jsonPath("$[0].code").isNotEmpty
        }

        @Test
        @DisplayName("Should return destinations for RUH origin")
        fun `get destinations for RUH`() {
            webClient.get()
                .uri("/api/v1/config/destinations/RUH")
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$").isArray
        }

        @Test
        @DisplayName("Should return full station info for destinations")
        fun `destinations include complete station details`() {
            webClient.get()
                .uri("/api/v1/config/destinations/JED")
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$[0].code").isNotEmpty
                .jsonPath("$[0].name").isNotEmpty
                .jsonPath("$[0].city").isNotEmpty
                .jsonPath("$[0].country").isNotEmpty
        }

        @Test
        @DisplayName("Should return empty for invalid origin")
        fun `destinations for invalid origin returns error`() {
            webClient.get()
                .uri("/api/v1/config/destinations/INVALID")
                .exchange()
                .expectStatus().isBadRequest
        }

        @Test
        @DisplayName("Should handle lowercase origin")
        fun `destinations endpoint normalizes lowercase`() {
            webClient.get()
                .uri("/api/v1/config/destinations/jed")
                .exchange()
                .expectStatus().isOk
        }

        @Test
        @DisplayName("Should not include origin in destinations")
        fun `JED is not in destinations for JED`() {
            val response = webClient.get()
                .uri("/api/v1/config/destinations/JED")
                .exchange()
                .expectStatus().isOk
                .expectBody(String::class.java)
                .returnResult()
                .responseBody!!

            val destinations = objectMapper.readTree(response)
            val codes = destinations.map { it.get("code").asText() }
            
            assert("JED" !in codes) { "Origin JED should not be in its own destinations" }
        }
    }

    @Nested
    @DisplayName("Consistency Checks")
    inner class ConsistencyChecks {

        @Test
        @DisplayName("All routes should reference valid stations")
        fun `routes only reference existing stations`() {
            // Get stations
            val stationsResponse = webClient.get()
                .uri("/api/v1/config/stations")
                .exchange()
                .expectStatus().isOk
                .expectBody(String::class.java)
                .returnResult()
                .responseBody!!

            val stations = objectMapper.readTree(stationsResponse)
            val stationCodes = stations.map { it.get("code").asText() }.toSet()

            // Get routes
            val routesResponse = webClient.get()
                .uri("/api/v1/config/routes")
                .exchange()
                .expectStatus().isOk
                .expectBody(String::class.java)
                .returnResult()
                .responseBody!!

            val routes = objectMapper.readTree(routesResponse).get("routes")
            
            // Verify all origins and destinations exist in stations
            routes.fieldNames().forEach { origin ->
                assert(origin in stationCodes) { "Origin $origin should exist in stations" }
                
                routes.get(origin).forEach { destination ->
                    val destCode = destination.asText()
                    assert(destCode in stationCodes) { "Destination $destCode should exist in stations" }
                }
            }
        }

        @Test
        @DisplayName("Destinations endpoint should match routes")
        fun `destinations API matches route map`() {
            // Get routes
            val routesResponse = webClient.get()
                .uri("/api/v1/config/routes")
                .exchange()
                .expectStatus().isOk
                .expectBody(String::class.java)
                .returnResult()
                .responseBody!!

            val routes = objectMapper.readTree(routesResponse).get("routes")
            val jedDestinations = routes.get("JED")?.map { it.asText() }?.toSet() ?: emptySet()

            // Get destinations via API
            val destinationsResponse = webClient.get()
                .uri("/api/v1/config/destinations/JED")
                .exchange()
                .expectStatus().isOk
                .expectBody(String::class.java)
                .returnResult()
                .responseBody!!

            val apiDestinations = objectMapper.readTree(destinationsResponse)
            val apiCodes = apiDestinations.map { it.get("code").asText() }.toSet()

            assert(jedDestinations == apiCodes) {
                "Destinations API should match route map. Routes: $jedDestinations, API: $apiCodes"
            }
        }
    }

    @Nested
    @DisplayName("Caching Behavior")
    inner class CachingBehavior {

        @Test
        @DisplayName("Routes should be consistent across requests")
        fun `repeated route requests return same data`() {
            val firstResponse = webClient.get()
                .uri("/api/v1/config/routes")
                .exchange()
                .expectStatus().isOk
                .expectBody(String::class.java)
                .returnResult()
                .responseBody!!

            val secondResponse = webClient.get()
                .uri("/api/v1/config/routes")
                .exchange()
                .expectStatus().isOk
                .expectBody(String::class.java)
                .returnResult()
                .responseBody!!

            assert(firstResponse == secondResponse) { "Route data should be consistent" }
        }

        @Test
        @DisplayName("Stations should be consistent across requests")
        fun `repeated station requests return same data`() {
            val firstResponse = webClient.get()
                .uri("/api/v1/config/stations")
                .exchange()
                .expectStatus().isOk
                .expectBody(String::class.java)
                .returnResult()
                .responseBody!!

            val secondResponse = webClient.get()
                .uri("/api/v1/config/stations")
                .exchange()
                .expectStatus().isOk
                .expectBody(String::class.java)
                .returnResult()
                .responseBody!!

            assert(firstResponse == secondResponse) { "Station data should be consistent" }
        }
    }

    @Nested
    @DisplayName("Response Format")
    inner class ResponseFormat {

        @Test
        @DisplayName("Routes should return JSON object with routes map")
        fun `routes response has correct structure`() {
            webClient.get()
                .uri("/api/v1/config/routes")
                .exchange()
                .expectStatus().isOk
                .expectHeader().contentType("application/json")
                .expectBody()
                .jsonPath("$.routes").isMap
        }

        @Test
        @DisplayName("Stations should return JSON array")
        fun `stations response is array`() {
            webClient.get()
                .uri("/api/v1/config/stations")
                .exchange()
                .expectStatus().isOk
                .expectHeader().contentType("application/json")
                .expectBody()
                .jsonPath("$").isArray
        }

        @Test
        @DisplayName("Destinations should return JSON array")
        fun `destinations response is array`() {
            webClient.get()
                .uri("/api/v1/config/destinations/JED")
                .exchange()
                .expectStatus().isOk
                .expectHeader().contentType("application/json")
                .expectBody()
                .jsonPath("$").isArray
        }
    }
}
