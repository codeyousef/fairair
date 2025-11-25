package com.flyadeal.exception

import jakarta.ws.rs.WebApplicationException
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.ExceptionMapper
import jakarta.ws.rs.ext.Provider
import org.jboss.logging.Logger
import java.time.Instant

/**
 * Standard error response format for all API errors.
 */
data class ErrorResponse(
    val error: String,
    val message: String,
    val timestamp: String = Instant.now().toString(),
    val details: Map<String, Any>? = null
)

/**
 * Base exception for all application-specific errors.
 */
open class FlyadealException(
    message: String,
    val errorCode: String,
    val statusCode: Int = Response.Status.BAD_REQUEST.statusCode,
    val details: Map<String, Any>? = null,
    cause: Throwable? = null
) : RuntimeException(message, cause)

/**
 * Exception thrown when a requested resource is not found.
 */
class NotFoundException(
    message: String,
    details: Map<String, Any>? = null
) : FlyadealException(
    message = message,
    errorCode = "NOT_FOUND",
    statusCode = Response.Status.NOT_FOUND.statusCode,
    details = details
)

/**
 * Exception thrown when request validation fails.
 */
class ValidationException(
    message: String,
    details: Map<String, Any>? = null
) : FlyadealException(
    message = message,
    errorCode = "VALIDATION_ERROR",
    statusCode = Response.Status.BAD_REQUEST.statusCode,
    details = details
)

/**
 * Exception thrown when booking creation fails.
 */
class BookingException(
    message: String,
    details: Map<String, Any>? = null,
    cause: Throwable? = null
) : FlyadealException(
    message = message,
    errorCode = "BOOKING_ERROR",
    statusCode = 422, // Unprocessable Entity
    details = details,
    cause = cause
)

/**
 * Exception thrown when external service (Navitaire) fails.
 */
class ExternalServiceException(
    message: String,
    serviceName: String,
    cause: Throwable? = null
) : FlyadealException(
    message = message,
    errorCode = "EXTERNAL_SERVICE_ERROR",
    statusCode = Response.Status.SERVICE_UNAVAILABLE.statusCode,
    details = mapOf("service" to serviceName),
    cause = cause
)

/**
 * Global exception mapper for FlyadealException.
 * Converts application exceptions to proper HTTP responses.
 */
@Provider
class FlyadealExceptionMapper : ExceptionMapper<FlyadealException> {

    private val log: Logger = Logger.getLogger(FlyadealExceptionMapper::class.java)

    override fun toResponse(exception: FlyadealException): Response {
        log.warn("Application error: ${exception.errorCode} - ${exception.message}")

        val errorResponse = ErrorResponse(
            error = exception.errorCode,
            message = exception.message ?: "An error occurred",
            details = exception.details
        )

        return Response
            .status(exception.statusCode)
            .entity(errorResponse)
            .build()
    }
}

/**
 * Global exception mapper for WebApplicationException (JAX-RS exceptions).
 * Handles standard JAX-RS exceptions like 404, 405, etc.
 */
@Provider
class WebApplicationExceptionMapper : ExceptionMapper<WebApplicationException> {

    private val log: Logger = Logger.getLogger(WebApplicationExceptionMapper::class.java)

    override fun toResponse(exception: WebApplicationException): Response {
        val status = exception.response?.status ?: Response.Status.INTERNAL_SERVER_ERROR.statusCode

        log.warn("Web application error: $status - ${exception.message}")

        val errorCode = when (status) {
            400 -> "BAD_REQUEST"
            401 -> "UNAUTHORIZED"
            403 -> "FORBIDDEN"
            404 -> "NOT_FOUND"
            405 -> "METHOD_NOT_ALLOWED"
            415 -> "UNSUPPORTED_MEDIA_TYPE"
            else -> "HTTP_ERROR_$status"
        }

        val errorResponse = ErrorResponse(
            error = errorCode,
            message = exception.message ?: "Request failed"
        )

        return Response
            .status(status)
            .entity(errorResponse)
            .build()
    }
}

/**
 * Global exception mapper for IllegalArgumentException.
 * Converts validation errors to 400 Bad Request.
 */
@Provider
class IllegalArgumentExceptionMapper : ExceptionMapper<IllegalArgumentException> {

    private val log: Logger = Logger.getLogger(IllegalArgumentExceptionMapper::class.java)

    override fun toResponse(exception: IllegalArgumentException): Response {
        log.warn("Validation error: ${exception.message}")

        val errorResponse = ErrorResponse(
            error = "VALIDATION_ERROR",
            message = exception.message ?: "Invalid request parameters"
        )

        return Response
            .status(Response.Status.BAD_REQUEST)
            .entity(errorResponse)
            .build()
    }
}

/**
 * Global exception mapper for all unhandled exceptions.
 * Ensures consistent error response format and prevents leaking internal details.
 */
@Provider
class GenericExceptionMapper : ExceptionMapper<Exception> {

    private val log: Logger = Logger.getLogger(GenericExceptionMapper::class.java)

    override fun toResponse(exception: Exception): Response {
        // Don't handle exceptions that have more specific mappers
        if (exception is FlyadealException || exception is WebApplicationException) {
            throw exception
        }

        log.error("Unhandled exception: ${exception.javaClass.simpleName}", exception)

        val errorResponse = ErrorResponse(
            error = "INTERNAL_SERVER_ERROR",
            message = "An unexpected error occurred. Please try again later."
        )

        return Response
            .status(Response.Status.INTERNAL_SERVER_ERROR)
            .entity(errorResponse)
            .build()
    }
}
