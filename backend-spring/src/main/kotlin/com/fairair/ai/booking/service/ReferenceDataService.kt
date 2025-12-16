package com.fairair.ai.booking.service

import com.fairair.domain.reference.AirportAliasRepository
import com.fairair.domain.reference.AirportRepository
import com.fairair.domain.reference.RouteRepository
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.reactive.asFlow
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

@Service
class ReferenceDataService(
    private val airportRepository: AirportRepository,
    private val airportAliasRepository: AirportAliasRepository,
    private val routeRepository: RouteRepository
) {
    private val logger = LoggerFactory.getLogger(ReferenceDataService::class.java)
    
    private val aliasToCodeMap = ConcurrentHashMap<String, String>()
    private val codeToAliasesMap = ConcurrentHashMap<String, List<String>>()
    private val validRoutes = ConcurrentHashMap.newKeySet<String>()

    @EventListener(ApplicationReadyEvent::class)
    fun loadData() = runBlocking {
        logger.info("Loading reference data from database...")
        
        try {
            // Load Airports
            val airports = airportRepository.findAll().asFlow().toList()
            airports.forEach { airport ->
                addAlias(airport.code, airport.code)
                airport.nameEn?.let { addAlias(it, airport.code) }
                airport.nameAr?.let { addAlias(it, airport.code) }
            }

            // Load Aliases
            // Using findAllBy because findAll is inherited but findAllBy was explicit in my previous step
            // Actually findAll() is standard
            val aliases = airportAliasRepository.findAll().asFlow().toList()
            aliases.forEach { alias ->
                addAlias(alias.alias, alias.airportCode)
            }
            
            // Load Routes
            val routes = routeRepository.findAll().asFlow().toList()
            routes.forEach { route ->
                validRoutes.add("${route.origin}-${route.destination}")
            }
            
            logger.info("Loaded ${aliasToCodeMap.size} aliases and ${validRoutes.size} routes.")
        } catch (e: Exception) {
            logger.error("Failed to load reference data", e)
        }
    }

    private fun addAlias(alias: String, code: String) {
        val normalized = alias.lowercase().trim()
        aliasToCodeMap[normalized] = code
        
        codeToAliasesMap.compute(code) { _, list ->
            (list ?: emptyList()) + normalized
        }
    }

    fun getCodeForAlias(alias: String): String? {
        return aliasToCodeMap[alias.lowercase().trim()]
    }
    
    fun getAllAliases(): Set<String> {
        return aliasToCodeMap.keys
    }

    fun isValidRoute(origin: String, destination: String): Boolean {
        return validRoutes.contains("$origin-$destination")
    }
    
    fun getCityCodesMap(): Map<String, List<String>> {
        return codeToAliasesMap
    }
    
    /**
     * Get all destinations reachable from a given origin airport code.
     */
    fun getDestinationsFrom(origin: String): List<String> {
        return validRoutes
            .filter { it.startsWith("$origin-") }
            .map { it.substringAfter("-") }
    }
    
    /**
     * Get city name for an airport code (returns the first alias that looks like a city name).
     */
    fun getCityName(code: String): String {
        val aliases = codeToAliasesMap[code] ?: return code
        // Return the alias that's not the code itself and looks like a name (has spaces or > 3 chars)
        return aliases.firstOrNull { it != code.lowercase() && it.length > 3 } 
            ?: aliases.firstOrNull { it != code.lowercase() } 
            ?: code
    }
}
