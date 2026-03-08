package com.zerothreat.core.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "all_scanned_URLs",
    indices = [Index(value = ["url"], unique = true)]
)
data class AllScannedUrl(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val url: String,
    val domain: String,
    val result: com.zerothreat.core.detector.PhishingResult = com.zerothreat.core.detector.PhishingResult.SAFE,
    val phishingScore: Int = 0,
    val analysisNote: String? = null,
    val source: String,
    val timestamp: Long = System.currentTimeMillis()
)
