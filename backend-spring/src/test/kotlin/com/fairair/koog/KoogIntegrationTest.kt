package com.fairair.koog

import com.fairair.ai.booking.service.BookingOrchestrator
import com.fairair.client.NavitaireClient
import com.fairair.contract.model.FlightResponse
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.test.context.ActiveProfiles
import com.fairair.ai.GenAiProvider
import com.fairair.ai.AiChatResponse
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.whenever
import org.mockito.kotlin.verify

@SpringBootTest
@ActiveProfiles("test")
class KoogIntegrationTest {

    @Autowired
    private lateinit var bookingOrchestrator: BookingOrchestrator

    @MockBean
    private lateinit var navitaireClient: NavitaireClient

    @MockBean
    private lateinit var genAiProvider: GenAiProvider

    @Test
    fun `should extract entities and call navitaire search`() {
        runBlocking {
            // Mock GenAiProvider to return JSON
            whenever(genAiProvider.chat(any(), any(), anyOrNull())).thenReturn(
                AiChatResponse(text = """
                {
                    "origin": "riyadh",
                    "destination": "jeddah",
                    "date": "2025-12-25",
                    "passengers": 2
                }
                """)
            )

            // Mock Navitaire
            whenever(navitaireClient.searchFlights(any())).thenReturn(
                FlightResponse(flights = emptyList(), searchId = "test-search")
            )

            val result = bookingOrchestrator.handleUserRequest("I want to fly from Riyadh to Jeddah on Dec 25 for 2 people")

            assertTrue(result.response.contains("I couldn't find any flights"))
            
            // Verify Navitaire called
            verify(navitaireClient).searchFlights(any())
        }
    }
}
