package com.zerothreat.core.ui.dashboard

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zerothreat.core.data.db.AppDatabase
import com.zerothreat.core.data.db.ScannedUrl
import com.zerothreat.core.data.repository.UrlRepository
import com.zerothreat.core.detector.PhishingResult
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: UrlRepository
    private val _uiState = MutableStateFlow(DashboardState())
    val uiState: StateFlow<DashboardState> = _uiState.asStateFlow()

    init {
        val urlDao = AppDatabase.getDatabase(application).urlDao()
        repository = UrlRepository(urlDao)

        viewModelScope.launch {
            combine(
                repository.totalScans,
                repository.threatCount,
                repository.blockedCount,
                repository.getRecentPhishingUrls(10) // Get last 10 PHISHING/SUSPICIOUS for alerts
            ) { total, threats, blocked, recent ->
                val safe = total - threats
                
                // Process recent alerts
                val alerts = recent.map { scan ->
                    ThreatAlert(
                        id = scan.id.toString(),
                        url = scan.url,
                        severity = when (scan.result) {
                            PhishingResult.SAFE -> ThreatSeverity.SAFE
                            PhishingResult.SUSPICIOUS -> ThreatSeverity.SUSPICIOUS
                            PhishingResult.PHISHING -> ThreatSeverity.PHISHING
                        },
                        source = scan.source,
                        timeAgo = getTimeAgo(scan.timestamp)
                    )
                }

                // Mock trend data for now (random fluctuation around threat count)
                val trend = List(7) { (0..5).random() } 

                DashboardState(
                    isProtected = true, // We assume VPN/Service is running
                    totalLinksAnalyzed = total,
                    threatsDetected = threats,
                    threatsBlocked = blocked,
                    safeLinks = safe,
                    threatSources = ThreatSources(
                        sms = (total * 0.2).toInt(),
                        browser = (total * 0.5).toInt(),
                        notifications = (total * 0.3).toInt()
                    ),
                    recentAlerts = alerts,
                    trendData = trend
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }

    private fun getTimeAgo(timestamp: Long): String {
        val diff = System.currentTimeMillis() - timestamp
        val minutes = TimeUnit.MILLISECONDS.toMinutes(diff)
        val hours = TimeUnit.MILLISECONDS.toHours(diff)
        val days = TimeUnit.MILLISECONDS.toDays(diff)

        return when {
            minutes < 1 -> "Just now"
            minutes < 60 -> "${minutes}m ago"
            hours < 24 -> "${hours}h ago"
            else -> "${days}d ago"
        }
    }
}
