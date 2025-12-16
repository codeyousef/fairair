package com.fairair.ai.booking.exception

import com.fairair.contract.dto.PendingBookingContext
import com.fairair.koog.KoogException

/**
 * Exception thrown when entity extraction is incomplete.
 * Carries partial data for conversation continuity.
 */
class EntityExtractionException(
    message: String,
    val pendingContext: PendingBookingContext? = null
) : KoogException(message)

class RouteValidationException(message: String) : KoogException(message)
class ToolValidationException(message: String) : KoogException(message)
