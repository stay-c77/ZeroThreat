package com.zerothreat.app.ui.dashboard

data class DashboardState(
    val isProtected: Boolean = true,
    val totalLinksAnalyzed: Int = 0,
    val threatsDetected: Int = 0,
    val threatsBlocked: Int = 0,
    val safeLinks: Int = 0,
    val threatSources: ThreatSources = ThreatSources(),
    val recentAlerts: List<ThreatAlert> = emptyList(),
    val trendData: List<Int> = emptyList(),
    val isLoading: Boolean = false
)

data class ThreatSources(
    val sms: Int = 0,
    val browser: Int = 0,
    val notifications: Int = 0
)

data class ThreatAlert(
    val id: String,
    val url: String,
    val severity: ThreatSeverity,
    val source: String,
    val timeAgo: String
)

enum class ThreatSeverity {
    SAFE, SUSPICIOUS, PHISHING
}