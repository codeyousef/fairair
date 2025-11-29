package com.flyadeal.app.ui.screens.results

import com.flyadeal.contract.model.Money

/**
 * Fare family code identifying the type of fare.
 */
enum class FareFamilyCode {
    /**
     * Basic fare - Fly
     */
    FLY,

    /**
     * Enhanced fare with extras - Fly+
     */
    FLY_PLUS,

    /**
     * Premium fare with full benefits - FlyMax
     */
    FLY_MAX
}

/**
 * Display model for a fare option within a flight card.
 */
data class FareFamily(
    /**
     * Unique identifier for this fare option.
     */
    val id: String,

    /**
     * Fare family code (FLY, FLY_PLUS, FLY_MAX).
     */
    val code: FareFamilyCode,

    /**
     * Human-readable display name (e.g., "Fly", "Fly+", "FlyMax").
     */
    val displayName: String,

    /**
     * Price for this fare option.
     */
    val price: Money,

    /**
     * Whether this fare is currently selected.
     */
    val isSelected: Boolean = false
) {
    companion object {
        /**
         * Creates a FareFamily from a fare family string code.
         */
        fun fromCode(id: String, code: String, price: Money): FareFamily {
            val fareFamilyCode = when (code.uppercase()) {
                "FLY" -> FareFamilyCode.FLY
                "FLY+", "FLY_PLUS", "FLYPLUS" -> FareFamilyCode.FLY_PLUS
                "FLYMAX", "FLY_MAX" -> FareFamilyCode.FLY_MAX
                else -> FareFamilyCode.FLY
            }

            val displayName = when (fareFamilyCode) {
                FareFamilyCode.FLY -> "Fly"
                FareFamilyCode.FLY_PLUS -> "Fly+"
                FareFamilyCode.FLY_MAX -> "FlyMax"
            }

            return FareFamily(
                id = id,
                code = fareFamilyCode,
                displayName = displayName,
                price = price
            )
        }

        /**
         * Arabic display names for fare families.
         */
        fun getArabicDisplayName(code: FareFamilyCode): String = when (code) {
            FareFamilyCode.FLY -> "فلاي"
            FareFamilyCode.FLY_PLUS -> "فلاي+"
            FareFamilyCode.FLY_MAX -> "فلاي ماكس"
        }
    }
}

/**
 * Extension to format price for display in Arabic (amount then currency).
 */
fun Money.formatForDisplayArabic(): String {
    return "${formatAmount()} ${currency.name}"
}
