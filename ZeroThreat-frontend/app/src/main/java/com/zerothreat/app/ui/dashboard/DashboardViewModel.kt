package com.zerothreat.app.ui.dashboard

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class DashboardViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(createDummyState())
    val uiState: StateFlow<DashboardState> = _uiState.asStateFlow()

    private fun createDummyState(): DashboardState {
        return DashboardState(
            isProtected = true,
            totalLinksAnalyzed = 1247,
            threatsDetected = 38,
            threatsBlocked = 35,
            safeLinks = 1209,
            threatSources = ThreatSources(
                sms = 15,
                browser = 18,
                notifications = 5
            ),
            recentAlerts = listOf(
                ThreatAlert(
                    id = "1",
                    url = "https://fake-bank-login.phish.com",
                    severity = ThreatSeverity.PHISHING,
                    source = "SMS",
                    timeAgo = "2h ago"
                ),
                ThreatAlert(
                    id = "2",
                    url = "https://suspicious-link.xyz/offer",
                    severity = ThreatSeverity.SUSPICIOUS,
                    source = "Browser",
                    timeAgo = "5h ago"
                ),
                ThreatAlert(
                    id = "3",
                    url = "https://malware-download.net",
                    severity = ThreatSeverity.PHISHING,
                    source = "Notification",
                    timeAgo = "1d ago"
                ),
                ThreatAlert(
                    id = "4",
                    url = "https://google.com",
                    severity = ThreatSeverity.SAFE,
                    source = "Browser",
                    timeAgo = "2d ago"
                ),
                ThreatAlert(
                    id = "5",
                    url = "https://scam-prize.win/claim",
                    severity = ThreatSeverity.PHISHING,
                    source = "SMS",
                    timeAgo = "3d ago"
                )
            ),
            trendData = listOf(2, 5, 3, 8, 6, 4, 7), // Last 7 days
            isLoading = false
        )
    }
}