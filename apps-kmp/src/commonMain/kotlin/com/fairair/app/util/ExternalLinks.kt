package com.fairair.app.util

/**
 * External service URLs and integrations.
 * These are partner services that flyadeal/FairAir integrates with.
 */
object ExternalLinks {
    
    /**
     * Simple helper to build a hotel search URL.
     * Uses default Booking.com search.
     */
    fun buildHotelSearchUrl(destinationCode: String? = null): String {
        return Hotels.searchUrl(destinationCode)
    }
    
    /**
     * Simple helper to build a car rental URL.
     * Uses default Rentalcars.com search.
     */
    fun buildCarRentalUrl(pickupLocation: String? = null): String {
        return CarRental.searchUrl(pickupLocation)
    }
    
    /**
     * Simple helper to get the contact page URL.
     */
    fun buildContactUrl(): String {
        return Support.CONTACT_US
    }
    
    /**
     * Partner hotel booking service.
     * Using Booking.com affiliate link structure.
     */
    object Hotels {
        const val BASE_URL = "https://www.booking.com"
        
        /**
         * Generate a hotel search URL for a specific destination.
         * @param destinationCode Airport/city code
         * @param checkInDate Check-in date (YYYY-MM-DD)
         * @param checkOutDate Check-out date (YYYY-MM-DD)
         * @param guests Number of guests
         */
        fun searchUrl(
            destinationCode: String? = null,
            checkInDate: String? = null,
            checkOutDate: String? = null,
            guests: Int = 1
        ): String {
            val params = buildList {
                destinationCode?.let { add("ss=$it") }
                checkInDate?.let { add("checkin=$it") }
                checkOutDate?.let { add("checkout=$it") }
                add("group_adults=$guests")
                add("aid=fairair_affiliate") // Affiliate tracking
            }
            return "$BASE_URL/searchresults.html?${params.joinToString("&")}"
        }
        
        /**
         * Get hotel search URL for Saudi destinations.
         */
        fun forDestination(destinationCode: String): String {
            val cityName = when (destinationCode) {
                "RUH" -> "Riyadh"
                "JED" -> "Jeddah"
                "DMM" -> "Dammam"
                "MED" -> "Medina"
                "TUU" -> "Tabuk"
                "AHB" -> "Abha"
                "GIZ" -> "Jazan"
                "ELQ" -> "Al+Qassim"
                "TIF" -> "Taif"
                "DXB" -> "Dubai"
                "BAH" -> "Bahrain"
                "KWI" -> "Kuwait"
                "DOH" -> "Doha"
                "MCT" -> "Muscat"
                "AMM" -> "Amman"
                "CAI" -> "Cairo"
                "IST" -> "Istanbul"
                else -> destinationCode
            }
            return "$BASE_URL/city/sa/$cityName.html?aid=fairair_affiliate"
        }
    }
    
    /**
     * Partner car rental service.
     * Using generic rental car affiliate structure.
     */
    object CarRental {
        const val BASE_URL = "https://www.rentalcars.com"
        
        /**
         * Generate a car rental search URL.
         * @param pickupLocation Pickup location code
         * @param pickupDate Pickup date (YYYY-MM-DD)
         * @param pickupTime Pickup time (HH:MM)
         * @param dropoffDate Dropoff date (YYYY-MM-DD)
         * @param dropoffTime Dropoff time (HH:MM)
         */
        fun searchUrl(
            pickupLocation: String? = null,
            pickupDate: String? = null,
            pickupTime: String = "10:00",
            dropoffDate: String? = null,
            dropoffTime: String = "10:00"
        ): String {
            val params = buildList {
                pickupLocation?.let { add("searchoption.pickup=$it") }
                pickupDate?.let { add("searchoption.puDay=${it.split("-").getOrNull(2)}") }
                pickupDate?.let { add("searchoption.puMonth=${it.split("-").getOrNull(1)}") }
                pickupDate?.let { add("searchoption.puYear=${it.split("-").getOrNull(0)}") }
                add("searchoption.puHour=${pickupTime.split(":").getOrNull(0)}")
                add("searchoption.puMinute=${pickupTime.split(":").getOrNull(1)}")
                dropoffDate?.let { add("searchoption.doDay=${it.split("-").getOrNull(2)}") }
                dropoffDate?.let { add("searchoption.doMonth=${it.split("-").getOrNull(1)}") }
                dropoffDate?.let { add("searchoption.doYear=${it.split("-").getOrNull(0)}") }
                add("searchoption.doHour=${dropoffTime.split(":").getOrNull(0)}")
                add("searchoption.doMinute=${dropoffTime.split(":").getOrNull(1)}")
                add("affiliateCode=fairair")
            }
            return "$BASE_URL/SearchResults.do?${params.joinToString("&")}"
        }
        
        /**
         * Get car rental URL for Saudi airports.
         */
        fun forAirport(airportCode: String): String {
            return "$BASE_URL/$airportCode?affiliateCode=fairair"
        }
    }
    
    /**
     * Travel insurance partner.
     */
    object Insurance {
        const val BASE_URL = "https://www.allianz-travel.com"
        
        fun quoteUrl(
            tripType: String = "single",
            destination: String? = null
        ): String {
            val params = buildList {
                add("tripType=$tripType")
                destination?.let { add("destination=$it") }
                add("partner=fairair")
            }
            return "$BASE_URL/quote?${params.joinToString("&")}"
        }
    }
    
    /**
     * Airport services (parking, lounges, fast track).
     */
    object AirportServices {
        const val LOUNGE_URL = "https://www.loungepass.com"
        const val PARKING_URL = "https://www.parkvia.com"
        const val FAST_TRACK_URL = "https://www.iconcierge.me"
        
        fun loungeFor(airportCode: String): String {
            return "$LOUNGE_URL/airport/$airportCode?partner=fairair"
        }
        
        fun parkingFor(airportCode: String): String {
            return "$PARKING_URL/airport/$airportCode?partner=fairair"
        }
    }
    
    /**
     * Social media and support links.
     */
    object Social {
        const val TWITTER = "https://twitter.com/fairair"
        const val INSTAGRAM = "https://instagram.com/fairair"
        const val FACEBOOK = "https://facebook.com/fairair"
        const val LINKEDIN = "https://linkedin.com/company/fairair"
        const val YOUTUBE = "https://youtube.com/fairair"
    }
    
    /**
     * Support and help links.
     */
    object Support {
        const val HELP_CENTER = "https://help.fairair.com"
        const val CONTACT_US = "https://fairair.com/contact"
        const val FEEDBACK = "https://fairair.com/feedback"
        const val PHONE = "tel:+966920000123"
        const val EMAIL = "mailto:support@fairair.com"
    }
}

/**
 * Platform-specific URL opener interface.
 * Implementation provided by each platform.
 */
expect object UrlOpener {
    /**
     * Opens a URL in the default browser or appropriate app.
     * @param url The URL to open
     * @return true if the URL was opened successfully
     */
    fun openUrl(url: String): Boolean
    
    /**
     * Opens the phone dialer with the given number.
     * @param phoneNumber The phone number (with tel: scheme)
     */
    fun openPhone(phoneNumber: String): Boolean
    
    /**
     * Opens the email client with the given address.
     * @param email The email address (with mailto: scheme)
     */
    fun openEmail(email: String): Boolean
}
