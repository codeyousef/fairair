package com.fairair.contract.model

import kotlinx.serialization.Serializable

/**
 * Available meal option for pre-order.
 */
@Serializable
data class MealOption(
    val id: String,
    val name: String,
    val nameArabic: String,
    val description: String,
    val descriptionArabic: String,
    val category: MealCategory,
    val dietaryInfo: List<DietaryTag>,
    val price: Money,
    val imageUrl: String?,
    val isAvailable: Boolean,
    val servingSize: String,
    val calories: Int?
)

/**
 * Meal categories.
 */
@Serializable
enum class MealCategory {
    /**
     * Hot main course.
     */
    HOT_MEAL,
    
    /**
     * Cold meal/sandwich.
     */
    COLD_MEAL,
    
    /**
     * Breakfast option.
     */
    BREAKFAST,
    
    /**
     * Snack/light bite.
     */
    SNACK,
    
    /**
     * Kids meal.
     */
    KIDS_MEAL,
    
    /**
     * Premium/gourmet meal.
     */
    PREMIUM,
    
    /**
     * Beverage.
     */
    BEVERAGE,
    
    /**
     * Dessert.
     */
    DESSERT
}

/**
 * Dietary tags for meal filtering.
 */
@Serializable
enum class DietaryTag {
    VEGETARIAN,
    VEGAN,
    HALAL,
    KOSHER,
    GLUTEN_FREE,
    DAIRY_FREE,
    NUT_FREE,
    LOW_SODIUM,
    DIABETIC_FRIENDLY,
    LOW_FAT,
    SEAFOOD,
    CHICKEN,
    BEEF,
    LAMB
}

/**
 * Request to add meal to booking.
 */
@Serializable
data class AddMealRequest(
    val pnr: String?,
    val searchId: String?,
    val flightNumber: String,
    val meals: List<PassengerMealSelection>
)

/**
 * Meal selection for a passenger.
 */
@Serializable
data class PassengerMealSelection(
    val passengerIndex: Int,
    val mealId: String,
    val quantity: Int
)

/**
 * Meal order confirmation.
 */
@Serializable
data class MealConfirmation(
    val orderId: String,
    val pnr: PnrCode?,
    val meals: List<OrderedMeal>,
    val totalPrice: Money,
    val message: String
)

/**
 * Ordered meal details.
 */
@Serializable
data class OrderedMeal(
    val passengerName: String,
    val passengerIndex: Int,
    val mealName: String,
    val quantity: Int,
    val price: Money
)

/**
 * Menu for a specific route/flight duration.
 */
@Serializable
data class FlightMenu(
    val origin: AirportCode,
    val destination: AirportCode,
    val flightDurationMinutes: Int,
    val categories: List<MenuCategory>,
    val specialOffers: List<MealDeal>
)

/**
 * Menu category with meals.
 */
@Serializable
data class MenuCategory(
    val category: MealCategory,
    val displayName: String,
    val displayNameArabic: String,
    val meals: List<MealOption>
)

/**
 * Meal deal/bundle offer.
 */
@Serializable
data class MealDeal(
    val id: String,
    val name: String,
    val description: String,
    val includedMeals: List<String>,
    val originalPrice: Money,
    val dealPrice: Money,
    val savingsPercentage: Int
)
