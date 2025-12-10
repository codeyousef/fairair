package com.fairair.app.di

import com.fairair.app.api.FairairApiClient
import com.fairair.app.persistence.LocalStorage
import com.fairair.app.state.BookingFlowState
import com.fairair.app.ui.screens.search.SearchScreenModel
import com.fairair.app.ui.screens.results.ResultsScreenModel
import com.fairair.app.ui.screens.passengers.PassengerInfoScreenModel
import com.fairair.app.ui.screens.ancillaries.AncillariesScreenModel
import com.fairair.app.ui.screens.payment.PaymentScreenModel
import com.fairair.app.ui.screens.confirmation.ConfirmationScreenModel
import com.fairair.app.ui.screens.saved.SavedBookingsScreenModel
import com.fairair.app.ui.screens.checkin.CheckInScreenModel
import com.fairair.app.ui.screens.manage.ManageBookingScreenModel
import com.fairair.app.ui.screens.membership.MembershipScreenModel
import com.fairair.app.ui.chat.ChatScreenModel
import org.koin.dsl.module

/**
 * Platform-specific configuration interface.
 * Each platform implements this to provide platform-specific values.
 */
interface PlatformConfig {
    /**
     * Base URL for the API.
     * For development: http://localhost:8080 or http://10.0.2.2:8080 (Android emulator)
     */
    val apiBaseUrl: String

    /**
     * Whether running in debug mode.
     */
    val isDebug: Boolean
}

/**
 * Default configuration for development.
 */
class DefaultPlatformConfig(
    override val apiBaseUrl: String = "http://localhost:8080",
    override val isDebug: Boolean = true
) : PlatformConfig

/**
 * Creates the main application Koin module.
 * Contains all dependency definitions.
 *
 * This is a function (not a val) to ensure it's not initialized before
 * any platform-specific polyfills are installed (e.g., crypto.randomUUID for Wasm).
 */
fun createAppModule() = module {
    // Platform config - can be overridden by platform-specific modules
    single<PlatformConfig> { DefaultPlatformConfig() }

    // HTTP Client
    single {
        FairairApiClient.createHttpClient()
    }

    // API Client
    single {
        val config: PlatformConfig = get()
        FairairApiClient(
            baseUrl = config.apiBaseUrl,
            httpClient = get()
        )
    }

    // Booking flow state - shared across screens as singleton
    single { BookingFlowState() }

    // Local storage for persistence
    single { LocalStorage() }
}

/**
 * Creates the ScreenModel module for Voyager screens.
 * Uses factory to create new instances per screen.
 *
 * This is a function (not a val) to ensure it's not initialized before
 * any platform-specific polyfills are installed.
 */
fun createScreenModelModule() = module {
    // Search Screen Model
    factory {
        SearchScreenModel(
            apiClient = get(),
            bookingFlowState = get()
        )
    }

    // Results Screen Model
    factory {
        ResultsScreenModel(
            bookingFlowState = get()
        )
    }

    // Passenger Info Screen Model
    factory {
        PassengerInfoScreenModel(
            bookingFlowState = get()
        )
    }

    // Ancillaries Screen Model
    factory {
        AncillariesScreenModel(
            bookingFlowState = get()
        )
    }

    // Payment Screen Model
    factory {
        PaymentScreenModel(
            apiClient = get(),
            bookingFlowState = get()
        )
    }

    // Confirmation Screen Model
    factory {
        ConfirmationScreenModel(
            bookingFlowState = get(),
            localStorage = get()
        )
    }

    // Saved Bookings Screen Model
    factory {
        SavedBookingsScreenModel(
            localStorage = get(),
            apiClient = get()
        )
    }

    // Check-In Screen Model
    factory {
        CheckInScreenModel(
            apiClient = get()
        )
    }

    // Manage Booking Screen Model
    factory {
        ManageBookingScreenModel(
            apiClient = get()
        )
    }

    // Membership Screen Model
    factory {
        MembershipScreenModel(
            apiClient = get(),
            localStorage = get()
        )
    }

    // Chat Screen Model for Pilot AI assistant
    // Singleton to maintain conversation state across screen changes
    single {
        ChatScreenModel(
            apiClient = get(),
            localStorage = get(),
            bookingFlowState = get()
        )
    }

    // Profile Screen Model for saved travelers and payment methods
    factory {
        com.fairair.app.ui.screens.profile.ProfileScreenModel(
            apiClient = get(),
            localStorage = get()
        )
    }
}

/**
 * Creates the list of Koin modules for the application.
 * Platform-specific modules can be added here.
 *
 * IMPORTANT: This must only be called after any required polyfills are installed
 * (e.g., crypto.randomUUID for Wasm).
 */
fun appModules(platformModule: org.koin.core.module.Module? = null): List<org.koin.core.module.Module> {
    return listOfNotNull(createAppModule(), createScreenModelModule(), platformModule)
}
