package com.zerothreat.core.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.zerothreat.core.detector.PhishingResult

@Entity(
    tableName = "all_scanned_URLs",
    indices = [Index(value = ["url"], unique = false)] // URLs might repeat in history
)
data class ScannedUrl(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val url: String,
    val domain: String,
    val result: PhishingResult,
    val source: String,
    val timestamp: Long = System.currentTimeMillis(),
    val threatType: String? = null
)
