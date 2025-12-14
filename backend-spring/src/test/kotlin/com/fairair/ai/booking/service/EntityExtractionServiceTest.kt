package com.fairair.ai.booking.service

import com.fairair.ai.booking.exception.EntityExtractionException
import com.fairair.ai.booking.executor.BedrockLlamaExecutor
import com.fairair.ai.booking.executor.LocalModelExecutor
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class EntityExtractionServiceTest {

    private lateinit var referenceDataService: ReferenceDataService
    private lateinit var levenshteinMatcher: LevenshteinMatcher
    private lateinit var bedrockExecutor: BedrockLlamaExecutor
    private lateinit var localExecutor: LocalModelExecutor
    private lateinit var service: EntityExtractionService

    @BeforeEach
    fun setup() {
        referenceDataService = mock()
        levenshteinMatcher = mock()
        bedrockExecutor = mock()
        localExecutor = mock()
        
        service = EntityExtractionService(
            referenceDataService,
            levenshteinMatcher,
            bedrockExecutor,
            localExecutor
        )
    }

    @Test
    fun `extractAndValidate should return valid map when extraction and resolution succeed`() = runBlocking {
        // Arrange
        val userInput = "fly from Riyadh to Jeddah tomorrow"
        val extractedJson = """{"origin": "Riyadh", "destination": "Jeddah", "date": "2023-12-25", "passengers": 1}"""
        
        whenever(bedrockExecutor.generate(any())).thenReturn(extractedJson)
        whenever(referenceDataService.getCodeForAlias("Riyadh")).thenReturn("RUH")
        whenever(referenceDataService.getCodeForAlias("Jeddah")).thenReturn("JED")
        whenever(referenceDataService.isValidRoute("RUH", "JED")).thenReturn(true)

        // Act
        val result = service.extractAndValidate(userInput)

        // Assert
        assertEquals("RUH", result["origin"])
        assertEquals("JED", result["destination"])
        assertEquals("2023-12-25", result["date"])
        assertEquals(1, result["passengers"])
    }

    @Test
    fun `extractAndValidate should use fuzzy matcher if direct lookup fails`() = runBlocking {
        // Arrange
        val userInput = "fly from Riyad to Jedda"
        val extractedJson = """{"origin": "Riyad", "destination": "Jedda", "date": "2023-12-25", "passengers": 1}"""
        
        whenever(bedrockExecutor.generate(any())).thenReturn(extractedJson)
        whenever(referenceDataService.getCodeForAlias("Riyad")).thenReturn(null)
        whenever(referenceDataService.getCodeForAlias("Jedda")).thenReturn(null)
        
        whenever(levenshteinMatcher.findClosestMatch("Riyad")).thenReturn("RUH")
        whenever(levenshteinMatcher.findClosestMatch("Jedda")).thenReturn("JED")
        
        whenever(referenceDataService.isValidRoute("RUH", "JED")).thenReturn(true)

        // Act
        val result = service.extractAndValidate(userInput)

        // Assert
        assertEquals("RUH", result["origin"])
        assertEquals("JED", result["destination"])
    }
}
