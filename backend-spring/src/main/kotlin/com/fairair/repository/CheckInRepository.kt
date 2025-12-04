package com.fairair.repository

import com.fairair.service.CheckInRecord
import kotlinx.datetime.Instant
import org.springframework.stereotype.Repository
import java.util.concurrent.ConcurrentHashMap

/**
 * Repository for check-in records.
 * 
 * Production Notes:
 * - Replace with Spring Data R2DBC repository
 * - Add CheckInEntity with proper schema
 * - Consider Redis for caching recent check-ins
 */
@Repository
class CheckInRepository {
    
    // In-memory storage for mock implementation
    // Production: Use PostgreSQL table
    private val checkIns = ConcurrentHashMap<String, MutableList<CheckInRecord>>()
    
    /**
     * Find all check-ins for a PNR.
     */
    fun findByPnr(pnr: String): List<CheckInRecord> {
        return checkIns[pnr] ?: emptyList()
    }
    
    /**
     * Find check-in for specific passenger.
     */
    fun findByPnrAndPassengerIndex(pnr: String, passengerIndex: Int): CheckInRecord? {
        return checkIns[pnr]?.find { it.passengerIndex == passengerIndex }
    }
    
    /**
     * Save a check-in record.
     */
    fun save(
        pnr: String,
        passengerIndex: Int,
        seatNumber: String,
        boardingGroup: String,
        sequenceNumber: Int,
        checkedInAt: Instant
    ): CheckInRecord {
        val record = CheckInRecord(
            pnr = pnr,
            passengerIndex = passengerIndex,
            seatNumber = seatNumber,
            boardingGroup = boardingGroup,
            sequenceNumber = sequenceNumber,
            checkedInAt = checkedInAt
        )
        
        checkIns.computeIfAbsent(pnr) { mutableListOf() }.add(record)
        return record
    }
    
    /**
     * Delete all check-ins for a PNR (for cancellation).
     */
    fun deleteByPnr(pnr: String) {
        checkIns.remove(pnr)
    }
}
