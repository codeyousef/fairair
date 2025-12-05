package com.fairair.app.ui.screens.results

import com.fairair.contract.dto.FareFamilyDto
import com.fairair.contract.dto.FlightDto
import com.fairair.contract.model.Currency
import com.fairair.contract.model.Money

/**
 * Maps API DTOs to Velocity UI display models.
 */
object FlightMapper {

    /**
     * Converts a FlightDto from the API to a VelocityFlightCard for display.
     *
     * @param flight The API flight data
     * @return VelocityFlightCard display model
     */
    fun toVelocityFlightCard(flight: FlightDto): VelocityFlightCard {
        val fareFamilies = flight.fareFamilies.mapIndexed { index, dto ->
            toFareFamily("${flight.flightNumber}_$index", dto)
        }
        val lowestPrice = fareFamilies.minByOrNull { it.price.amountMinor }?.price
            ?: Money.zero(Currency.SAR)

        return VelocityFlightCard(
            id = flight.flightNumber,
            flightNumber = flight.flightNumber,
            departureTime = formatTime(flight.departureTime),
            arrivalTime = formatTime(flight.arrivalTime),
            originCode = flight.origin,
            destinationCode = flight.destination,
            durationMinutes = flight.durationMinutes,
            lowestPrice = lowestPrice,
            fareFamilies = fareFamilies
        )
    }

    /**
     * Converts a FareFamilyDto to a FareFamily display model.
     */
    fun toFareFamily(id: String, dto: FareFamilyDto): FareFamily {
        val code = when (dto.code.uppercase()) {
            "FLY" -> FareFamilyCode.FLY
            "FLY_PLUS", "FLYPLUS", "FLY+" -> FareFamilyCode.FLY_PLUS
            "FLY_MAX", "FLYMAX" -> FareFamilyCode.FLY_MAX
            else -> FareFamilyCode.FLY
        }

        val displayName = when (code) {
            FareFamilyCode.FLY -> "Fly"
            FareFamilyCode.FLY_PLUS -> "Fly+"
            FareFamilyCode.FLY_MAX -> "FlyMax"
        }

        val currency = when (dto.currency.uppercase()) {
            "USD" -> Currency.USD
            "EUR" -> Currency.EUR
            else -> Currency.SAR
        }

        return FareFamily(
            id = id,
            code = code,
            displayName = displayName,
            price = Money.fromMinor(dto.priceMinor, currency)
        )
    }

    /**
     * Converts a list of FlightDto to VelocityFlightCard list.
     */
    fun toVelocityFlightCards(flights: List<FlightDto>): List<VelocityFlightCard> {
        return flights.map { toVelocityFlightCard(it) }
    }

    /**
     * Formats a time string to display format.
     * Expects ISO datetime format (2025-11-30T08:00:00) or HH:mm and returns HH:mm.
     */
    private fun formatTime(time: String): String {
        // If already in HH:mm format, return as is
        if (time.matches(Regex("\\d{2}:\\d{2}"))) {
            return time
        }

        // If in ISO datetime format, extract the time part after "T"
        return try {
            val timePart = if (time.contains("T")) {
                time.substringAfter("T")
            } else {
                time
            }
            // Take first two components (HH:mm) and ignore seconds/timezone
            timePart.split(":")
                .take(2)
                .joinToString(":")
        } catch (e: Exception) {
            time
        }
    }
}

/**
 * Extension function to convert a list of FlightDto to VelocityFlightCard list.
 */
fun List<FlightDto>.toVelocityFlightCards(): List<VelocityFlightCard> {
    return FlightMapper.toVelocityFlightCards(this)
}

/**
 * Extension function to convert a single FlightDto to VelocityFlightCard.
 */
fun FlightDto.toVelocityFlightCard(): VelocityFlightCard {
    return FlightMapper.toVelocityFlightCard(this)
}
