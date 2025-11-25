package com.flyadeal.app.util

import com.flyadeal.app.api.ApiResult

/**
 * Maps API error codes and messages to user-friendly messages.
 * Provides consistent error messaging across the app.
 */
object ErrorMessageMapper {

    /**
     * Maps an API error result to a user-friendly message.
     */
    fun mapError(error: ApiResult.Error): UserFriendlyError {
        return when {
            // Network connectivity errors
            error.code == "NETWORK_ERROR" -> UserFriendlyError(
                title = "Connection Error",
                message = "Unable to connect to the server. Please check your internet connection and try again.",
                isRetryable = true,
                suggestion = "Check your WiFi or mobile data connection"
            )

            error.code == "TIMEOUT" -> UserFriendlyError(
                title = "Request Timed Out",
                message = "The server took too long to respond. Please try again.",
                isRetryable = true,
                suggestion = "This might be due to a slow connection"
            )

            // Server errors (5xx)
            error.code == "SERVER_ERROR" || error.code.startsWith("SERVER_ERROR_") -> UserFriendlyError(
                title = "Server Error",
                message = "Something went wrong on our end. We're working to fix it.",
                isRetryable = true,
                suggestion = "Please try again in a few moments"
            )

            // Client errors (4xx)
            error.code == "HTTP_400" || error.code == "VALIDATION_ERROR" -> UserFriendlyError(
                title = "Invalid Request",
                message = parseValidationError(error.message),
                isRetryable = false,
                suggestion = "Please check your input and try again"
            )

            error.code == "HTTP_401" || error.code == "UNAUTHORIZED" -> UserFriendlyError(
                title = "Session Expired",
                message = "Your session has expired. Please start a new search.",
                isRetryable = false,
                suggestion = "Return to the search page to continue"
            )

            error.code == "HTTP_403" || error.code == "FORBIDDEN" -> UserFriendlyError(
                title = "Access Denied",
                message = "You don't have permission to perform this action.",
                isRetryable = false,
                suggestion = null
            )

            error.code == "HTTP_404" || error.code == "NOT_FOUND" -> UserFriendlyError(
                title = "Not Found",
                message = "The requested resource was not found.",
                isRetryable = false,
                suggestion = "The flight or booking may no longer be available"
            )

            error.code == "HTTP_429" -> UserFriendlyError(
                title = "Too Many Requests",
                message = "You're making requests too quickly. Please wait a moment.",
                isRetryable = true,
                suggestion = "Wait 30 seconds before trying again"
            )

            // Booking-specific errors
            error.code == "SEARCH_EXPIRED" -> UserFriendlyError(
                title = "Search Expired",
                message = "Your search session has expired. Please search again for current availability.",
                isRetryable = false,
                suggestion = "Flight prices and availability may have changed"
            )

            error.code == "FLIGHT_NOT_FOUND" -> UserFriendlyError(
                title = "Flight Unavailable",
                message = "The selected flight is no longer available.",
                isRetryable = false,
                suggestion = "Please search again for available flights"
            )

            error.code == "FARE_NOT_FOUND" -> UserFriendlyError(
                title = "Fare Unavailable",
                message = "The selected fare is no longer available at this price.",
                isRetryable = false,
                suggestion = "Please select a different fare option"
            )

            error.code == "BOOKING_ERROR" || error.code == "BOOKING_FAILED" -> UserFriendlyError(
                title = "Booking Failed",
                message = "We couldn't complete your booking. No payment has been charged.",
                isRetryable = true,
                suggestion = "Please try again or contact support if the problem persists"
            )

            error.code == "PAYMENT_ERROR" -> UserFriendlyError(
                title = "Payment Failed",
                message = parsePaymentError(error.message),
                isRetryable = true,
                suggestion = "Please check your card details and try again"
            )

            error.code == "INVALID_ROUTE" -> UserFriendlyError(
                title = "Invalid Route",
                message = "This route is not available. Please select a different destination.",
                isRetryable = false,
                suggestion = "Check our available destinations"
            )

            // Parse errors
            error.code == "ERROR" && error.message.contains("parse", ignoreCase = true) -> UserFriendlyError(
                title = "Data Error",
                message = "There was a problem processing the response. Please try again.",
                isRetryable = true,
                suggestion = null
            )

            // Generic fallback
            else -> UserFriendlyError(
                title = "Something Went Wrong",
                message = sanitizeErrorMessage(error.message),
                isRetryable = error.isRetryable,
                suggestion = if (error.isRetryable) "Please try again" else null
            )
        }
    }

    /**
     * Parses validation error messages to make them more user-friendly.
     */
    private fun parseValidationError(message: String): String {
        return when {
            message.contains("passenger", ignoreCase = true) && message.contains("required", ignoreCase = true) ->
                "Please enter all required passenger information."
            message.contains("date", ignoreCase = true) && message.contains("invalid", ignoreCase = true) ->
                "Please select a valid date."
            message.contains("email", ignoreCase = true) ->
                "Please enter a valid email address."
            message.contains("phone", ignoreCase = true) ->
                "Please enter a valid phone number."
            message.contains("document", ignoreCase = true) ->
                "Please enter valid document information."
            else -> sanitizeErrorMessage(message)
        }
    }

    /**
     * Parses payment error messages to make them more user-friendly.
     */
    private fun parsePaymentError(message: String): String {
        return when {
            message.contains("declined", ignoreCase = true) ->
                "Your card was declined. Please try a different card."
            message.contains("insufficient", ignoreCase = true) ->
                "Insufficient funds. Please try a different card."
            message.contains("expired", ignoreCase = true) ->
                "Your card has expired. Please use a valid card."
            message.contains("cvv", ignoreCase = true) || message.contains("cvc", ignoreCase = true) ->
                "Invalid security code. Please check and try again."
            message.contains("number", ignoreCase = true) ->
                "Invalid card number. Please check and try again."
            else -> "Payment could not be processed. Please check your card details."
        }
    }

    /**
     * Sanitizes raw error messages to remove technical details.
     */
    private fun sanitizeErrorMessage(message: String): String {
        // Remove stack traces, technical paths, etc.
        val sanitized = message
            .replace(Regex("\\bat\\b.*\\(.*\\)"), "") // Remove stack trace lines
            .replace(Regex("Exception:?\\s*"), "") // Remove exception prefixes
            .replace(Regex("java\\.[a-z.]+"), "") // Remove Java package names
            .replace(Regex("kotlin\\.[a-z.]+"), "") // Remove Kotlin package names
            .replace(Regex("io\\.ktor\\.[a-z.]+"), "") // Remove Ktor package names
            .replace(Regex("\\s+"), " ") // Normalize whitespace
            .trim()

        // If the message is too technical or empty, return a generic message
        return if (sanitized.length < 10 || sanitized.contains("null", ignoreCase = true)) {
            "An unexpected error occurred. Please try again."
        } else {
            sanitized.take(200) // Limit message length
        }
    }

    /**
     * Gets a short error message suitable for snackbars or toasts.
     */
    fun getShortMessage(error: ApiResult.Error): String {
        val mapped = mapError(error)
        return "${mapped.title}: ${mapped.message.take(50)}${if (mapped.message.length > 50) "..." else ""}"
    }
}

/**
 * Represents a user-friendly error with context.
 */
data class UserFriendlyError(
    val title: String,
    val message: String,
    val isRetryable: Boolean,
    val suggestion: String? = null
) {
    /**
     * Returns the full error message including suggestion.
     */
    val fullMessage: String
        get() = if (suggestion != null) {
            "$message\n\n$suggestion"
        } else {
            message
        }

    /**
     * Returns a short display string.
     */
    val displayMessage: String
        get() = message
}

/**
 * Extension function to convert ApiResult.Error to user-friendly error.
 */
fun ApiResult.Error.toUserFriendly(): UserFriendlyError {
    return ErrorMessageMapper.mapError(this)
}

/**
 * Extension function to get a display message from ApiResult.Error.
 */
fun ApiResult.Error.toDisplayMessage(): String {
    return ErrorMessageMapper.mapError(this).displayMessage
}
