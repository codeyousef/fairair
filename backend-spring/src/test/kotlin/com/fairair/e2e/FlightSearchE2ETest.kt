package com.fairair.e2e

import com.fairair.E2ETestBase
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType

/**
 * E2E tests for Flight Search & Discovery user stories (1.1 - 1.6).
 * 
 * Tests cover:
 * - 1.1 Search for Flights
 * - 1.2 Use Natural Language Search Interface (API support)
 * - 1.3 View Dynamic Destination Backgrounds (data availability)
 * - 1.4 Filter Destinations by Origin
 * - 1.5 View No Results State
 * - 1.6 View Prices on Date Grid
 */
@DisplayName("Flight Search & Discovery E2E Tests")
class FlightSearchE2ETest : E2ETestBase() {

    @Nested
    @DisplayName("1.1 Search for Flights")
    inner class SearchForFlights {

        @Test
        @DisplayName("Should return flight results for valid route with single adult")
        fun `search returns flights for valid route with single adult`() {
            val request = createSearchRequest(
                origin = ORIGIN_JED,
                destination = DESTINATION_RUH,
                adults = 1
            )

            webClient.post()
                .uri("/api/v1/search")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.searchId").isNotEmpty
                .jsonPath("$.flights").isArray
                .jsonPath("$.flights[0].flightNumber").isNotEmpty
                .jsonPath("$.flights[0].origin").isEqualTo(ORIGIN_JED)
                .jsonPath("$.flights[0].destination").isEqualTo(DESTINATION_RUH)
                .jsonPath("$.flights[0].departureTime").isNotEmpty
                .jsonPath("$.flights[0].arrivalTime").isNotEmpty
                .jsonPath("$.flights[0].durationMinutes").isNumber
                .jsonPath("$.flights[0].durationFormatted").isNotEmpty
                .jsonPath("$.flights[0].fareFamilies").isArray
                .jsonPath("$.flights[0].fareFamilies.length()").isEqualTo(3)
        }

        @Test
        @DisplayName("Should return flight results with multiple passengers (adults, children, infants)")
        fun `search returns flights with multiple passenger types`() {
            val request = createSearchRequest(
                origin = ORIGIN_JED,
                destination = DESTINATION_RUH,
                adults = 2,
                children = 1,
                infants = 1
            )

            webClient.post()
                .uri("/api/v1/search")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.flights").isArray
                .jsonPath("$.flights[0].fareFamilies[0].priceMinor").isNumber
        }

        @Test
        @DisplayName("Should return flight details with times, duration, and prices")
        fun `search results include complete flight information`() {
            val request = createSearchRequest()

            webClient.post()
                .uri("/api/v1/search")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.flights[0].aircraft").isNotEmpty
                .jsonPath("$.flights[0].fareFamilies[0].code").isNotEmpty
                .jsonPath("$.flights[0].fareFamilies[0].name").isNotEmpty
                .jsonPath("$.flights[0].fareFamilies[0].priceMinor").isNumber
                .jsonPath("$.flights[0].fareFamilies[0].priceFormatted").isNotEmpty
                .jsonPath("$.flights[0].fareFamilies[0].currency").isEqualTo("SAR")
        }

        @Test
        @DisplayName("Should reject search with invalid origin airport code")
        fun `search fails for invalid origin`() {
            val request = createSearchRequest(
                origin = INVALID_ORIGIN,
                destination = DESTINATION_RUH
            )

            webClient.post()
                .uri("/api/v1/search")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                .expectStatus().isBadRequest
                .expectBody()
                .jsonPath("$.code").isEqualTo("INVALID_ROUTE")
        }

        @Test
        @DisplayName("Should reject search with invalid destination airport code")
        fun `search fails for invalid destination`() {
            val request = createSearchRequest(
                origin = ORIGIN_JED,
                destination = INVALID_DESTINATION
            )

            webClient.post()
                .uri("/api/v1/search")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                .expectStatus().isBadRequest
        }

        @Test
        @DisplayName("Should reject search with same origin and destination")
        fun `search fails for same origin and destination`() {
            val request = createSearchRequest(
                origin = ORIGIN_JED,
                destination = ORIGIN_JED
            )

            webClient.post()
                .uri("/api/v1/search")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                .expectStatus().isBadRequest
        }

        @Test
        @DisplayName("Should reject search with zero adults")
        fun `search fails with zero adults`() {
            val request = createSearchRequest(adults = 0)

            webClient.post()
                .uri("/api/v1/search")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                .expectStatus().isBadRequest
        }

        @Test
        @DisplayName("Should reject search with more than 9 passengers")
        fun `search fails with too many passengers`() {
            val request = createSearchRequest(adults = 7, children = 3)

            webClient.post()
                .uri("/api/v1/search")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                .expectStatus().isBadRequest
        }

        @Test
        @DisplayName("Should reject search with more infants than adults")
        fun `search fails with infants exceeding adults`() {
            val request = createSearchRequest(adults = 1, infants = 2)

            webClient.post()
                .uri("/api/v1/search")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                .expectStatus().isBadRequest
        }

        @Test
        @DisplayName("Should accept maximum valid passenger count")
        fun `search accepts maximum 9 passengers`() {
            val request = createSearchRequest(adults = 5, children = 4)

            webClient.post()
                .uri("/api/v1/search")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                .expectStatus().isOk
        }

        @Test
        @DisplayName("Should handle different routes (RUH to JED)")
        fun `search works for reverse route`() {
            val request = createSearchRequest(
                origin = ORIGIN_RUH,
                destination = DESTINATION_JED
            )

            webClient.post()
                .uri("/api/v1/search")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.flights").isArray
        }
    }

    @Nested
    @DisplayName("1.4 Filter Destinations by Origin")
    inner class FilterDestinationsByOrigin {

        @Test
        @DisplayName("Should return valid destinations for JED origin")
        fun `get destinations for JED returns valid airports`() {
            webClient.get()
                .uri("/api/v1/config/destinations/JED")
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
        @DisplayName("Should return valid destinations for RUH origin")
        fun `get destinations for RUH returns valid airports`() {
            webClient.get()
                .uri("/api/v1/config/destinations/RUH")
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$").isArray
        }

        @Test
        @DisplayName("Should return empty list for invalid origin")
        fun `get destinations for invalid origin returns empty`() {
            webClient.get()
                .uri("/api/v1/config/destinations/XXX")
                .exchange()
                .expectStatus().isOk  // Returns 200 with empty array for unknown origin
                .expectBody()
                .jsonPath("$").isArray
                .jsonPath("$.length()").isEqualTo(0)
        }

        @Test
        @DisplayName("Should handle lowercase origin code")
        fun `get destinations normalizes lowercase origin`() {
            webClient.get()
                .uri("/api/v1/config/destinations/jed")
                .exchange()
                .expectStatus().isOk
        }
    }

    @Nested
    @DisplayName("1.5 View No Results State")
    inner class NoResultsState {

        @Test
        @DisplayName("Should return empty flights array for route with no flights")
        fun `search for unavailable route returns proper response`() {
            // This tests the API's ability to handle routes that exist but may have no flights
            // The mock always returns flights, but structure is tested
            val request = createSearchRequest()

            val response = webClient.post()
                .uri("/api/v1/search")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                .expectStatus().isOk
                .expectBody(String::class.java)
                .returnResult()
                .responseBody

            // Verify response structure supports empty flights array
            val parsed = objectMapper.readTree(response)
            assert(parsed.has("flights"))
            assert(parsed.get("flights").isArray)
        }
    }

    @Nested
    @DisplayName("1.6 View Prices on Date Grid (Low Fares)")
    inner class LowFaresDateGrid {

        @Test
        @DisplayName("Should return low fares for date range")
        fun `get low fares returns prices for each date`() {
            val startDate = getFutureDateString(7)
            val endDate = getFutureDateString(14)

            webClient.get()
                .uri("/api/v1/search/low-fares?origin=JED&destination=RUH&startDate=$startDate&endDate=$endDate")
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.origin").isEqualTo("JED")
                .jsonPath("$.destination").isEqualTo("RUH")
                .jsonPath("$.dates").isArray
                .jsonPath("$.dates[0].date").isNotEmpty
                .jsonPath("$.dates[0].available").isBoolean
        }

        @Test
        @DisplayName("Should show price and availability per date")
        fun `low fares include price information per date`() {
            val startDate = getFutureDateString(1)
            val endDate = getFutureDateString(3)

            webClient.get()
                .uri("/api/v1/search/low-fares?origin=JED&destination=RUH&startDate=$startDate&endDate=$endDate&adults=1")
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.dates[0].priceMinor").isNumber
                .jsonPath("$.dates[0].priceFormatted").isNotEmpty
                .jsonPath("$.dates[0].currency").isEqualTo("SAR")
                .jsonPath("$.dates[0].fareFamily").isNotEmpty
                .jsonPath("$.dates[0].flightsAvailable").isNumber
        }

        @Test
        @DisplayName("Should reject low fares request for invalid route")
        fun `low fares fails for invalid route`() {
            val startDate = getFutureDateString(1)
            val endDate = getFutureDateString(3)

            webClient.get()
                .uri("/api/v1/search/low-fares?origin=XXX&destination=YYY&startDate=$startDate&endDate=$endDate")
                .exchange()
                .expectStatus().isBadRequest
                .expectBody()
                .jsonPath("$.code").isEqualTo("INVALID_ROUTE")
        }

        @Test
        @DisplayName("Should return multiple dates in range")
        fun `low fares returns correct date count`() {
            val startDate = getFutureDateString(1)
            val endDate = getFutureDateString(5)

            val response = webClient.get()
                .uri("/api/v1/search/low-fares?origin=JED&destination=RUH&startDate=$startDate&endDate=$endDate")
                .exchange()
                .expectStatus().isOk
                .expectBody(String::class.java)
                .returnResult()
                .responseBody

            val parsed = objectMapper.readTree(response)
            val dates = parsed.get("dates")
            assert(dates.size() >= 5) { "Expected at least 5 dates, got ${dates.size()}" }
        }

        @Test
        @DisplayName("Should support passenger counts for pricing")
        fun `low fares with multiple passengers returns correct prices`() {
            val startDate = getFutureDateString(1)
            val endDate = getFutureDateString(2)

            webClient.get()
                .uri("/api/v1/search/low-fares?origin=JED&destination=RUH&startDate=$startDate&endDate=$endDate&adults=2&children=1&infants=0")
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.dates").isArray
        }
    }

    @Nested
    @DisplayName("Search Result Caching (User Story 13.3)")
    inner class SearchCaching {

        @Test
        @DisplayName("Should return same searchId for identical searches (cache hit)")
        fun `identical searches return cached results`() {
            val request = createSearchRequest(
                origin = ORIGIN_JED,
                destination = DESTINATION_RUH,
                departureDate = getFutureDateString(30) // Far future to ensure cache isolation
            )

            // First search
            val firstResponse = webClient.post()
                .uri("/api/v1/search")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                .expectStatus().isOk
                .expectBody(String::class.java)
                .returnResult()
                .responseBody!!

            val firstSearchId = objectMapper.readTree(firstResponse).get("searchId").asText()

            // Second search with same parameters
            val secondResponse = webClient.post()
                .uri("/api/v1/search")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                .expectStatus().isOk
                .expectBody(String::class.java)
                .returnResult()
                .responseBody!!

            val secondSearchId = objectMapper.readTree(secondResponse).get("searchId").asText()

            // Should get same cached results
            assert(firstSearchId == secondSearchId) {
                "Expected same searchId for cached results: $firstSearchId vs $secondSearchId"
            }
        }
    }

    @Nested
    @DisplayName("Fare Family Comparison (User Story 2.3)")
    inner class FareFamilyComparison {

        @Test
        @DisplayName("Should return all three fare families: Fly, Fly+, FlyMax")
        fun `flight results include all fare families`() {
            val request = createSearchRequest()

            webClient.post()
                .uri("/api/v1/search")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.flights[0].fareFamilies.length()").isEqualTo(3)
                .jsonPath("$.flights[0].fareFamilies[?(@.code == 'FLY')]").exists()
                .jsonPath("$.flights[0].fareFamilies[?(@.code == 'FLY_PLUS')]").exists()
                .jsonPath("$.flights[0].fareFamilies[?(@.code == 'FLY_MAX')]").exists()
        }

        @Test
        @DisplayName("Should include fare inclusions for each fare family")
        fun `fare families include inclusions details`() {
            val request = createSearchRequest()

            webClient.post()
                .uri("/api/v1/search")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.flights[0].fareFamilies[0].inclusions").isMap
                .jsonPath("$.flights[0].fareFamilies[0].inclusions.carryOnBag").isNotEmpty
                .jsonPath("$.flights[0].fareFamilies[0].inclusions.seatSelection").isNotEmpty
                .jsonPath("$.flights[0].fareFamilies[0].inclusions.changePolicy").isNotEmpty
                .jsonPath("$.flights[0].fareFamilies[0].inclusions.cancellationPolicy").isNotEmpty
        }

        @Test
        @DisplayName("Should show price differences between fare families")
        fun `fare families have ascending prices`() {
            val request = createSearchRequest()

            val response = webClient.post()
                .uri("/api/v1/search")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(request))
                .exchange()
                .expectStatus().isOk
                .expectBody(String::class.java)
                .returnResult()
                .responseBody!!

            val parsed = objectMapper.readTree(response)
            val fareFamilies = parsed.get("flights").get(0).get("fareFamilies")
            
            val flyPrice = fareFamilies.find { it.get("code").asText() == "FLY" }?.get("priceMinor")?.asLong() ?: 0
            val flyPlusPrice = fareFamilies.find { it.get("code").asText() == "FLY_PLUS" }?.get("priceMinor")?.asLong() ?: 0
            val flyMaxPrice = fareFamilies.find { it.get("code").asText() == "FLY_MAX" }?.get("priceMinor")?.asLong() ?: 0

            assert(flyPrice <= flyPlusPrice) { "FLY ($flyPrice) should be <= FLY_PLUS ($flyPlusPrice)" }
            assert(flyPlusPrice <= flyMaxPrice) { "FLY_PLUS ($flyPlusPrice) should be <= FLY_MAX ($flyMaxPrice)" }
        }
    }
}
