package com.zerothreat.core.ui.manual

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zerothreat.core.data.db.AppDatabase
import com.zerothreat.core.data.repository.UrlRepository
import com.zerothreat.core.detector.DnsChecker
import com.zerothreat.core.detector.PhishingDetector
import com.zerothreat.core.detector.PhishingResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ManualCheckViewModel(application: Application) : AndroidViewModel(application) {

    private val _scanResult = MutableStateFlow<ScanEvent?>(null)
    val scanResult = _scanResult.asStateFlow()

    private val _isChecking = MutableStateFlow(false)
    val isChecking = _isChecking.asStateFlow()

    private val dnsChecker = DnsChecker()
    private val urlRepository = UrlRepository(AppDatabase.getDatabase(application).urlDao())

    fun checkUrl(rawUrl: String) {
        if (rawUrl.isBlank()) return

        _isChecking.value = true
        
        viewModelScope.launch(Dispatchers.IO) {
            // Normalize URL (same as SafeBrowserActivity)
            val url = normalizeUrl(rawUrl)

            // Extract domain for DNS check
            val domain = try {
                Uri.parse(url).host ?: ""
            } catch (_: Exception) {
                ""
            }

            if (domain.isBlank()) {
                logScanEvent(
                    url = url,
                    domain = PhishingDetector.normalizeInput(url).ifBlank { "invalid-url" },
                    result = PhishingResult.SUSPICIOUS,
                    score = 50,
                    note = "Invalid URL format",
                    threatType = "Invalid URL"
                )

                withContext(Dispatchers.Main) {
                    _scanResult.value = ScanEvent(
                        url = url,
                        result = PhishingResult.SUSPICIOUS,
                        score = 50,
                        description = "Invalid URL format",
                        dnsCheckFailed = false
                    )
                    _isChecking.value = false
                }
                return@launch
            }

            // STEP 1: DNS CHECK (Same as SafeBrowserActivity and NotificationListener)
            val dnsResult = dnsChecker.checkUrlExists(url)

            if (!dnsResult.exists) {
                // DNS check failed - domain doesn't exist
                logScanEvent(
                    url = url,
                    domain = domain.lowercase(),
                    result = PhishingResult.SUSPICIOUS,
                    score = dnsResult.score,
                    note = dnsResult.message,
                    threatType = "Non-existent domain"
                )

                withContext(Dispatchers.Main) {
                    _scanResult.value = ScanEvent(
                        url = url,
                        result = PhishingResult.SUSPICIOUS,
                        score = dnsResult.score,
                        description = dnsResult.message,
                        dnsCheckFailed = true
                    )
                    _isChecking.value = false
                }
                return@launch
            }

            // STEP 2: PHISHING DETECTION (Same as SafeBrowserActivity and NotificationListener)
            // This includes: blacklist, whitelist, brand similarity, keywords, etc.
            val report = PhishingDetector.analyzeDetailed(getApplication(), url, source = "Manual Check")

            // Save scan result to database
            logScanEvent(
                url = url,
                domain = domain.lowercase(),
                result = report.result,
                score = report.score,
                note = report.description,
                threatType = null
            )

            withContext(Dispatchers.Main) {
                _scanResult.value = ScanEvent(
                    url = url,
                    result = report.result,
                    score = report.score,
                    description = report.description,
                    dnsCheckFailed = false
                )
                _isChecking.value = false
            }
        }
    }

    private fun normalizeUrl(rawUrl: String): String {
        val trimmed = rawUrl.trim()
        return when {
            trimmed.startsWith("http://") -> trimmed.replace("http://", "https://")
            trimmed.startsWith("https://") -> trimmed
            else -> "https://$trimmed"
        }
    }

    fun dismissAlert() {
        _scanResult.value = null
    }

    private suspend fun logScanEvent(
        url: String,
        domain: String,
        result: PhishingResult,
        score: Int,
        note: String,
        threatType: String? = null
    ) {
        try {
            urlRepository.logScan(
                url = url,
                domain = domain,
                result = result,
                source = "Manual Check",
                threatType = threatType,
                phishingScore = score,
                analysisNote = note
            )
        } catch (e: Exception) {
            Log.e("ManualCheckViewModel", "Failed to save manual scan", e)
        }
    }

    data class ScanEvent(
        val url: String,
        val result: PhishingResult,
        val score: Int = 0,
        val description: String = "",
        val dnsCheckFailed: Boolean = false
    )
}
