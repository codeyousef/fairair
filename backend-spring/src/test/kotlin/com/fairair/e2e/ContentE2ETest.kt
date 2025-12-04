package com.fairair.e2e

import com.fairair.E2ETestBase
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.springframework.http.MediaType

/**
 * E2E tests for Content/Help Center functionality.
 * 
 * Tests cover:
 * - Help categories and articles
 * - Destinations listing
 * - Newsletter subscription
 * - Contact form submission
 */
@DisplayName("Content & Help Center E2E Tests")
class ContentE2ETest : E2ETestBase() {

    @Nested
    @DisplayName("Help Center")
    inner class HelpCenterTests {

        @Test
        @DisplayName("Should return help categories")
        fun `get help categories returns list of categories`() {
            webClient.get()
                .uri("/api/v1/content/help/categories")
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.categories").isArray
                .jsonPath("$.categories.length()").value<Int> { assert(it >= 4) }
                .jsonPath("$.categories[0].id").isNotEmpty
                .jsonPath("$.categories[0].name").isNotEmpty
                .jsonPath("$.categories[0].description").isNotEmpty
                .jsonPath("$.categories[0].icon").isNotEmpty
        }

        @Test
        @DisplayName("Should return articles for category")
        fun `get articles by category returns list`() {
            webClient.get()
                .uri("/api/v1/content/help/categories/booking")
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.categoryId").isEqualTo("booking")
                .jsonPath("$.categoryName").isNotEmpty
                .jsonPath("$.articles").isArray
                .jsonPath("$.articles[0].id").isNotEmpty
                .jsonPath("$.articles[0].title").isNotEmpty
                .jsonPath("$.articles[0].summary").isNotEmpty
        }

        @Test
        @DisplayName("Should return 404 for invalid category")
        fun `get articles for non-existent category returns not found`() {
            webClient.get()
                .uri("/api/v1/content/help/categories/invalid-category")
                .exchange()
                .expectStatus().isNotFound
                .expectBody()
                .jsonPath("$.code").isEqualTo("CATEGORY_NOT_FOUND")
        }

        @Test
        @DisplayName("Should return specific help article")
        fun `get article by id returns full article`() {
            webClient.get()
                .uri("/api/v1/content/help/articles/how-to-book")
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.id").isEqualTo("how-to-book")
                .jsonPath("$.title").isNotEmpty
                .jsonPath("$.content").isNotEmpty
                .jsonPath("$.categoryId").isNotEmpty
        }

        @Test
        @DisplayName("Should return 404 for invalid article")
        fun `get non-existent article returns not found`() {
            webClient.get()
                .uri("/api/v1/content/help/articles/invalid-article")
                .exchange()
                .expectStatus().isNotFound
                .expectBody()
                .jsonPath("$.code").isEqualTo("ARTICLE_NOT_FOUND")
        }

        @Test
        @DisplayName("Should search help articles")
        fun `search help returns matching articles`() {
            webClient.get()
                .uri("/api/v1/content/help/search?q=baggage")
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.query").isEqualTo("baggage")
                .jsonPath("$.results").isArray
        }
    }

    @Nested
    @DisplayName("Destinations")
    inner class DestinationsTests {

        @Test
        @DisplayName("Should return all destinations")
        fun `get destinations returns list of destinations`() {
            webClient.get()
                .uri("/api/v1/content/destinations")
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.destinations").isArray
                .jsonPath("$.destinations[0].code").isNotEmpty
                .jsonPath("$.destinations[0].name").isNotEmpty
                .jsonPath("$.destinations[0].country").isNotEmpty
                .jsonPath("$.destinations[0].description").isNotEmpty
        }

        @Test
        @DisplayName("Should return destination details")
        fun `get destination by code returns details`() {
            webClient.get()
                .uri("/api/v1/content/destinations/RUH")
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.code").isEqualTo("RUH")
                .jsonPath("$.name").isNotEmpty
                .jsonPath("$.country").isNotEmpty
                .jsonPath("$.description").isNotEmpty
                .jsonPath("$.highlights").isArray
        }

        @Test
        @DisplayName("Should return 404 for invalid destination")
        fun `get non-existent destination returns not found`() {
            webClient.get()
                .uri("/api/v1/content/destinations/INVALID")
                .exchange()
                .expectStatus().isNotFound
                .expectBody()
                .jsonPath("$.code").isEqualTo("DESTINATION_NOT_FOUND")
        }
    }

    @Nested
    @DisplayName("Newsletter")
    inner class NewsletterTests {

        @Test
        @DisplayName("Should subscribe to newsletter")
        fun `subscribe to newsletter returns success`() {
            val email = "newsletter-${System.currentTimeMillis()}@test.com"
            val subscribeRequest = mapOf(
                "email" to email,
                "name" to "Test User",
                "preferences" to listOf("deals", "news")
            )

            webClient.post()
                .uri("/api/v1/content/newsletter/subscribe")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(subscribeRequest))
                .exchange()
                .expectStatus().isCreated
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.email").isEqualTo(email)
                .jsonPath("$.message").isNotEmpty
        }

        @Test
        @DisplayName("Should reject duplicate subscription")
        fun `subscribe with existing email returns conflict`() {
            val email = "duplicate-${System.currentTimeMillis()}@test.com"
            val subscribeRequest = mapOf(
                "email" to email,
                "name" to "Test User"
            )

            // First subscription
            webClient.post()
                .uri("/api/v1/content/newsletter/subscribe")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(subscribeRequest))
                .exchange()
                .expectStatus().isCreated

            // Second subscription with same email
            webClient.post()
                .uri("/api/v1/content/newsletter/subscribe")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(subscribeRequest))
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody()
                .jsonPath("$.code").isEqualTo("ALREADY_SUBSCRIBED")
        }

        @Test
        @DisplayName("Should reject invalid email")
        fun `subscribe with invalid email returns bad request`() {
            val subscribeRequest = mapOf(
                "email" to "invalid-email",
                "name" to "Test User"
            )

            webClient.post()
                .uri("/api/v1/content/newsletter/subscribe")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(subscribeRequest))
                .exchange()
                .expectStatus().isBadRequest
                .expectBody()
                .jsonPath("$.code").isEqualTo("VALIDATION_ERROR")
        }

        @Test
        @DisplayName("Should unsubscribe from newsletter")
        fun `unsubscribe returns success for valid token`() {
            val email = "unsub-${System.currentTimeMillis()}@test.com"
            
            // First subscribe
            val subscribeRequest = mapOf(
                "email" to email,
                "name" to "Test User"
            )

            val subscribeResponse = webClient.post()
                .uri("/api/v1/content/newsletter/subscribe")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(subscribeRequest))
                .exchange()
                .expectStatus().isCreated
                .expectBody(String::class.java)
                .returnResult()
                .responseBody!!

            // Unsubscribe with both email and token (using email as token for simplicity)
            val unsubscribeRequest = mapOf(
                "email" to email,
                "token" to email
            )

            webClient.post()
                .uri("/api/v1/content/newsletter/unsubscribe")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(unsubscribeRequest))
                .exchange()
                .expectStatus().isOk
        }
    }

    @Nested
    @DisplayName("Contact Form")
    inner class ContactFormTests {

        @Test
        @DisplayName("Should submit contact form")
        fun `submit contact form returns success`() {
            val contactRequest = mapOf(
                "name" to "John Doe",
                "email" to "john@example.com",
                "subject" to "general",
                "message" to "This is a test message for the contact form.",
                "pnr" to null as String?
            )

            webClient.post()
                .uri("/api/v1/content/contact")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(contactRequest))
                .exchange()
                .expectStatus().isCreated
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
                .jsonPath("$.ticketNumber").isNotEmpty
                .jsonPath("$.message").isNotEmpty
        }

        @Test
        @DisplayName("Should submit contact form with PNR")
        fun `submit contact form with pnr reference`() {
            val contactRequest = mapOf(
                "name" to "Jane Doe",
                "email" to "jane@example.com",
                "subject" to "booking",
                "message" to "I have a question about my booking.",
                "pnr" to "ABC123"
            )

            webClient.post()
                .uri("/api/v1/content/contact")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(contactRequest))
                .exchange()
                .expectStatus().isCreated
                .expectBody()
                .jsonPath("$.success").isEqualTo(true)
        }

        @Test
        @DisplayName("Should reject empty message")
        fun `submit contact form with empty message returns bad request`() {
            val contactRequest = mapOf(
                "name" to "John Doe",
                "email" to "john@example.com",
                "subject" to "general",
                "message" to ""
            )

            webClient.post()
                .uri("/api/v1/content/contact")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(contactRequest))
                .exchange()
                .expectStatus().isBadRequest
        }

        @Test
        @DisplayName("Should reject invalid email in contact form")
        fun `submit contact form with invalid email returns bad request`() {
            val contactRequest = mapOf(
                "name" to "John Doe",
                "email" to "invalid-email",
                "subject" to "general",
                "message" to "Test message"
            )

            webClient.post()
                .uri("/api/v1/content/contact")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(objectMapper.writeValueAsString(contactRequest))
                .exchange()
                .expectStatus().isBadRequest
        }
    }

    @Nested
    @DisplayName("FAQ")
    inner class FAQTests {

        @Test
        @DisplayName("Should return FAQ list")
        fun `get faqs returns list of questions and answers`() {
            webClient.get()
                .uri("/api/v1/content/faq")
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.faqs").isArray
                .jsonPath("$.faqs[0].question").isNotEmpty
                .jsonPath("$.faqs[0].answer").isNotEmpty
                .jsonPath("$.faqs[0].category").isNotEmpty
        }

        @Test
        @DisplayName("Should filter FAQ by category")
        fun `get faqs by category returns filtered list`() {
            webClient.get()
                .uri("/api/v1/content/faq?category=booking")
                .exchange()
                .expectStatus().isOk
                .expectBody()
                .jsonPath("$.faqs").isArray
        }
    }
}
