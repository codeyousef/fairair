package com.fairair.controller

import com.fairair.contract.api.ApiRoutes
import com.fairair.service.ContentService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

/**
 * REST controller for content endpoints.
 * Handles help center, destinations, newsletter, and contact form.
 */
@RestController
@RequestMapping(ApiRoutes.Content.BASE)
class ContentController(
    private val contentService: ContentService
) {
    private val log = LoggerFactory.getLogger(ContentController::class.java)

    // ========================================================================
    // Help Center Endpoints
    // ========================================================================

    /**
     * GET /api/v1/content/help/categories
     *
     * Returns all help center categories.
     */
    @GetMapping("/help/categories")
    suspend fun getHelpCategories(): ResponseEntity<Map<String, Any>> {
        log.info("GET /content/help/categories")
        val categories = contentService.getHelpCategories()
        return ResponseEntity.ok(mapOf("categories" to categories))
    }

    /**
     * GET /api/v1/content/help/categories/{categoryId}
     *
     * Returns articles in a category.
     */
    @GetMapping("/help/categories/{categoryId}")
    suspend fun getArticlesInCategory(
        @PathVariable categoryId: String
    ): ResponseEntity<Any> {
        log.info("GET /content/help/categories/$categoryId")

        return try {
            val result = contentService.getArticlesInCategory(categoryId)
            ResponseEntity.ok(result)
        } catch (e: CategoryNotFoundException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ContentErrorResponse("CATEGORY_NOT_FOUND", "Category $categoryId not found"))
        }
    }

    /**
     * GET /api/v1/content/help/articles/{articleId}
     *
     * Returns a single article.
     */
    @GetMapping("/help/articles/{articleId}")
    suspend fun getArticle(
        @PathVariable articleId: String
    ): ResponseEntity<Any> {
        log.info("GET /content/help/articles/$articleId")

        return try {
            val article = contentService.getArticle(articleId)
            ResponseEntity.ok(article)
        } catch (e: ArticleNotFoundException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ContentErrorResponse("ARTICLE_NOT_FOUND", "Article $articleId not found"))
        }
    }

    /**
     * GET /api/v1/content/help/search
     *
     * Searches help articles.
     */
    @GetMapping("/help/search")
    suspend fun searchHelp(
        @RequestParam("q") query: String,
        @RequestParam(defaultValue = "en") lang: String
    ): ResponseEntity<Map<String, Any>> {
        log.info("GET /content/help/search?q=$query&lang=$lang")
        val results = contentService.searchArticles(query, lang)
        return ResponseEntity.ok(mapOf("query" to query, "results" to results))
    }

    // ========================================================================
    // Destination Endpoints
    // ========================================================================

    /**
     * GET /api/v1/content/destinations
     *
     * Returns all destinations.
     */
    @GetMapping("/destinations")
    suspend fun getDestinations(): ResponseEntity<Map<String, Any>> {
        log.info("GET /content/destinations")
        val destinations = contentService.getDestinations()
        return ResponseEntity.ok(mapOf("destinations" to destinations))
    }

    /**
     * GET /api/v1/content/destinations/{code}
     *
     * Returns destination details.
     */
    @GetMapping("/destinations/{code}")
    suspend fun getDestination(
        @PathVariable code: String
    ): ResponseEntity<Any> {
        log.info("GET /content/destinations/$code")

        return try {
            val destination = contentService.getDestinationDetails(code.uppercase())
            ResponseEntity.ok(destination)
        } catch (e: DestinationNotFoundException) {
            ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ContentErrorResponse("DESTINATION_NOT_FOUND", "Destination $code not found"))
        }
    }

    // ========================================================================
    // Newsletter Endpoints
    // ========================================================================

    /**
     * POST /api/v1/content/newsletter/subscribe
     *
     * Subscribes to newsletter.
     */
    @PostMapping("/newsletter/subscribe")
    suspend fun subscribeNewsletter(
        @RequestBody request: NewsletterSubscribeRequest
    ): ResponseEntity<Any> {
        log.info("POST /content/newsletter/subscribe: ${request.email}")

        return try {
            val response = contentService.subscribeToNewsletter(
                email = request.email,
                name = request.name,
                preferences = request.preferences
            )
            ResponseEntity.status(HttpStatus.CREATED).body(response)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ContentErrorResponse("VALIDATION_ERROR", e.message ?: "Invalid email"))
        } catch (e: NewsletterAlreadySubscribedException) {
            ResponseEntity.status(HttpStatus.CONFLICT)
                .body(ContentErrorResponse("ALREADY_SUBSCRIBED", "This email is already subscribed"))
        }
    }

    /**
     * POST /api/v1/content/newsletter/unsubscribe
     *
     * Unsubscribes from newsletter.
     */
    @PostMapping("/newsletter/unsubscribe")
    suspend fun unsubscribeNewsletter(
        @RequestBody request: NewsletterUnsubscribeRequest
    ): ResponseEntity<Any> {
        log.info("POST /content/newsletter/unsubscribe: ${request.email}")

        return try {
            contentService.unsubscribeFromNewsletter(request.email, request.token)
            ResponseEntity.ok(mapOf("message" to "Successfully unsubscribed"))
        } catch (e: InvalidTokenException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ContentErrorResponse("INVALID_TOKEN", "Invalid or expired unsubscribe token"))
        }
    }

    // ========================================================================
    // Contact Form Endpoints
    // ========================================================================

    /**
     * POST /api/v1/content/contact
     *
     * Submits contact form.
     */
    @PostMapping("/contact")
    suspend fun submitContactForm(
        @RequestBody request: ContactFormRequest
    ): ResponseEntity<Any> {
        log.info("POST /content/contact: ${request.subject} from ${request.email}")

        return try {
            val response = contentService.submitContactForm(
                name = request.name,
                email = request.email,
                phone = request.phone,
                subject = request.subject,
                category = request.category ?: "general",
                message = request.message,
                bookingReference = request.pnr
            )
            ResponseEntity.status(HttpStatus.CREATED).body(response)
        } catch (e: IllegalArgumentException) {
            ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ContentErrorResponse("VALIDATION_ERROR", e.message ?: "Invalid request"))
        }
    }

    // ========================================================================
    // FAQ Endpoints
    // ========================================================================

    /**
     * GET /api/v1/content/faq
     *
     * Returns FAQ list, optionally filtered by category.
     */
    @GetMapping("/faq")
    suspend fun getFAQs(
        @RequestParam(required = false) category: String?
    ): ResponseEntity<Map<String, Any>> {
        log.info("GET /content/faq?category=$category")
        val faqs = contentService.getFAQs(category)
        return ResponseEntity.ok(mapOf("faqs" to faqs))
    }
}

// Request DTOs
data class NewsletterSubscribeRequest(
    val email: String,
    val name: String?,
    val preferences: List<String>? = null
)

data class NewsletterUnsubscribeRequest(
    val email: String,
    val token: String
)

data class ContactFormRequest(
    val name: String,
    val email: String,
    val phone: String? = null,
    val subject: String,
    val category: String? = null,
    val message: String,
    val pnr: String? = null
)

// Response DTOs
data class ContentErrorResponse(
    val code: String,
    val message: String
)

data class CategoryArticlesResponse(
    val categoryId: String,
    val categoryName: String,
    val articles: List<HelpArticle>
)

data class HelpCategory(
    val id: String,
    val name: String,
    val nameAr: String,
    val description: String,
    val descriptionAr: String,
    val icon: String,
    val articleCount: Int
)

data class HelpArticle(
    val id: String,
    val categoryId: String,
    val title: String,
    val titleAr: String,
    val content: String,
    val contentAr: String,
    val summary: String,
    val summaryAr: String,
    val tags: List<String>,
    val helpful: Int = 0,
    val notHelpful: Int = 0
)

data class DestinationSummary(
    val code: String,
    val name: String,
    val nameAr: String,
    val country: String,
    val countryAr: String,
    val description: String,
    val descriptionAr: String,
    val imageUrl: String,
    val lowestFare: com.fairair.contract.model.Money?
)

data class DestinationDetail(
    val code: String,
    val name: String,
    val nameAr: String,
    val country: String,
    val countryAr: String,
    val description: String,
    val descriptionAr: String,
    val imageUrl: String,
    val galleryImages: List<String>,
    val highlights: List<DestinationHighlight>,
    val weather: WeatherInfo,
    val timezone: String,
    val currency: String,
    val language: String,
    val lowestFare: com.fairair.contract.model.Money?,
    val popularRoutes: List<PopularRoute>
)

data class DestinationHighlight(
    val title: String,
    val titleAr: String,
    val description: String,
    val descriptionAr: String,
    val icon: String
)

data class WeatherInfo(
    val averageTemp: Int,
    val description: String,
    val descriptionAr: String
)

data class PopularRoute(
    val origin: String,
    val originName: String,
    val lowestFare: com.fairair.contract.model.Money,
    val flightDuration: String
)

data class NewsletterSubscribeResponse(
    val success: Boolean,
    val email: String,
    val message: String,
    val subscriptionId: String
)

data class ContactFormResponse(
    val success: Boolean,
    val ticketNumber: String,
    val message: String,
    val estimatedResponseTime: String
)

// Exceptions
class CategoryNotFoundException(message: String) : RuntimeException(message)
class ArticleNotFoundException(message: String) : RuntimeException(message)
class DestinationNotFoundException(message: String) : RuntimeException(message)
class NewsletterAlreadySubscribedException(message: String) : RuntimeException(message)
class InvalidTokenException(message: String) : RuntimeException(message)
