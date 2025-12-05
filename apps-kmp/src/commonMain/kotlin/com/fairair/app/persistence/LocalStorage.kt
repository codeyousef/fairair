package com.fairair.app.persistence

import com.fairair.contract.dto.BookingConfirmationDto
import com.fairair.contract.dto.UserInfoDto
import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Expect function to create platform-specific Settings instance.
 */
expect fun createSettings(): Settings

/**
 * Local storage for persisting bookings and app preferences.
 * Uses multiplatform-settings for cross-platform persistence.
 */
class LocalStorage(
    private val settings: Settings = createSettings()
) {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
        encodeDefaults = true
    }

    // Flow for saved bookings - updated when bookings change
    private val _savedBookingsFlow = MutableStateFlow<List<BookingConfirmationDto>>(emptyList())

    // Flow for current language
    private val _currentLanguageFlow = MutableStateFlow("en")

    // Flow for search history
    private val _searchHistoryFlow = MutableStateFlow<List<SearchHistoryEntry>>(emptyList())

    init {
        // Initialize flows with stored values
        refreshFlows()
    }

    private fun refreshFlows() {
        _savedBookingsFlow.value = getSavedBookingsListInternal()
        _currentLanguageFlow.value = settings.getString(KEY_CURRENT_LANGUAGE, "en")
        _searchHistoryFlow.value = getSearchHistoryInternal()
    }

    companion object {
        private const val KEY_SAVED_BOOKINGS = "saved_bookings"
        private const val KEY_CURRENT_LANGUAGE = "current_language"
        private const val KEY_ROUTE_CACHE = "route_cache"
        private const val KEY_ROUTE_CACHE_TIMESTAMP = "route_cache_timestamp"
        private const val KEY_SEARCH_HISTORY = "search_history"
        private const val KEY_AUTH_TOKEN = "auth_token"
        private const val KEY_CURRENT_USER = "current_user"
        private const val ROUTE_CACHE_TTL_MS = 24 * 60 * 60 * 1000L // 24 hours
        private const val MAX_SEARCH_HISTORY = 10
    }

    // ============ Authentication ============

    /**
     * Saves the auth token to local storage.
     */
    fun saveAuthToken(token: String) {
        settings.putString(KEY_AUTH_TOKEN, token)
    }

    /**
     * Gets the stored auth token.
     * Returns null if not logged in.
     */
    fun getAuthToken(): String? {
        val token = settings.getString(KEY_AUTH_TOKEN, "")
        return token.ifEmpty { null }
    }

    /**
     * Saves the current user to local storage.
     */
    fun saveCurrentUser(user: UserInfoDto) {
        val jsonString = json.encodeToString(user)
        settings.putString(KEY_CURRENT_USER, jsonString)
    }

    /**
     * Gets the stored current user.
     * Returns null if not logged in.
     */
    fun getCurrentUser(): UserInfoDto? {
        val jsonString = settings.getString(KEY_CURRENT_USER, "")
        if (jsonString.isEmpty()) return null
        return try {
            json.decodeFromString(jsonString)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Clears auth data (logout).
     */
    fun clearAuth() {
        settings.remove(KEY_AUTH_TOKEN)
        settings.remove(KEY_CURRENT_USER)
    }

    // ============ Saved Bookings ============

    /**
     * Saves a booking to local storage.
     * Adds to the existing list without duplicates (by PNR).
     */
    suspend fun saveBooking(booking: BookingConfirmationDto) {
        val current = getSavedBookingsListInternal()
        val updated = current.filterNot { it.pnr == booking.pnr } + booking
        val jsonString = json.encodeToString(updated)
        settings.putString(KEY_SAVED_BOOKINGS, jsonString)
        _savedBookingsFlow.value = updated
    }

    /**
     * Gets all saved bookings as a Flow for reactive updates.
     */
    fun getSavedBookingsFlow(): Flow<List<BookingConfirmationDto>> = _savedBookingsFlow

    /**
     * Gets all saved bookings synchronously.
     */
    suspend fun getSavedBookingsList(): List<BookingConfirmationDto> {
        return getSavedBookingsListInternal()
    }

    private fun getSavedBookingsListInternal(): List<BookingConfirmationDto> {
        val jsonString = settings.getString(KEY_SAVED_BOOKINGS, "[]")
        return try {
            json.decodeFromString(jsonString)
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Gets a specific booking by PNR.
     */
    suspend fun getBookingByPnr(pnr: String): BookingConfirmationDto? {
        return getSavedBookingsListInternal().find { it.pnr == pnr }
    }

    /**
     * Deletes a booking by PNR.
     */
    suspend fun deleteBooking(pnr: String) {
        val current = getSavedBookingsListInternal()
        val updated = current.filterNot { it.pnr == pnr }
        val jsonString = json.encodeToString(updated)
        settings.putString(KEY_SAVED_BOOKINGS, jsonString)
        _savedBookingsFlow.value = updated
    }

    /**
     * Clears all saved bookings.
     */
    suspend fun clearAllBookings() {
        settings.putString(KEY_SAVED_BOOKINGS, "[]")
        _savedBookingsFlow.value = emptyList()
    }

    // ============ Language Preference ============

    /**
     * Gets the current language setting.
     * Returns "en" (English) by default.
     */
    suspend fun getCurrentLanguage(): String {
        return settings.getString(KEY_CURRENT_LANGUAGE, "en")
    }

    /**
     * Gets the current language as a Flow for reactive updates.
     */
    fun getCurrentLanguageFlow(): Flow<String> = _currentLanguageFlow

    /**
     * Sets the current language.
     * @param language "en" for English, "ar" for Arabic
     */
    suspend fun setCurrentLanguage(language: String) {
        require(language in listOf("en", "ar")) { "Language must be 'en' or 'ar'" }
        settings.putString(KEY_CURRENT_LANGUAGE, language)
        _currentLanguageFlow.value = language
    }

    // ============ Route Cache ============

    /**
     * Cached route map data for offline access.
     */
    data class CachedRoutes(
        val routes: Map<String, List<String>>,
        val timestamp: Long
    )

    /**
     * Saves the route map to cache.
     */
    suspend fun cacheRoutes(routes: Map<String, List<String>>) {
        val jsonString = json.encodeToString(routes)
        settings.putString(KEY_ROUTE_CACHE, jsonString)
        settings.putLong(KEY_ROUTE_CACHE_TIMESTAMP, currentTimeMillis())
    }

    /**
     * Gets cached routes if still valid (within TTL).
     * Returns null if cache is expired or doesn't exist.
     */
    suspend fun getCachedRoutes(): CachedRoutes? {
        val jsonString = settings.getString(KEY_ROUTE_CACHE, "")
        if (jsonString.isEmpty()) return null

        val timestamp = settings.getLong(KEY_ROUTE_CACHE_TIMESTAMP, 0L)
        val now = currentTimeMillis()

        if (now - timestamp > ROUTE_CACHE_TTL_MS) {
            return null // Cache expired
        }

        return try {
            val routes = json.decodeFromString<Map<String, List<String>>>(jsonString)
            CachedRoutes(routes, timestamp)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Forces refresh of route cache (clears timestamp).
     */
    suspend fun invalidateRouteCache() {
        settings.putLong(KEY_ROUTE_CACHE_TIMESTAMP, 0L)
    }

    // ============ Search History ============

    /**
     * Search history entry for quick re-search.
     */
    @kotlinx.serialization.Serializable
    data class SearchHistoryEntry(
        val origin: String,
        val destination: String,
        val departureDate: String,
        val timestamp: Long
    )

    /**
     * Adds a search to history (keeps last 10).
     */
    suspend fun addSearchToHistory(origin: String, destination: String, departureDate: String) {
        val entry = SearchHistoryEntry(
            origin = origin,
            destination = destination,
            departureDate = departureDate,
            timestamp = currentTimeMillis()
        )

        val current = getSearchHistoryInternal()
        // Remove duplicates and add new entry
        val updated = (listOf(entry) + current.filterNot {
            it.origin == origin && it.destination == destination
        }).take(MAX_SEARCH_HISTORY)

        val jsonString = json.encodeToString(updated)
        settings.putString(KEY_SEARCH_HISTORY, jsonString)
        _searchHistoryFlow.value = updated
    }

    /**
     * Gets search history.
     */
    suspend fun getSearchHistory(): List<SearchHistoryEntry> {
        return getSearchHistoryInternal()
    }

    private fun getSearchHistoryInternal(): List<SearchHistoryEntry> {
        val jsonString = settings.getString(KEY_SEARCH_HISTORY, "[]")
        return try {
            json.decodeFromString(jsonString)
        } catch (e: Exception) {
            emptyList()
        }
    }

    /**
     * Gets search history as a Flow.
     */
    fun getSearchHistoryFlow(): Flow<List<SearchHistoryEntry>> = _searchHistoryFlow

    /**
     * Clears search history.
     */
    suspend fun clearSearchHistory() {
        settings.putString(KEY_SEARCH_HISTORY, "[]")
        _searchHistoryFlow.value = emptyList()
    }

    // ============ Utility ============

    /**
     * Clears all stored data.
     */
    suspend fun clearAll() {
        clearAllBookings()
        clearSearchHistory()
        invalidateRouteCache()
        settings.putString(KEY_CURRENT_LANGUAGE, "en")
        _currentLanguageFlow.value = "en"
    }
}

/**
 * Gets current time in milliseconds - platform-specific implementation.
 */
expect fun currentTimeMillis(): Long
