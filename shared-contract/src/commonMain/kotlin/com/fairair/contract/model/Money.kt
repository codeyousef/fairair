package com.fairair.contract.model

import kotlinx.serialization.Serializable

/**
 * Represents a monetary amount with currency.
 * Used for all pricing throughout the booking flow.
 *
 * We store amounts as minor units (cents/halalas) to avoid floating point precision
 * issues in cross-platform scenarios.
 */
@Serializable
data class Money(
    val amountMinor: Long,
    val currency: Currency
) {
    /**
     * Returns the amount as a Double for display purposes.
     * SAR uses 2 decimal places (100 halalas = 1 SAR).
     */
    val amountAsDouble: Double
        get() = amountMinor / 100.0

    /**
     * Returns a formatted display string (e.g., "SAR 150.00").
     */
    fun formatDisplay(): String {
        val integerPart = amountMinor / 100
        val decimalPart = (amountMinor % 100).toString().padStart(2, '0')
        return "${currency.name} $integerPart.$decimalPart"
    }

    /**
     * Returns the amount as a formatted string without currency.
     */
    fun formatAmount(): String {
        val integerPart = amountMinor / 100
        val decimalPart = (amountMinor % 100).toString().padStart(2, '0')
        return "$integerPart.$decimalPart"
    }

    companion object {
        /**
         * Creates a Money instance from a double amount (in major units).
         * @param amount The monetary value (e.g., 150.50)
         * @param currency The currency
         * @return A new Money instance
         */
        fun of(amount: Double, currency: Currency): Money =
            Money((amount * 100).toLong(), currency)

        /**
         * Creates a Money instance in SAR (Saudi Riyal).
         * @param amount The monetary value
         * @return A new Money instance in SAR
         */
        fun sar(amount: Double): Money = of(amount, Currency.SAR)

        /**
         * Creates a zero-value Money instance.
         * @param currency The currency (defaults to SAR)
         * @return A Money instance with zero amount
         */
        fun zero(currency: Currency = Currency.SAR): Money = Money(0, currency)

        /**
         * Creates a Money instance from minor units.
         * @param minor The amount in minor units (halalas/cents)
         * @param currency The currency (defaults to SAR)
         * @return A new Money instance
         */
        fun fromMinor(minor: Long, currency: Currency = Currency.SAR): Money =
            Money(minor, currency)
    }

    operator fun plus(other: Money): Money {
        require(currency == other.currency) {
            "Cannot add amounts with different currencies: $currency vs ${other.currency}"
        }
        return Money(amountMinor + other.amountMinor, currency)
    }

    operator fun minus(other: Money): Money {
        require(currency == other.currency) {
            "Cannot subtract amounts with different currencies: $currency vs ${other.currency}"
        }
        require(amountMinor >= other.amountMinor) {
            "Cannot subtract larger amount from smaller"
        }
        return Money(amountMinor - other.amountMinor, currency)
    }

    operator fun times(multiplier: Int): Money =
        Money(amountMinor * multiplier, currency)

    operator fun compareTo(other: Money): Int {
        require(currency == other.currency) {
            "Cannot compare amounts with different currencies: $currency vs ${other.currency}"
        }
        return amountMinor.compareTo(other.amountMinor)
    }
}

/**
 * Supported currencies for fairair bookings.
 * SAR (Saudi Riyal) is the primary currency.
 */
@Serializable
enum class Currency {
    /**
     * Saudi Riyal - Primary currency for fairair
     */
    SAR,

    /**
     * United States Dollar
     */
    USD,

    /**
     * Euro
     */
    EUR
}
