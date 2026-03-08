package com.zerothreat.core.data.db

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.zerothreat.core.detector.PhishingResult

@Entity(
    tableName = "blocked_URLs",
    indices = [Index(value = ["url"], unique = true), Index(value = ["domain"], unique = false)]
)
data class BlockedUrl(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val url: String,
    val domain: String,
    val result: PhishingResult = PhishingResult.SUSPICIOUS,
    val phishingScore: Int = 0,
    val analysisNote: String? = null,
    val source: String,
    val timestamp: Long = System.currentTimeMillis()
)
