package com.fairair.contract.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

/**
 * Help center category.
 */
@Serializable
data class HelpCategory(
    val id: String,
    val name: String,
    val nameArabic: String,
    val description: String,
    val descriptionArabic: String,
    val icon: String,
    val articleCount: Int,
    val displayOrder: Int
)

/**
 * Help article.
 */
@Serializable
data class HelpArticle(
    val id: String,
    val categoryId: String,
    val title: String,
    val titleArabic: String,
    val summary: String,
    val summaryArabic: String,
    val content: String,
    val contentArabic: String,
    val tags: List<String>,
    val relatedArticleIds: List<String>,
    val lastUpdated: Instant,
    val helpfulCount: Int,
    val notHelpfulCount: Int
)

/**
 * Destination detail page content.
 */
@Serializable
data class DestinationDetail(
    val code: AirportCode,
    val city: String,
    val cityArabic: String,
    val country: String,
    val countryArabic: String,
    val airport: String,
    val airportArabic: String,
    val description: String,
    val descriptionArabic: String,
    val heroImageUrl: String,
    val galleryImages: List<String>,
    val highlights: List<DestinationHighlight>,
    val travelInfo: TravelInfo,
    val flightsPerWeek: Int,
    val lowestFare: Money?,
    val popularRoutes: List<PopularRoute>
)

/**
 * Destination highlight/attraction.
 */
@Serializable
data class DestinationHighlight(
    val title: String,
    val titleArabic: String,
    val description: String,
    val descriptionArabic: String,
    val imageUrl: String?,
    val category: HighlightCategory
)

/**
 * Highlight categories.
 */
@Serializable
enum class HighlightCategory {
    LANDMARK,
    FOOD,
    CULTURE,
    NATURE,
    SHOPPING,
    ENTERTAINMENT,
    HISTORY,
    BEACH,
    ADVENTURE
}

/**
 * Travel information for a destination.
 */
@Serializable
data class TravelInfo(
    val timezone: String,
    val currency: String,
    val language: String,
    val visaRequired: Boolean,
    val visaInfo: String?,
    val bestTimeToVisit: String,
    val averageTemperature: String,
    val flightDuration: String
)

/**
 * Popular route to a destination.
 */
@Serializable
data class PopularRoute(
    val origin: AirportCode,
    val originCity: String,
    val lowestFare: Money,
    val flightDurationMinutes: Int,
    val flightsPerDay: Int
)

/**
 * Destination summary for listings.
 */
@Serializable
data class DestinationSummary(
    val code: AirportCode,
    val city: String,
    val cityArabic: String,
    val country: String,
    val countryArabic: String,
    val imageUrl: String,
    val lowestFare: Money?,
    val isPopular: Boolean,
    val isNew: Boolean
)

/**
 * Newsletter subscription request.
 */
@Serializable
data class NewsletterSubscribeRequest(
    val email: String,
    val firstName: String?,
    val preferredLanguage: String,
    val interests: List<NewsletterInterest>
) {
    init {
        require(email.contains("@") && email.contains(".")) {
            "Invalid email format: '$email'"
        }
    }
}

/**
 * Newsletter interest categories.
 */
@Serializable
enum class NewsletterInterest {
    DEALS,
    NEW_ROUTES,
    TRAVEL_TIPS,
    MEMBERSHIP,
    DESTINATION_GUIDES
}

/**
 * Newsletter subscription response.
 */
@Serializable
data class NewsletterSubscribeResponse(
    val success: Boolean,
    val message: String,
    val subscriptionId: String?
)

/**
 * Contact form submission.
 */
@Serializable
data class ContactFormRequest(
    val category: ContactCategory,
    val pnr: String?,
    val fullName: String,
    val email: String,
    val phone: String?,
    val subject: String,
    val message: String,
    val attachmentUrls: List<String>
)

/**
 * Contact form categories.
 */
@Serializable
enum class ContactCategory {
    BOOKING_INQUIRY,
    REFUND_REQUEST,
    COMPLAINT,
    FEEDBACK,
    LOST_BAGGAGE,
    SPECIAL_ASSISTANCE,
    GROUP_BOOKING,
    CHARTER_REQUEST,
    PARTNERSHIP,
    MEDIA_INQUIRY,
    OTHER
}

/**
 * Contact form response.
 */
@Serializable
data class ContactFormResponse(
    val success: Boolean,
    val ticketNumber: String,
    val estimatedResponseTime: String,
    val message: String
)
