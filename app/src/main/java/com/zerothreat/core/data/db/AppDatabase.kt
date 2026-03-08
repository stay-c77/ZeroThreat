package com.zerothreat.core.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.zerothreat.core.data.converters.PhishingResultConverter

@Database(
    entities = [AllScannedUrl::class, SafeUrl::class, SuspiciousUrl::class, PhishingUrl::class, BlockedUrl::class],
    version = 6,
    exportSchema = false
)
@TypeConverters(PhishingResultConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun urlDao(): UrlDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "zerothreat_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
