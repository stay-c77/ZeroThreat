package com.zerothreat.core.detector

import android.content.Context
import android.util.Log
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.ConcurrentHashMap

object WhitelistManager {
    private val allowedDomains = ConcurrentHashMap.newKeySet<String>()
    private val topBrands = ArrayList<String>()
    @Volatile private var isLoaded = false

    // Fallback hardcoded brands in case whitelist file is missing
    private val hardcodedBrands = listOf(
        "google", "youtube", "gmail", "facebook", "instagram", "whatsapp",
        "amazon", "paypal", "apple", "microsoft", "netflix", "twitter",
        "github", "stackoverflow", "wikipedia", "dropbox", "linkedin",
        "reddit", "pinterest", "github", "bitbucket", "gitlab",
        "docker", "heroku", "aws", "azure", "gcp",
        "slack", "discord", "telegram", "skype", "zoom",
        "medium", "dev", "npm", "maven", "gradle"
    )

    fun getTopBrands(limit: Int): List<String> {
        if (!isLoaded) {
            Log.w("WhitelistManager", "Whitelist not loaded, using hardcoded brands")
            return hardcodedBrands.take(limit)
        }
        return if (topBrands.isEmpty()) hardcodedBrands.take(limit) else topBrands.take(limit)
    }

    fun isWhitelisted(domain: String): Boolean {
        if (!isLoaded) {
            // Fallback check against hardcoded brands
            val brandPart = domain.substringBeforeLast(".")
            return hardcodedBrands.contains(brandPart.lowercase())
        }
        return allowedDomains.contains(domain.lowercase())
    }

    @Synchronized
    fun load(context: Context) {
        if (isLoaded) return

        try {
            Log.d("WhitelistManager", "Loading Top 1M Whitelist...")
            val startTime = System.currentTimeMillis()

            context.assets.open("whitelists/top-1m.csv").use { inputStream ->
                BufferedReader(InputStreamReader(inputStream)).useLines { lines ->
                    lines.forEach { line ->
                        val parts = line.split(",")
                        if (parts.size < 2) return@forEach

                        val rank = parts[0].toIntOrNull() ?: Int.MAX_VALUE
                        val domain = parts[1].trim().lowercase()
                        if (domain.isBlank()) return@forEach

                        allowedDomains.add(domain)

                        if (rank <= 1000) {
                            val brandPart = domain.substringBeforeLast(".")
                            if (brandPart.length > 3) {
                                topBrands.add(brandPart)
                            }
                        }
                    }
                }
            }

            isLoaded = true
            Log.d(
                "WhitelistManager",
                "Loaded ${allowedDomains.size} domains + ${topBrands.size} top brands in ${System.currentTimeMillis() - startTime}ms"
            )
        } catch (e: Exception) {
            Log.e("WhitelistManager", "Failed to load whitelist file, will use hardcoded brands: ${e.message}")
            // Still mark as loaded so we use hardcoded brands
            isLoaded = true
        }
    }
}
