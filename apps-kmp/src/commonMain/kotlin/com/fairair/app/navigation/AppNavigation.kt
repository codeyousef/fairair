package com.fairair.app.navigation

/**
 * Booking flow steps for progress indicator.
 */
enum class BookingStep(val label: String, val index: Int) {
    SEARCH("Search", 0),
    SELECT("Select", 1),
    PASSENGERS("Passengers", 2),
    EXTRAS("Extras", 3),
    PAYMENT("Payment", 4),
    DONE("Done", 5);

    companion object {
        val totalSteps: Int = entries.size
    }
}

/**
 * Marker interfaces for type-safe screen identification.
 * Screens implement these to indicate their role in the booking flow.
 */
sealed interface AppScreen {
    interface Search : AppScreen
    interface Results : AppScreen
    interface PassengerInfo : AppScreen
    interface Ancillaries : AppScreen
    interface Payment : AppScreen
    interface Confirmation : AppScreen
    interface SavedBookings : AppScreen
    interface Settings : AppScreen
    interface CheckIn : AppScreen
    interface ManageBooking : AppScreen
    interface Membership : AppScreen
    interface Help : AppScreen
    interface Contact : AppScreen
    interface Profile : AppScreen
}
