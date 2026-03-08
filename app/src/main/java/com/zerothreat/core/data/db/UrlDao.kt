package com.zerothreat.core.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UrlDao {

    // ---- ALL SCANNED URLS (Master Table) ----
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllUrl(entity: AllScannedUrl)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllUrls(entities: List<AllScannedUrl>)

    @Query("SELECT id FROM all_scanned_URLs WHERE url = :url LIMIT 1")
    suspend fun getAllUrlIdByUrl(url: String): Int?

    @Query("SELECT * FROM all_scanned_URLs ORDER BY timestamp DESC")
    fun getAllScannedUrls(): Flow<List<AllScannedUrl>>

    @Query("SELECT * FROM all_scanned_URLs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentUrls(limit: Int): Flow<List<AllScannedUrl>>

    @Query("SELECT COUNT(*) FROM all_scanned_URLs")
    fun getTotalScans(): Flow<Int>

    @Query("SELECT COUNT(*) FROM all_scanned_URLs WHERE result != 'SAFE'")
    fun getThreatDetectionsCount(): Flow<Int>
    
    @Query("DELETE FROM all_scanned_URLs WHERE id = :id")
    suspend fun deleteAllUrlById(id: Int)

    @Query("DELETE FROM all_scanned_URLs")
    suspend fun clearAllUrls()

    @Query("DELETE FROM sqlite_sequence WHERE name='all_scanned_URLs'")
    suspend fun resetAllUrlsId()

    @Query("SELECT * FROM all_scanned_URLs ORDER BY timestamp ASC, id ASC")
    suspend fun getAllScannedUrlsByScanOrderSnapshot(): List<AllScannedUrl>

    @Query("SELECT CASE WHEN COUNT(*) = COALESCE(MAX(id), 0) THEN 0 ELSE 1 END FROM all_scanned_URLs")
    suspend fun hasAllUrlIdGaps(): Int

    // ---- SAFE URLS ----
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSafeUrl(entity: SafeUrl)

    @Query("SELECT * FROM detected_as_safe_urls ORDER BY timestamp DESC")
    fun getAllSafeUrls(): Flow<List<SafeUrl>>

    @Query("DELETE FROM detected_as_safe_urls")
    suspend fun clearSafeUrls()

    @Query("DELETE FROM detected_as_safe_urls WHERE url = :url")
    suspend fun deleteSafeUrlByUrl(url: String)

    @Query("DELETE FROM sqlite_sequence WHERE name='detected_as_safe_urls'")
    suspend fun resetSafeUrlsId()

    // ---- SUSPICIOUS URLS ----
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSuspiciousUrl(entity: SuspiciousUrl)

    @Query("SELECT * FROM detected_as_suspicious_URLs ORDER BY timestamp DESC")
    fun getAllSuspiciousUrls(): Flow<List<SuspiciousUrl>>

    @Query("DELETE FROM detected_as_suspicious_URLs")
    suspend fun clearSuspiciousUrls()

    @Query("DELETE FROM detected_as_suspicious_URLs WHERE url = :url")
    suspend fun deleteSuspiciousUrlByUrl(url: String)

    @Query("DELETE FROM sqlite_sequence WHERE name='detected_as_suspicious_URLs'")
    suspend fun resetSuspiciousUrlsId()

    // ---- PHISHING URLS ----
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPhishingUrl(entity: PhishingUrl)

    @Query("SELECT * FROM detected_as_phishing_URLs ORDER BY timestamp DESC")
    fun getAllPhishingUrls(): Flow<List<PhishingUrl>>

    @Query("SELECT * FROM detected_as_phishing_URLs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentPhishingUrls(limit: Int): Flow<List<PhishingUrl>>

    @Query("SELECT COUNT(*) FROM detected_as_phishing_URLs")
    fun getPhishingCount(): Flow<Int>

    @Query("DELETE FROM detected_as_phishing_URLs")
    suspend fun clearPhishingUrls()

    @Query("DELETE FROM detected_as_phishing_URLs WHERE url = :url")
    suspend fun deletePhishingUrlByUrl(url: String)

    @Query("DELETE FROM sqlite_sequence WHERE name='detected_as_phishing_URLs'")
    suspend fun resetPhishingUrlsId()

    // ---- BLOCKED URLS ----
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBlockedUrl(entity: BlockedUrl)

    @Query("SELECT * FROM blocked_URLs ORDER BY timestamp DESC")
    fun getAllBlockedUrls(): Flow<List<BlockedUrl>>

    @Query("SELECT COUNT(*) FROM blocked_URLs")
    fun getBlockedCount(): Flow<Int>

    @Query("SELECT EXISTS(SELECT 1 FROM blocked_URLs WHERE url = :url LIMIT 1)")
    suspend fun isUrlBlocked(url: String): Boolean

    @Query("DELETE FROM blocked_URLs")
    suspend fun clearBlockedUrls()

    @Query("DELETE FROM sqlite_sequence WHERE name='blocked_URLs'")
    suspend fun resetBlockedUrlsId()

    // ---- UTILS ----
    @androidx.room.Transaction
    suspend fun clearEverything() {
        clearAllUrls()
        resetAllUrlsId()
        clearSafeUrls()
        resetSafeUrlsId()
        clearSuspiciousUrls()
        resetSuspiciousUrlsId()
        clearPhishingUrls()
        resetPhishingUrlsId()
        clearBlockedUrls()
        resetBlockedUrlsId()
    }

    @androidx.room.Transaction
    suspend fun compactAllUrlIdsByScanOrderIfNeeded() {
        if (hasAllUrlIdGaps() == 0) return

        val ordered = getAllScannedUrlsByScanOrderSnapshot()
        clearAllUrls()
        resetAllUrlsId()

        if (ordered.isNotEmpty()) {
            insertAllUrls(ordered.map { it.copy(id = 0) })
        }
    }
}
