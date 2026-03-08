package com.zerothreat.core.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.zerothreat.core.detector.PhishingResult

@Entity(
    tableName = "detected_as_phishing_URLs",
    indices = [Index(value = ["url"], unique = true)]
)
data class PhishingUrl(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val url: String,
    val domain: String,
    val result: PhishingResult = PhishingResult.PHISHING,
    val phishingScore: Int = 0,
    val analysisNote: String? = null,
    val source: String,
    val timestamp: Long = System.currentTimeMillis(),
    val threatType: String? = null
)
