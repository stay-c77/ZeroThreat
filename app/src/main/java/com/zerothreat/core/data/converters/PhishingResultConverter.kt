package com.zerothreat.core.data.converters

import androidx.room.TypeConverter
import com.zerothreat.core.detector.PhishingResult

class PhishingResultConverter {
    @TypeConverter
    fun fromPhishingResult(result: PhishingResult): String {
        return result.name
    }

    @TypeConverter
    fun toPhishingResult(value: String): PhishingResult {
        return try {
            PhishingResult.valueOf(value)
        } catch (e: Exception) {
            PhishingResult.SUSPICIOUS // Default fallback
        }
    }
}
