package com.fairair.ai.booking.exception

import com.fairair.koog.KoogException

class EntityExtractionException(message: String) : KoogException(message)
class RouteValidationException(message: String) : KoogException(message)
class ToolValidationException(message: String) : KoogException(message)
