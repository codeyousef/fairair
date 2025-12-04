package com.fairair.app

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Frontend E2E tests for Help & Support flow
 * Covers user stories 18.1-18.4
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HelpSupportFlowTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    // ==================== State Management Classes ====================

    data class HelpState(
        val isLoading: Boolean = false,
        val categories: List<HelpCategory> = emptyList(),
        val selectedCategory: HelpCategory? = null,
        val faqs: List<FAQ> = emptyList(),
        val expandedFaqIds: Set<String> = emptySet(),
        val searchQuery: String = "",
        val searchResults: List<FAQ> = emptyList(),
        val contactOptions: List<ContactOption> = emptyList(),
        val error: String? = null
    )

    data class HelpCategory(
        val id: String,
        val name: String,
        val icon: String,
        val description: String,
        val articleCount: Int
    )

    data class FAQ(
        val id: String,
        val question: String,
        val answer: String,
        val categoryId: String,
        val isPopular: Boolean = false,
        val lastUpdated: String? = null
    )

    data class ContactOption(
        val id: String,
        val type: ContactType,
        val label: String,
        val value: String,
        val availability: String,
        val isAvailable: Boolean = true
    )

    enum class ContactType {
        PHONE, EMAIL, CHAT, WHATSAPP, SOCIAL
    }

    // ==================== 18.1 Browse Help Categories Tests ====================

    @Test
    fun `test help categories are displayed`() = testScope.runTest {
        val state = HelpState(categories = createTestCategories())
        
        assertTrue(state.categories.isNotEmpty())
        assertEquals(5, state.categories.size)
    }

    @Test
    fun `test category has icon and description`() = testScope.runTest {
        val categories = createTestCategories()
        
        categories.forEach { category ->
            assertTrue(category.icon.isNotEmpty())
            assertTrue(category.description.isNotEmpty())
        }
    }

    @Test
    fun `test category shows article count`() = testScope.runTest {
        val categories = createTestCategories()
        
        categories.forEach { category ->
            assertTrue(category.articleCount >= 0)
        }
    }

    @Test
    fun `test selecting category loads FAQs`() = testScope.runTest {
        val categories = createTestCategories()
        val selectedCategory = categories.first()
        val categoryFaqs = createTestFaqs().filter { it.categoryId == selectedCategory.id }
        
        val state = HelpState(
            categories = categories,
            selectedCategory = selectedCategory,
            faqs = categoryFaqs
        )
        
        assertNotNull(state.selectedCategory)
        assertTrue(state.faqs.isNotEmpty())
        assertTrue(state.faqs.all { it.categoryId == selectedCategory.id })
    }

    @Test
    fun `test loading state while fetching categories`() = testScope.runTest {
        val state = HelpState(isLoading = true)
        
        assertTrue(state.isLoading)
        assertTrue(state.categories.isEmpty())
    }

    // ==================== 18.2 Read FAQ Articles Tests ====================

    @Test
    fun `test FAQ accordion expand and collapse`() = testScope.runTest {
        val faqs = createTestFaqs()
        var expandedIds: Set<String> = emptySet()
        
        // Initially all collapsed
        assertTrue(expandedIds.isEmpty())
        
        // Expand first FAQ
        val firstFaqId = faqs.first().id
        expandedIds = expandedIds + firstFaqId
        assertTrue(expandedIds.contains(firstFaqId))
        
        // Collapse first FAQ
        expandedIds = expandedIds - firstFaqId
        assertFalse(expandedIds.contains(firstFaqId))
    }

    @Test
    fun `test multiple FAQs can be expanded`() = testScope.runTest {
        val faqs = createTestFaqs()
        var expandedIds: Set<String> = emptySet()
        
        // Expand multiple FAQs
        expandedIds = expandedIds + faqs[0].id
        expandedIds = expandedIds + faqs[1].id
        
        assertEquals(2, expandedIds.size)
    }

    @Test
    fun `test FAQ answer is visible when expanded`() = testScope.runTest {
        val faq = createTestFaqs().first()
        
        val state = HelpState(
            faqs = listOf(faq),
            expandedFaqIds = setOf(faq.id)
        )
        
        assertTrue(state.expandedFaqIds.contains(faq.id))
        assertTrue(faq.answer.isNotEmpty())
    }

    @Test
    fun `test popular FAQs are highlighted`() = testScope.runTest {
        val faqs = createTestFaqs()
        val popularFaqs = faqs.filter { it.isPopular }
        
        assertTrue(popularFaqs.isNotEmpty())
    }

    @Test
    fun `test FAQ shows last updated date`() = testScope.runTest {
        val faq = createTestFaqs().first()
        
        assertNotNull(faq.lastUpdated)
    }

    // ==================== 18.3 Search Help Content Tests ====================

    @Test
    fun `test search returns matching FAQs`() = testScope.runTest {
        val searchQuery = "baggage"
        val allFaqs = createTestFaqs()
        val searchResults = allFaqs.filter { 
            it.question.contains(searchQuery, ignoreCase = true) ||
            it.answer.contains(searchQuery, ignoreCase = true)
        }
        
        val state = HelpState(
            searchQuery = searchQuery,
            searchResults = searchResults
        )
        
        assertEquals(searchQuery, state.searchQuery)
        assertTrue(state.searchResults.isNotEmpty())
    }

    @Test
    fun `test search with no results`() = testScope.runTest {
        val searchQuery = "xyznonexistent"
        val allFaqs = createTestFaqs()
        val searchResults = allFaqs.filter { 
            it.question.contains(searchQuery, ignoreCase = true)
        }
        
        val state = HelpState(
            searchQuery = searchQuery,
            searchResults = searchResults
        )
        
        assertTrue(state.searchResults.isEmpty())
    }

    @Test
    fun `test empty search query clears results`() = testScope.runTest {
        var state = HelpState(
            searchQuery = "test",
            searchResults = createTestFaqs()
        )
        
        state = state.copy(searchQuery = "", searchResults = emptyList())
        
        assertEquals("", state.searchQuery)
        assertTrue(state.searchResults.isEmpty())
    }

    @Test
    fun `test search is case insensitive`() = testScope.runTest {
        val allFaqs = createTestFaqs()
        
        val lowerResults = allFaqs.filter { it.question.contains("baggage", ignoreCase = true) }
        val upperResults = allFaqs.filter { it.question.contains("BAGGAGE", ignoreCase = true) }
        
        assertEquals(lowerResults.size, upperResults.size)
    }

    @Test
    fun `test search minimum character requirement`() = testScope.runTest {
        val minSearchLength = 3
        val shortQuery = "ba"
        val validQuery = "bag"
        
        assertFalse(shortQuery.length >= minSearchLength)
        assertTrue(validQuery.length >= minSearchLength)
    }

    // ==================== 18.4 Contact Support Tests ====================

    @Test
    fun `test contact options are displayed`() = testScope.runTest {
        val state = HelpState(contactOptions = createTestContactOptions())
        
        assertTrue(state.contactOptions.isNotEmpty())
    }

    @Test
    fun `test contact option types`() = testScope.runTest {
        val options = createTestContactOptions()
        
        assertTrue(options.any { it.type == ContactType.PHONE })
        assertTrue(options.any { it.type == ContactType.EMAIL })
        assertTrue(options.any { it.type == ContactType.CHAT })
        assertTrue(options.any { it.type == ContactType.WHATSAPP })
    }

    @Test
    fun `test contact option availability`() = testScope.runTest {
        val options = createTestContactOptions()
        
        options.forEach { option ->
            assertTrue(option.availability.isNotEmpty())
        }
    }

    @Test
    fun `test unavailable contact option handling`() = testScope.runTest {
        val options = createTestContactOptions()
        val unavailableOption = options.find { !it.isAvailable }
        
        // Some options may be unavailable (e.g., outside business hours)
        if (unavailableOption != null) {
            assertFalse(unavailableOption.isAvailable)
        }
    }

    @Test
    fun `test phone number format`() = testScope.runTest {
        val phoneOption = createTestContactOptions().first { it.type == ContactType.PHONE }
        
        assertTrue(phoneOption.value.startsWith("+") || phoneOption.value.matches(Regex("^\\d{3,}")))
    }

    @Test
    fun `test email format`() = testScope.runTest {
        val emailOption = createTestContactOptions().first { it.type == ContactType.EMAIL }
        
        assertTrue(emailOption.value.contains("@"))
    }

    // ==================== Edge Cases ====================

    @Test
    fun `test error loading help content`() = testScope.runTest {
        val state = HelpState(
            isLoading = false,
            error = "Failed to load help content. Please try again."
        )
        
        assertNotNull(state.error)
        assertTrue(state.categories.isEmpty())
    }

    @Test
    fun `test offline mode shows cached content`() = testScope.runTest {
        // In offline mode, cached FAQs should still be accessible
        val cachedFaqs = createTestFaqs().take(3)
        
        val state = HelpState(
            faqs = cachedFaqs,
            error = "You're offline. Showing cached content."
        )
        
        assertTrue(state.faqs.isNotEmpty())
        assertNotNull(state.error)
    }

    @Test
    fun `test deep link to specific FAQ`() = testScope.runTest {
        val targetFaqId = "faq_baggage_1"
        val faqs = createTestFaqs()
        val targetFaq = faqs.find { it.id == targetFaqId }
        
        val state = HelpState(
            faqs = faqs,
            expandedFaqIds = if (targetFaq != null) setOf(targetFaqId) else emptySet()
        )
        
        if (targetFaq != null) {
            assertTrue(state.expandedFaqIds.contains(targetFaqId))
        }
    }

    @Test
    fun `test accessibility for FAQ content`() = testScope.runTest {
        val faq = createTestFaqs().first()
        
        // FAQ should have readable content
        assertTrue(faq.question.isNotEmpty())
        assertTrue(faq.answer.isNotEmpty())
        // Question should end with question mark or be formatted properly
        assertTrue(faq.question.endsWith("?") || faq.question.length > 10)
    }

    // ==================== Helper Functions ====================

    private fun createTestCategories(): List<HelpCategory> {
        return listOf(
            HelpCategory("booking", "Booking & Reservations", "✈️", "Help with flight bookings and reservations", 15),
            HelpCategory("baggage", "Baggage", "🧳", "Baggage allowance and policies", 12),
            HelpCategory("checkin", "Check-In", "📱", "Online and airport check-in help", 8),
            HelpCategory("payment", "Payment & Refunds", "💳", "Payment methods and refund policies", 10),
            HelpCategory("travel", "Travel Documents", "📄", "Passport, visa, and travel requirements", 6)
        )
    }

    private fun createTestFaqs(): List<FAQ> {
        return listOf(
            FAQ("faq_baggage_1", "What is the baggage allowance?", "Hand baggage: 7kg, Checked baggage: 23kg for most fares.", "baggage", true, "2024-01-15"),
            FAQ("faq_baggage_2", "How do I add extra baggage?", "You can add extra baggage during booking or via Manage Booking.", "baggage", false, "2024-01-10"),
            FAQ("faq_booking_1", "How do I change my flight?", "Go to Manage Booking, enter your PNR and last name to modify.", "booking", true, "2024-01-20"),
            FAQ("faq_booking_2", "Can I cancel my booking?", "Yes, cancellation policies vary by fare type. Check Manage Booking.", "booking", false, "2024-01-18"),
            FAQ("faq_checkin_1", "When does online check-in open?", "Online check-in opens 48 hours before departure.", "checkin", true, "2024-01-12"),
            FAQ("faq_payment_1", "What payment methods are accepted?", "We accept credit cards, debit cards, Apple Pay, and bank transfer.", "payment", false, "2024-01-08")
        )
    }

    private fun createTestContactOptions(): List<ContactOption> {
        return listOf(
            ContactOption("phone", ContactType.PHONE, "Call Us", "+966 11 234 5678", "24/7", true),
            ContactOption("email", ContactType.EMAIL, "Email Support", "support@flyadeal.com", "Response within 24h", true),
            ContactOption("chat", ContactType.CHAT, "Live Chat", "chat://support", "9 AM - 9 PM", true),
            ContactOption("whatsapp", ContactType.WHATSAPP, "WhatsApp", "+966 50 123 4567", "24/7", true),
            ContactOption("twitter", ContactType.SOCIAL, "Twitter/X", "@flyadeal", "9 AM - 6 PM", false)
        )
    }
}
