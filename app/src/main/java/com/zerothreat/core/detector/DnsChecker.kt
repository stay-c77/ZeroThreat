package com.zerothreat.core.detector

import android.util.Log
import java.net.InetAddress
import java.net.UnknownHostException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class DnsChecker {
    private val TAG = "DnsChecker"

    suspend fun checkUrlExists(url: String): DnsCheckResult = withContext(Dispatchers.IO) {
        Log.d(TAG, "Checking URL existence: $url")

        try {
            val domain = extractDomain(url)
            Log.d(TAG, "Extracted domain: $domain")

            if (domain.isEmpty()) {
                Log.e(TAG, "Invalid URL format: $url")
                return@withContext DnsCheckResult(
                    exists = false,
                    message = "Invalid URL format",
                    score = 50
                )
            }

            // Attempt DNS resolution
            val address = InetAddress.getByName(domain)
            Log.d(TAG, "DNS resolved successfully: ${address.hostAddress}")

            return@withContext DnsCheckResult(
                exists = true,
                message = "URL exists - DNS resolved successfully",
                score = 0
            )

        } catch (e: UnknownHostException) {
            Log.w(TAG, "DNS resolution failed for URL: $url - ${e.message}")
            return@withContext DnsCheckResult(
                exists = false,
                message = "URL does not exist - DNS resolution failed",
                score = 75 // Suspicious score for non-existent domains
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error checking URL: ${e.message}", e)
            return@withContext DnsCheckResult(
                exists = false,
                message = "Error checking URL: ${e.message}",
                score = 50 // Moderate score for errors
            )
        }
    }

    private fun extractDomain(url: String): String {
        var domain = url.trim()
        Log.d(TAG, "Original URL: $domain")

        // Remove protocol
        domain = domain.replace(Regex("^https?://"), "")
        domain = domain.replace(Regex("^www\\."), "")

        // Remove path and query parameters
        domain = domain.split("/")[0]
        domain = domain.split("?")[0]

        Log.d(TAG, "Final domain extracted: $domain")
        return domain
    }
}

data class DnsCheckResult(
    val exists: Boolean,
    val message: String,
    val score: Int // 0-100: how suspicious it is if it doesn't exist
)

