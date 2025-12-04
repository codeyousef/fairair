package com.fairair.service

import com.fairair.contract.model.Money
import com.fairair.controller.MealNotFoundException
import com.fairair.repository.BookingRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.util.concurrent.ConcurrentHashMap

/**
 * Service for meal pre-order operations.
 * Handles meal catalog and ordering.
 *
 * Production Notes:
 * - Integrate with Navitaire SSR (Special Service Requests) API
 * - Support dietary restrictions and allergies
 * - Handle meal availability per route/aircraft
 */
@Service
class MealsService(
    private val bookingRepository: BookingRepository
) {
    private val log = LoggerFactory.getLogger(MealsService::class.java)

    // In-memory storage for meal orders (mock)
    private val mealOrders = ConcurrentHashMap<String, MutableList<MealOrder>>()

    companion object {
        // Static meal catalog
        private val MEALS = listOf(
            MealOption(
                id = "meal-chicken-rice",
                name = "Grilled Chicken with Rice",
                nameAr = "دجاج مشوي مع أرز",
                description = "Tender grilled chicken breast served with aromatic rice and vegetables",
                descriptionAr = "صدر دجاج مشوي طري يقدم مع أرز عطري وخضروات",
                category = MealCategory.HOT_MEAL,
                price = Money.sar(45.0),
                imageUrl = "/images/meals/chicken-rice.jpg",
                dietaryInfo = listOf("Halal"),
                allergens = emptyList(),
                calories = 450
            ),
            MealOption(
                id = "meal-beef-pasta",
                name = "Beef Pasta",
                nameAr = "باستا باللحم",
                description = "Italian pasta with seasoned ground beef in tomato sauce",
                descriptionAr = "باستا إيطالية مع لحم بقري متبل بصلصة الطماطم",
                category = MealCategory.HOT_MEAL,
                price = Money.sar(55.0),
                imageUrl = "/images/meals/beef-pasta.jpg",
                dietaryInfo = listOf("Halal"),
                allergens = listOf("Gluten", "Dairy"),
                calories = 520
            ),
            MealOption(
                id = "meal-vegetarian",
                name = "Vegetable Curry",
                nameAr = "كاري الخضار",
                description = "Mixed vegetables in aromatic curry sauce with basmati rice",
                descriptionAr = "خضروات مشكلة في صلصة كاري عطرية مع أرز بسمتي",
                category = MealCategory.HOT_MEAL,
                price = Money.sar(40.0),
                imageUrl = "/images/meals/veg-curry.jpg",
                dietaryInfo = listOf("Vegetarian", "Halal"),
                allergens = emptyList(),
                calories = 380
            ),
            MealOption(
                id = "meal-fish",
                name = "Grilled Fish Fillet",
                nameAr = "فيليه سمك مشوي",
                description = "Fresh fish fillet with herbs, served with potatoes",
                descriptionAr = "فيليه سمك طازج مع الأعشاب يقدم مع البطاطس",
                category = MealCategory.HOT_MEAL,
                price = Money.sar(60.0),
                imageUrl = "/images/meals/fish.jpg",
                dietaryInfo = listOf("Halal", "Gluten-Free"),
                allergens = listOf("Fish"),
                calories = 400
            ),
            MealOption(
                id = "meal-kids-nuggets",
                name = "Kids Chicken Nuggets",
                nameAr = "ناغتس دجاج للأطفال",
                description = "Chicken nuggets with fries and juice box",
                descriptionAr = "ناغتس دجاج مع بطاطس مقلية وعصير",
                category = MealCategory.KIDS_MEAL,
                price = Money.sar(35.0),
                imageUrl = "/images/meals/kids-nuggets.jpg",
                dietaryInfo = listOf("Halal"),
                allergens = listOf("Gluten"),
                calories = 420
            ),
            MealOption(
                id = "snack-sandwich",
                name = "Club Sandwich",
                nameAr = "كلوب ساندويتش",
                description = "Triple-decker sandwich with chicken, cheese, and vegetables",
                descriptionAr = "ساندويتش ثلاثي الطبقات مع الدجاج والجبن والخضروات",
                category = MealCategory.SNACK,
                price = Money.sar(30.0),
                imageUrl = "/images/meals/sandwich.jpg",
                dietaryInfo = listOf("Halal"),
                allergens = listOf("Gluten", "Dairy"),
                calories = 350
            ),
            MealOption(
                id = "snack-nuts",
                name = "Premium Nut Mix",
                nameAr = "مكسرات فاخرة",
                description = "Roasted almonds, cashews, and pistachios",
                descriptionAr = "لوز وكاجو وفستق محمص",
                category = MealCategory.SNACK,
                price = Money.sar(20.0),
                imageUrl = "/images/meals/nuts.jpg",
                dietaryInfo = listOf("Vegan", "Gluten-Free"),
                allergens = listOf("Tree Nuts"),
                calories = 280
            ),
            MealOption(
                id = "breakfast-arabic",
                name = "Arabic Breakfast",
                nameAr = "إفطار عربي",
                description = "Foul, hummus, falafel, cheese, and fresh bread",
                descriptionAr = "فول، حمص، فلافل، جبن، وخبز طازج",
                category = MealCategory.BREAKFAST,
                price = Money.sar(40.0),
                imageUrl = "/images/meals/arabic-breakfast.jpg",
                dietaryInfo = listOf("Vegetarian", "Halal"),
                allergens = listOf("Gluten", "Sesame"),
                calories = 480
            ),
            MealOption(
                id = "breakfast-continental",
                name = "Continental Breakfast",
                nameAr = "إفطار كونتيننتال",
                description = "Croissant, butter, jam, yogurt, and fresh fruits",
                descriptionAr = "كرواسون، زبدة، مربى، زبادي، وفواكه طازجة",
                category = MealCategory.BREAKFAST,
                price = Money.sar(35.0),
                imageUrl = "/images/meals/continental.jpg",
                dietaryInfo = listOf("Vegetarian"),
                allergens = listOf("Gluten", "Dairy"),
                calories = 420
            ),
            MealOption(
                id = "drink-coffee",
                name = "Premium Arabic Coffee",
                nameAr = "قهوة عربية فاخرة",
                description = "Traditional Saudi coffee with dates",
                descriptionAr = "قهوة سعودية تقليدية مع التمر",
                category = MealCategory.BEVERAGE,
                price = Money.sar(15.0),
                imageUrl = "/images/meals/coffee.jpg",
                dietaryInfo = listOf("Vegan"),
                allergens = emptyList(),
                calories = 50
            ),
            MealOption(
                id = "drink-juice",
                name = "Fresh Orange Juice",
                nameAr = "عصير برتقال طازج",
                description = "Freshly squeezed orange juice",
                descriptionAr = "عصير برتقال مضغوط طازج",
                category = MealCategory.BEVERAGE,
                price = Money.sar(12.0),
                imageUrl = "/images/meals/juice.jpg",
                dietaryInfo = listOf("Vegan", "Gluten-Free"),
                allergens = emptyList(),
                calories = 110
            ),
            MealOption(
                id = "dessert-cake",
                name = "Chocolate Lava Cake",
                nameAr = "كيكة الشوكولاتة",
                description = "Warm chocolate cake with molten center",
                descriptionAr = "كيكة شوكولاتة دافئة مع قلب ذائب",
                category = MealCategory.DESSERT,
                price = Money.sar(25.0),
                imageUrl = "/images/meals/lava-cake.jpg",
                dietaryInfo = listOf("Vegetarian"),
                allergens = listOf("Gluten", "Dairy", "Eggs"),
                calories = 380
            )
        )
    }

    /**
     * Gets available meals for a route.
     */
    suspend fun getAvailableMeals(
        origin: String,
        destination: String,
        mealTime: String?
    ): MealCatalogResponse {
        log.info("Getting meals for $origin -> $destination, time=$mealTime")

        // Filter by meal time if specified
        val category = mealTime?.let {
            try {
                MealCategory.valueOf(it.uppercase())
            } catch (e: IllegalArgumentException) {
                null
            }
        }

        val filteredMeals = if (category != null) {
            MEALS.filter { it.category == category }
        } else {
            MEALS
        }

        // Group by category
        val grouped = filteredMeals.groupBy { it.category }

        return MealCatalogResponse(
            route = "$origin-$destination",
            categories = MealCategory.values().mapNotNull { cat ->
                grouped[cat]?.let { meals ->
                    MealCategoryGroup(
                        category = cat.name,
                        displayName = cat.displayName,
                        displayNameAr = cat.displayNameAr,
                        meals = meals
                    )
                }
            },
            totalOptions = filteredMeals.size,
            currency = "SAR"
        )
    }

    /**
     * Adds a meal to a booking.
     */
    suspend fun addMealToBooking(
        pnr: String,
        passengerIndex: Int,
        segmentIndex: Int,
        mealId: String
    ): AddMealResponse {
        log.info("Adding meal $mealId to PNR=$pnr, passenger=$passengerIndex")

        val normalizedPnr = pnr.uppercase()
        val booking = bookingRepository.findByPnr(normalizedPnr)
            ?: throw com.fairair.controller.BookingNotFoundException(normalizedPnr)

        val meal = MEALS.find { it.id == mealId }
            ?: throw MealNotFoundException(mealId)

        // Parse passengers from JSON
        val passengerNames = parsePassengerNames(booking.passengersJson)
        
        if (passengerIndex < 0 || passengerIndex >= passengerNames.size) {
            throw IllegalArgumentException("Invalid passenger index: $passengerIndex")
        }

        val order = MealOrder(
            pnr = normalizedPnr,
            passengerIndex = passengerIndex,
            segmentIndex = segmentIndex,
            mealId = mealId,
            mealName = meal.name,
            price = meal.price
        )

        mealOrders.getOrPut(normalizedPnr) { mutableListOf() }.add(order)

        val passengerName = passengerNames[passengerIndex]

        return AddMealResponse(
            success = true,
            pnr = normalizedPnr,
            passengerName = passengerName,
            mealName = meal.name,
            price = meal.price,
            message = "Meal successfully added to your booking"
        )
    }
    
    private fun parsePassengerNames(passengersJson: String): List<String> {
        // Extract full names from JSON
        val regex = """"fullName"\s*:\s*"([^"]+)"""".toRegex()
        return regex.findAll(passengersJson).map { it.groupValues[1] }.toList()
            .ifEmpty { listOf("Passenger 1") }
    }

    /**
     * Removes a meal from a booking.
     */
    suspend fun removeMealFromBooking(
        pnr: String,
        passengerIndex: Int,
        segmentIndex: Int
    ) {
        log.info("Removing meal from PNR=$pnr, passenger=$passengerIndex")

        val normalizedPnr = pnr.uppercase()
        bookingRepository.findByPnr(normalizedPnr)
            ?: throw com.fairair.controller.BookingNotFoundException(normalizedPnr)

        mealOrders[normalizedPnr]?.removeIf {
            it.passengerIndex == passengerIndex && it.segmentIndex == segmentIndex
        }
    }

    /**
     * Gets all meals, optionally filtered by dietary preference or category.
     */
    suspend fun getAllMeals(dietary: String?, category: String?): List<MealDto> {
        log.info("Getting all meals, dietary=$dietary, category=$category")

        var filteredMeals = MEALS.toList()

        // Filter by dietary preference
        if (dietary != null) {
            val dietaryLower = dietary.lowercase()
            filteredMeals = filteredMeals.filter { meal ->
                when (dietaryLower) {
                    "vegetarian" -> meal.dietaryInfo.any { it.lowercase().contains("vegetarian") }
                    "vegan" -> meal.dietaryInfo.any { it.lowercase().contains("vegan") }
                    "halal" -> meal.dietaryInfo.any { it.lowercase().contains("halal") }
                    "gluten-free" -> meal.dietaryInfo.any { it.lowercase().contains("gluten-free") } ||
                        !meal.allergens.any { it.lowercase().contains("gluten") }
                    else -> true
                }
            }
        }

        // Filter by category
        if (category != null) {
            val categoryLower = category.lowercase().replace("-", "_")
            filteredMeals = filteredMeals.filter { meal ->
                meal.category.name.lowercase() == categoryLower
            }
        }

        return filteredMeals.map { meal ->
            MealDto(
                id = meal.id,
                name = meal.name,
                nameAr = meal.nameAr,
                description = meal.description,
                descriptionAr = meal.descriptionAr,
                category = meal.category.displayName,
                price = meal.price,
                imageUrl = meal.imageUrl,
                isVegetarian = meal.dietaryInfo.any { it.lowercase().contains("vegetarian") },
                isVegan = meal.dietaryInfo.any { it.lowercase().contains("vegan") },
                isHalal = meal.dietaryInfo.any { it.lowercase().contains("halal") },
                isGlutenFree = !meal.allergens.any { it.lowercase().contains("gluten") },
                allergens = meal.allergens,
                calories = meal.calories
            )
        }
    }

    /**
     * Gets meals ordered for a booking.
     */
    suspend fun getBookingMeals(pnr: String, lastName: String): BookingMealsResponse {
        log.info("Getting meals for PNR=$pnr")

        val normalizedPnr = pnr.uppercase()
        val booking = bookingRepository.findByPnr(normalizedPnr)
            ?: throw com.fairair.controller.BookingNotFoundException(normalizedPnr)

        val orders = mealOrders.getOrDefault(normalizedPnr, emptyList())

        return BookingMealsResponse(
            pnr = normalizedPnr,
            passengerMeals = orders.map { order ->
                PassengerMealInfo(
                    passengerIndex = order.passengerIndex,
                    mealId = order.mealId,
                    mealName = order.mealName,
                    price = order.price
                )
            }
        )
    }
}

// DTOs for API responses
data class MealDto(
    val id: String,
    val name: String,
    val nameAr: String,
    val description: String,
    val descriptionAr: String,
    val category: String,
    val price: Money,
    val imageUrl: String?,
    val isVegetarian: Boolean,
    val isVegan: Boolean,
    val isHalal: Boolean,
    val isGlutenFree: Boolean,
    val allergens: List<String>,
    val calories: Int
)

data class BookingMealsResponse(
    val pnr: String,
    val passengerMeals: List<PassengerMealInfo>
)

data class PassengerMealInfo(
    val passengerIndex: Int,
    val mealId: String,
    val mealName: String,
    val price: Money
)

data class AddMealResponse(
    val success: Boolean,
    val pnr: String,
    val passengerName: String,
    val mealName: String,
    val price: Money,
    val message: String
)

// Domain models
data class MealOption(
    val id: String,
    val name: String,
    val nameAr: String,
    val description: String,
    val descriptionAr: String,
    val category: MealCategory,
    val price: Money,
    val imageUrl: String?,
    val dietaryInfo: List<String>,
    val allergens: List<String>,
    val calories: Int
)

enum class MealCategory(val displayName: String, val displayNameAr: String) {
    HOT_MEAL("Hot Meals", "وجبات ساخنة"),
    KIDS_MEAL("Kids Meals", "وجبات الأطفال"),
    SNACK("Snacks", "وجبات خفيفة"),
    BREAKFAST("Breakfast", "الإفطار"),
    BEVERAGE("Beverages", "المشروبات"),
    DESSERT("Desserts", "الحلويات")
}

data class MealCatalogResponse(
    val route: String,
    val categories: List<MealCategoryGroup>,
    val totalOptions: Int,
    val currency: String
)

data class MealCategoryGroup(
    val category: String,
    val displayName: String,
    val displayNameAr: String,
    val meals: List<MealOption>
)

data class MealOrder(
    val pnr: String,
    val passengerIndex: Int,
    val segmentIndex: Int,
    val mealId: String,
    val mealName: String,
    val price: Money
)

data class MealConfirmation(
    val pnr: String,
    val passengerName: String,
    val meal: MealOption,
    val totalCharged: Money,
    val message: String
)
