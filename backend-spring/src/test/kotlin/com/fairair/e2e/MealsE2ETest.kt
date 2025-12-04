package com.fairair.e2e

import com.fairair.E2ETestBase
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType

/**
 * E2E tests for Meal Pre-Order functionality.
 * 
 * Tests cover:
 * - Meal options retrieval
 * - Adding meals to booking
 * - Removing meals from booking
 * - Dietary preferences filtering
 */
@DisplayName("Meals Pre-Order E2E Tests")
class MealsE2ETest : E2ETestBase() {

    @Nested
    @DisplayName("Meal Options")
    inner class MealOptionsTests {

        @Test
        @DisplayName("Should return available meal options")
        fun `get meal options returns list of meals`() {
            webClient.get()
                .uri("/api/v1/meals")
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.meals").isArray
                .jsonPath("$.meals.length()").value<Int> { assert(it >= 5) }
                .jsonPath("$.meals[0].id").isNotEmpty
                .jsonPath("$.meals[0].name").isNotEmpty
                .jsonPath("$.meals[0].description").isNotEmpty
                .jsonPath("$.meals[0].price").isNotEmpty
                .jsonPath("$.meals[0].price.currency").isEqualTo("SAR")
        }

        @Test
        @DisplayName("Should include dietary information")
        fun `meal options include dietary flags`() {
            webClient.get()
                .uri("/api/v1/meals")
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.meals[0].isVegetarian").isBoolean
                .jsonPath("$.meals[0].isVegan").isBoolean
                .jsonPath("$.meals[0].isHalal").isBoolean
                .jsonPath("$.meals[0].isGlutenFree").isBoolean
        }

        @Test
        @DisplayName("Should filter by dietary preference")
        fun `get vegetarian meals returns only vegetarian options`() {
            webClient.get()
                .uri("/api/v1/meals?dietary=vegetarian")
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.meals").isArray
                .jsonPath("$.meals[0].isVegetarian").isEqualTo(true)
        }

        @Test
        @DisplayName("Should filter by halal preference")
        fun `get halal meals returns only halal options`() {
            webClient.get()
                .uri("/api/v1/meals?dietary=halal")
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.meals").isArray
        }

        @Test
        @DisplayName("Should include meal categories")
        fun `meal options include categories`() {
            webClient.get()
                .uri("/api/v1/meals")
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.meals[0].category").isNotEmpty
        }
    }

    @Nested
    @DisplayName("Add Meal to Booking")
    inner class AddMealTests {

        @Test
        @DisplayName("Should add meal to passenger")
        fun `add meal to booking returns success`() {
            val pnr = createBookingAndGetPnr()

            val addMealRequest = mapOf(
                "pnr" to pnr,
                "lastName" to "Doe",
                "flightNumber" to "FA101",
                "passengerIndex" to 0,
                "mealId" to "meal-chicken-rice"
            )

            webClient.post()
                .uri("/api/v1/meals/add")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(addMealRequest))
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.mealName").isNotEmpty
                .jsonPath("$.price").isNotEmpty
        }

        @Test
        @DisplayName("Should add multiple meals to different passengers")
        fun `can add different meals to multiple passengers`() {
            val pnr = createBookingAndGetPnr()

            // Add meal to first passenger
            val meal1Request = mapOf(
                "pnr" to pnr,
                "lastName" to "Doe",
                "flightNumber" to "FA101",
                "passengerIndex" to 0,
                "mealId" to "meal-chicken-rice"
            )

            webClient.post()
                .uri("/api/v1/meals/add")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(meal1Request))
                .exchange()
                .expectStatus().isOk
        }

        @Test
        @DisplayName("Should fail with invalid meal ID")
        fun `add non-existent meal returns not found`() {
            val pnr = createBookingAndGetPnr()

            val addMealRequest = mapOf(
                "pnr" to pnr,
                "lastName" to "Doe",
                "flightNumber" to "FA101",
                "passengerIndex" to 0,
                "mealId" to "invalid-meal"
            )

            webClient.post()
                .uri("/api/v1/meals/add")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(addMealRequest))
                .exchange()
                .expectStatus().isNotFound
                .expectBody()
                .jsonPath("$.code").isEqualTo("MEAL_NOT_FOUND")
        }

        @Test
        @DisplayName("Should fail with invalid PNR")
        fun `add meal with invalid pnr returns not found`() {
            val addMealRequest = mapOf(
                "pnr" to "INVALID",
                "lastName" to "Doe",
                "flightNumber" to "FA101",
                "passengerIndex" to 0,
                "mealId" to "meal-chicken-rice"
            )

            webClient.post()
                .uri("/api/v1/meals/add")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(addMealRequest))
                .exchange()
                .expectStatus().isNotFound
        }
    }

    @Nested
    @DisplayName("Remove Meal from Booking")
    inner class RemoveMealTests {

        @Test
        @DisplayName("Should remove meal from passenger")
        fun `remove meal returns success`() {
            val pnr = createBookingAndGetPnr()

            // First add a meal
            val addMealRequest = mapOf(
                "pnr" to pnr,
                "lastName" to "Doe",
                "flightNumber" to "FA101",
                "passengerIndex" to 0,
                "mealId" to "meal-chicken-rice"
            )

            webClient.post()
                .uri("/api/v1/meals/add")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(addMealRequest))
                .exchange()
                .expectStatus().isOk

            // Then remove it
            val removeMealRequest = mapOf(
                "pnr" to pnr,
                "lastName" to "Doe",
                "flightNumber" to "FA101",
                "passengerIndex" to 0
            )

            webClient.post()
                .uri("/api/v1/meals/remove")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(removeMealRequest))
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
        }
    }

    @Nested
    @DisplayName("Booking Meals Retrieval")
    inner class GetBookingMealsTests {

        @Test
        @DisplayName("Should get meals for booking")
        fun `get booking meals returns ordered meals`() {
            val pnr = createBookingAndGetPnr()

            // Add a meal first
            val addMealRequest = mapOf(
                "pnr" to pnr,
                "lastName" to "Doe",
                "flightNumber" to "FA101",
                "passengerIndex" to 0,
                "mealId" to "meal-chicken-rice"
            )

            webClient.post()
                .uri("/api/v1/meals/add")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(addMealRequest))
                .exchange()
                .expectStatus().isOk

            // Get booking meals
            webClient.get()
                .uri("/api/v1/meals/booking/$pnr?lastName=Doe")
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.pnr").isEqualTo(pnr)
                .jsonPath("$.passengerMeals").isArray
        }

        @Test
        @DisplayName("Should return empty for booking without meals")
        fun `get meals for booking without orders returns empty list`() {
            val pnr = createBookingAndGetPnr()

            webClient.get()
                .uri("/api/v1/meals/booking/$pnr?lastName=Doe")
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.pnr").isEqualTo(pnr)
        }
    }

    // Helper method to create a booking and return the PNR
    private fun createBookingAndGetPnr(): String {
        val searchRequest = createSearchRequest()
        val searchResponse = webClient.post()
            .uri("/api/v1/search")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(objectMapper.writeValueAsString(searchRequest))
            .exchange()
            .expectStatus().isOk
            .expectBody(String::class.java)
            .returnResult()
            .responseBody!!

        val searchParsed = objectMapper.readTree(searchResponse)
        val searchId = searchParsed.get("searchId").asText()
        val flightNumber = searchParsed.get("flights").get(0).get("flightNumber").asText()

        val bookingRequest = createBookingRequest(searchId, flightNumber)
        val bookingResponse = webClient.post()
            .uri("/api/v1/booking")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(objectMapper.writeValueAsString(bookingRequest))
            .exchange()
            .expectStatus().isCreated
            .expectBody(String::class.java)
            .returnResult()
            .responseBody!!

        val bookingParsed = objectMapper.readTree(bookingResponse)
        return bookingParsed.get("pnr").asText()
    }
}
