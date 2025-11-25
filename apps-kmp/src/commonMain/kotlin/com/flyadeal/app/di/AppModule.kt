package com.flyadeal.app.di

import com.flyadeal.app.api.FlyadealApiClient
import com.flyadeal.app.state.BookingFlowState
import com.flyadeal.app.ui.screens.search.SearchScreenModel
import com.flyadeal.app.ui.screens.results.ResultsScreenModel
import com.flyadeal.app.ui.screens.passengers.PassengerInfoScreenModel
import com.flyadeal.app.ui.screens.ancillaries.AncillariesScreenModel
import com.flyadeal.app.ui.screens.payment.PaymentScreenModel
import com.flyadeal.app.ui.screens.confirmation.ConfirmationScreenModel
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
 * Main application Koin module.
 * Contains all dependency definitions.
 */
val appModule = module {
    // Platform config - can be overridden by platform-specific modules
    single<PlatformConfig> { DefaultPlatformConfig() }

    // HTTP Client
    single {
        FlyadealApiClient.createHttpClient()
    }

    // API Client
    single {
        val config: PlatformConfig = get()
        FlyadealApiClient(
            baseUrl = config.apiBaseUrl,
            httpClient = get()
        )
    }

    // Booking flow state - shared across screens as singleton
    single { BookingFlowState() }
}

/**
 * ScreenModel module for Voyager screens.
 * Uses factory to create new instances per screen.
 */
val screenModelModule = module {
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
            bookingFlowState = get()
        )
    }
}

/**
 * Creates the list of Koin modules for the application.
 * Platform-specific modules can be added here.
 */
fun appModules(platformModule: org.koin.core.module.Module? = null): List<org.koin.core.module.Module> {
    return listOfNotNull(appModule, screenModelModule, platformModule)
}
