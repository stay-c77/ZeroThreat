package com.zerothreat.core.data.repository

import com.zerothreat.core.data.db.AllScannedUrl
import com.zerothreat.core.data.db.BlockedUrl
import com.zerothreat.core.data.db.PhishingUrl
import com.zerothreat.core.data.db.SafeUrl
import com.zerothreat.core.data.db.SuspiciousUrl
import com.zerothreat.core.data.db.UrlDao
import com.zerothreat.core.detector.PhishingResult
import kotlinx.coroutines.flow.Flow

class UrlRepository(private val urlDao: UrlDao) {

    // Expose the "All" list as the main history
    val allUrls: Flow<List<AllScannedUrl>> = urlDao.getAllScannedUrls()
    val threatCount: Flow<Int> = urlDao.getThreatDetectionsCount()
    val totalScans: Flow<Int> = urlDao.getTotalScans()
    val blockedCount: Flow<Int> = urlDao.getBlockedCount()
    val allBlockedUrls: Flow<List<BlockedUrl>> = urlDao.getAllBlockedUrls()

    /**
     * LOGIC: Write to "All" table + Specific Table
     */
    suspend fun logScan(
        url: String, 
        domain: String, 
        result: PhishingResult, 
        source: String, 
        threatType: String? = null,
        phishingScore: Int = 0,
        analysisNote: String? = null
    ) {
        val timestamp = System.currentTimeMillis()

        // Keep only one exact URL entry across category tables.
        urlDao.deleteSafeUrlByUrl(url)
        urlDao.deleteSuspiciousUrlByUrl(url)
        urlDao.deletePhishingUrlByUrl(url)

        // Preserve stable ID for existing URL instead of letting REPLACE bump it.
        val existingId = urlDao.getAllUrlIdByUrl(url)

        // 1. Write to Master Table
        // 1. Write to Master Table
        val allEntity = AllScannedUrl(
            id = existingId ?: 0,
            url = url,
            domain = domain,
            result = result,
            phishingScore = phishingScore,
            analysisNote = analysisNote,
            source = source,
            timestamp = timestamp
        )
        urlDao.insertAllUrl(allEntity)

        // 2. Write to Specific Category Table
        when (result) {
            PhishingResult.SAFE -> {
                val safeEntity = SafeUrl(
                    url = url,
                    domain = domain,
                    result = result,
                    phishingScore = phishingScore,
                    analysisNote = analysisNote,
                    source = source,
                    timestamp = timestamp
                )
                urlDao.insertSafeUrl(safeEntity)
            }
            PhishingResult.SUSPICIOUS -> {
                val suspEntity = SuspiciousUrl(
                    url = url,
                    domain = domain,
                    result = result,
                    phishingScore = phishingScore,
                    analysisNote = analysisNote,
                    source = source,
                    timestamp = timestamp,
                    threatType = threatType
                )
                urlDao.insertSuspiciousUrl(suspEntity)
            }
            PhishingResult.PHISHING -> {
                val phishEntity = PhishingUrl(
                    url = url,
                    domain = domain,
                    result = result,
                    phishingScore = phishingScore,
                    analysisNote = analysisNote,
                    source = source,
                    timestamp = timestamp,
                    threatType = threatType
                )
                urlDao.insertPhishingUrl(phishEntity)
            }
        }

        // Keep IDs contiguous (1..N) in scan order for easier mapping in UI and reports.
        urlDao.compactAllUrlIdsByScanOrderIfNeeded()
    }

    suspend fun deleteUrl(id: Int) {
        urlDao.deleteAllUrlById(id)
        urlDao.compactAllUrlIdsByScanOrderIfNeeded()
        // Note: For now we only delete from "All" view in UI. 
        // Syncing deletion across tables is complex without a foreign key or shared ID.
        // User asked to "store" in refined tables, we treat them as logs.
    }

    suspend fun clearHistory() {
        urlDao.clearEverything()
    }

    suspend fun blockUrl(
        url: String,
        domain: String,
        result: PhishingResult,
        source: String,
        phishingScore: Int = 0,
        analysisNote: String? = null
    ) {
        val blockedEntity = BlockedUrl(
            url = url,
            domain = domain,
            result = result,
            phishingScore = phishingScore,
            analysisNote = analysisNote,
            source = source
        )
        urlDao.insertBlockedUrl(blockedEntity)
    }

    suspend fun isUrlBlocked(url: String): Boolean {
        return urlDao.isUrlBlocked(url)
    }

    fun getRecentUrls(limit: Int): Flow<List<AllScannedUrl>> {
        return urlDao.getRecentUrls(limit)
    }

    fun getRecentPhishingUrls(limit: Int): Flow<List<PhishingUrl>> {
        return urlDao.getRecentPhishingUrls(limit)
    }

    suspend fun normalizeAllUrlIds() {
        urlDao.compactAllUrlIdsByScanOrderIfNeeded()
    }
}
