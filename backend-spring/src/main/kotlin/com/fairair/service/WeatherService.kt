package com.fairair.service

import kotlinx.coroutines.reactor.awaitSingle
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.awaitBodyOrNull
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Service for fetching real weather data using Open-Meteo API (free, no API key required).
 */
@Service
class WeatherService {
    private val log = LoggerFactory.getLogger(WeatherService::class.java)
    
    private val webClient = WebClient.builder()
        .baseUrl("https://api.open-meteo.com")
        .build()
    
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }
    
    /**
     * City coordinates for weather lookup.
     */
    private val cityCoordinates = mapOf(
        // Saudi Arabia
        "JED" to Pair(21.4858, 39.1925),    // Jeddah
        "RUH" to Pair(24.7136, 46.6753),    // Riyadh
        "DMM" to Pair(26.4367, 50.1039),    // Dammam
        "MED" to Pair(24.5247, 39.5692),    // Madinah
        "AHB" to Pair(18.2164, 42.5053),    // Abha
        "GIZ" to Pair(16.9016, 42.5871),    // Jazan
        "TUU" to Pair(28.3654, 36.6189),    // Tabuk
        
        // UAE
        "DXB" to Pair(25.2048, 55.2708),    // Dubai
        "AUH" to Pair(24.4539, 54.3773),    // Abu Dhabi
        
        // Egypt
        "CAI" to Pair(30.0444, 31.2357),    // Cairo
        
        // Jordan
        "AMM" to Pair(31.9454, 35.9284),    // Amman
        
        // Turkey
        "IST" to Pair(41.0082, 28.9784),    // Istanbul
        
        // Bahrain
        "BAH" to Pair(26.2285, 50.5860),    // Bahrain
        
        // Kuwait
        "KWI" to Pair(29.3759, 47.9774),    // Kuwait
        
        // Oman
        "MCT" to Pair(23.5880, 58.3829),    // Muscat
        
        // Thailand
        "BKK" to Pair(13.7563, 100.5018),   // Bangkok
        
        // Maldives
        "MLE" to Pair(4.1755, 73.5093)      // Male
    )
    
    /**
     * City names for display.
     */
    private val cityNames = mapOf(
        "JED" to "Jeddah",
        "RUH" to "Riyadh",
        "DMM" to "Dammam",
        "MED" to "Madinah",
        "AHB" to "Abha",
        "GIZ" to "Jazan",
        "TUU" to "Tabuk",
        "DXB" to "Dubai",
        "AUH" to "Abu Dhabi",
        "CAI" to "Cairo",
        "AMM" to "Amman",
        "IST" to "Istanbul",
        "BAH" to "Bahrain",
        "KWI" to "Kuwait",
        "MCT" to "Muscat",
        "BKK" to "Bangkok",
        "MLE" to "Maldives"
    )
    
    /**
     * Fetch weather for a city by airport code.
     * Returns null if city not found or API fails.
     */
    suspend fun getWeather(airportCode: String): WeatherData? {
        val coords = cityCoordinates[airportCode.uppercase()] ?: return null
        val cityName = cityNames[airportCode.uppercase()] ?: airportCode
        
        return try {
            val response = webClient.get()
                .uri { uriBuilder ->
                    uriBuilder
                        .path("/v1/forecast")
                        .queryParam("latitude", coords.first)
                        .queryParam("longitude", coords.second)
                        .queryParam("current", "temperature_2m,weather_code")
                        .queryParam("timezone", "auto")
                        .build()
                }
                .retrieve()
                .bodyToMono(String::class.java)
                .awaitSingle()
            
            val apiResponse = json.decodeFromString<OpenMeteoResponse>(response)
            val current = apiResponse.current
            
            WeatherData(
                cityCode = airportCode.uppercase(),
                cityName = cityName,
                temperature = current.temperature_2m.toInt(),
                condition = weatherCodeToCondition(current.weather_code),
                description = weatherCodeToDescription(current.weather_code)
            )
        } catch (e: Exception) {
            log.warn("Failed to fetch weather for $airportCode: ${e.message}")
            // Return fallback data
            getFallbackWeather(airportCode)
        }
    }
    
    /**
     * Fetch weather for multiple cities.
     */
    suspend fun getWeatherForCities(airportCodes: List<String>): Map<String, WeatherData> {
        return airportCodes.mapNotNull { code ->
            getWeather(code)?.let { code.uppercase() to it }
        }.toMap()
    }
    
    /**
     * Convert Open-Meteo weather code to simple condition string.
     * See: https://open-meteo.com/en/docs#weathervariables
     */
    private fun weatherCodeToCondition(code: Int): String {
        return when (code) {
            0 -> "sunny"
            1, 2, 3 -> "partly_cloudy"
            45, 48 -> "foggy"
            51, 53, 55, 56, 57 -> "drizzle"
            61, 63, 65, 66, 67 -> "rainy"
            71, 73, 75, 77 -> "snowy"
            80, 81, 82 -> "rainy"
            85, 86 -> "snowy"
            95, 96, 99 -> "stormy"
            else -> "cloudy"
        }
    }
    
    /**
     * Convert weather code to human-readable description.
     */
    private fun weatherCodeToDescription(code: Int): String {
        return when (code) {
            0 -> "Clear skies"
            1 -> "Mainly clear"
            2 -> "Partly cloudy"
            3 -> "Overcast"
            45, 48 -> "Foggy"
            51 -> "Light drizzle"
            53 -> "Moderate drizzle"
            55 -> "Dense drizzle"
            56, 57 -> "Freezing drizzle"
            61 -> "Slight rain"
            63 -> "Moderate rain"
            65 -> "Heavy rain"
            66, 67 -> "Freezing rain"
            71 -> "Slight snow"
            73 -> "Moderate snow"
            75, 77 -> "Heavy snow"
            80, 81, 82 -> "Rain showers"
            85, 86 -> "Snow showers"
            95 -> "Thunderstorm"
            96, 99 -> "Thunderstorm with hail"
            else -> "Variable conditions"
        }
    }
    
    /**
     * Fallback weather data when API fails.
     */
    private fun getFallbackWeather(airportCode: String): WeatherData? {
        val cityName = cityNames[airportCode.uppercase()] ?: return null
        // Return reasonable defaults based on typical climate
        val (temp, condition) = when (airportCode.uppercase()) {
            "JED", "DMM", "DXB", "BAH", "KWI", "MCT", "MLE", "BKK" -> 30 to "sunny"
            "RUH", "MED" -> 28 to "sunny"
            "CAI", "AMM" -> 24 to "sunny"
            "IST" -> 15 to "partly_cloudy"
            "AHB" -> 20 to "sunny"
            else -> 25 to "sunny"
        }
        return WeatherData(
            cityCode = airportCode.uppercase(),
            cityName = cityName,
            temperature = temp,
            condition = condition,
            description = "Weather data temporarily unavailable"
        )
    }
}

/**
 * Weather data returned by our service.
 */
data class WeatherData(
    val cityCode: String,
    val cityName: String,
    val temperature: Int,
    val condition: String,
    val description: String
)

/**
 * Open-Meteo API response structure.
 */
@Serializable
private data class OpenMeteoResponse(
    val current: CurrentWeather
)

@Serializable
private data class CurrentWeather(
    val temperature_2m: Double,
    val weather_code: Int
)
