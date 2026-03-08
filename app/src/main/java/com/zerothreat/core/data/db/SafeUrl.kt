package com.zerothreat.core.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.zerothreat.core.detector.PhishingResult

@Entity(
    tableName = "detected_as_safe_urls",
    indices = [Index(value = ["url"], unique = true)]
)
data class SafeUrl(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val url: String,
    val domain: String,
    val result: PhishingResult = PhishingResult.SAFE,
    val phishingScore: Int = 0,
    val analysisNote: String? = null,
    val source: String,
    val timestamp: Long = System.currentTimeMillis()
)
