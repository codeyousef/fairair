package com.fairair.ai.booking.service

import org.springframework.stereotype.Component
import kotlin.math.min

@Component
class LevenshteinMatcher(
    private val referenceDataService: ReferenceDataService
) {
    
    fun findClosestMatch(input: String, threshold: Int = 2): String? {
        val normalizedInput = input.lowercase().trim()
        
        // 1. Direct match (though usually handled before calling this, safe to double check)
        val directMatch = referenceDataService.getCodeForAlias(normalizedInput)
        if (directMatch != null) return directMatch
        
        // 2. Fuzzy match
        var bestMatch: String? = null
        var minDistance = Int.MAX_VALUE
        
        for (alias in referenceDataService.getAllAliases()) {
            val distance = calculateDistance(normalizedInput, alias)
            if (distance <= threshold && distance < minDistance) {
                minDistance = distance
                bestMatch = alias
            }
        }
        
        return bestMatch?.let { referenceDataService.getCodeForAlias(it) }
    }

    private fun calculateDistance(s1: String, s2: String): Int {
        val dp = Array(s1.length + 1) { IntArray(s2.length + 1) }

        for (i in 0..s1.length) dp[i][0] = i
        for (j in 0..s2.length) dp[0][j] = j

        for (i in 1..s1.length) {
            for (j in 1..s2.length) {
                val cost = if (s1[i - 1] == s2[j - 1]) 0 else 1
                dp[i][j] = min(
                    min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                    dp[i - 1][j - 1] + cost
                )
            }
        }

        return dp[s1.length][s2.length]
    }
}
