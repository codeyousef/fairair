package com.fairair.exception

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.server.ServerWebInputException
import org.springframework.web.server.UnsupportedMediaTypeStatusException
import java.time.Instant

/**
 * Standard error response format for all API errors.
 * Matches the Quarkus implementation for frontend compatibility.
 */
data class ErrorResponse(
    val error: String,
    val message: String,
    val timestamp: String = Instant.now().toString(),
    val details: Map<String, Any>? = null
)

/**
 * Global exception handler for all controllers.
 *
 * Converts exceptions to proper HTTP responses with consistent
 * error format matching the Quarkus implementation.
 */
@ControllerAdvice
class GlobalExceptionHandler {

    private val log = LoggerFactory.getLogger(GlobalExceptionHandler::class.java)

    /**
     * Handle all FairairException subclasses.
     */
    @ExceptionHandler(FairairException::class)
    fun handleFairairException(ex: FairairException): ResponseEntity<ErrorResponse> {
        log.warn("Application error: ${ex.errorCode} - ${ex.message}")

        val errorResponse = ErrorResponse(
            error = ex.errorCode,
            message = ex.message,
            details = ex.details
        )

        return ResponseEntity
            .status(ex.httpStatus)
            .body(errorResponse)
    }

    /**
     * Handle IllegalArgumentException for validation errors.
     */
    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgumentException(ex: IllegalArgumentException): ResponseEntity<ErrorResponse> {
        log.warn("Validation error: ${ex.message}")

        val errorResponse = ErrorResponse(
            error = "VALIDATION_ERROR",
            message = ex.message ?: "Invalid request parameters"
        )

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(errorResponse)
    }

    /**
     * Handle Spring's ServerWebInputException for malformed requests.
     */
    @ExceptionHandler(ServerWebInputException::class)
    fun handleServerWebInputException(ex: ServerWebInputException): ResponseEntity<ErrorResponse> {
        log.warn("Input error: ${ex.message}")

        val errorResponse = ErrorResponse(
            error = "BAD_REQUEST",
            message = "Invalid request body"
        )

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .body(errorResponse)
    }
    
    /**
     * Handle unsupported media type errors.
     */
    @ExceptionHandler(UnsupportedMediaTypeStatusException::class)
    fun handleUnsupportedMediaTypeException(ex: UnsupportedMediaTypeStatusException): ResponseEntity<ErrorResponse> {
        log.warn("Unsupported media type: ${ex.message}")

        val errorResponse = ErrorResponse(
            error = "UNSUPPORTED_MEDIA_TYPE",
            message = "Content type not supported. Use application/json."
        )

        return ResponseEntity
            .status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
            .body(errorResponse)
    }

    /**
     * Handle all unhandled exceptions.
     * Ensures consistent error response format and prevents leaking internal details.
     */
    @ExceptionHandler(Exception::class)
    fun handleGenericException(ex: Exception): ResponseEntity<ErrorResponse> {
        log.error("Unhandled exception: ${ex.javaClass.simpleName}", ex)

        val errorResponse = ErrorResponse(
            error = "INTERNAL_SERVER_ERROR",
            message = "An unexpected error occurred. Please try again later."
        )

        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(errorResponse)
    }
}
