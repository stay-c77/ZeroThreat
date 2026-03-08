package com.zerothreat.app.ui.dashboard

import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.zerothreat.app.R
import androidx.compose.foundation.Canvas
import androidx.compose.foundation. background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation. layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx. compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout. fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout. padding
import androidx.compose.foundation.layout.size
import androidx. compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx. compose.foundation.shape.RoundedCornerShape
import androidx. compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx. compose.material.icons.filled. CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Language
import androidx.compose.material. icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose. material.icons.filled.Message
import androidx.compose.material.icons.filled. Notifications
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons. filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text. font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zerothreat.app.ui.theme.CardBackground
import com.zerothreat.app.ui.theme.DangerRed
import com.zerothreat.app.ui.theme.DarkBackground
import com.zerothreat.app.ui.theme.ElectricPurple
import com.zerothreat.app.ui.theme.ElectricPurpleLight
import com.zerothreat.app.ui.theme.PureBlack
import com.zerothreat.app.ui.theme.PurpleGlow
import com.zerothreat.app.ui.theme.SafeGreen
import com.zerothreat.app.ui.theme.TextMuted
import com.zerothreat.app.ui.theme.TextPrimary
import com.zerothreat.app.ui.theme.TextSecondary
import com.zerothreat.app.ui.theme.WarningYellow
@Composable
fun DashboardScreen(
    viewModel:  DashboardViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground),
        contentPadding = PaddingValues(0.dp)
    ) {
        // SECTION 1: Security Status Header (Gradient)
        item {
            SecurityStatusHeader(isProtected = uiState.isProtected)
        }

        // SECTION 2: Threat Intelligence Summary (Metrics Grid)
        item {
            Spacer(modifier = Modifier.height(16.dp))
            if (uiState.totalLinksAnalyzed > 0) {
                ThreatMetricsGrid(
                    totalLinks = uiState.totalLinksAnalyzed,
                    threatsDetected = uiState.threatsDetected,
                    threatsBlocked = uiState.threatsBlocked,
                    safeLinks = uiState.safeLinks
                )
            } else {
                EmptyStateCard()
            }
        }

        // SECTION 3: Threat Trend Chart
        item {
            if (uiState.trendData.isNotEmpty()) {
                Spacer(modifier = Modifier. height(16.dp))
                ThreatTrendChart(trendData = uiState.trendData)
            }
        }

        // SECTION 4: Threat Sources Overview
        item {
            if (uiState.totalLinksAnalyzed > 0) {
                Spacer(modifier = Modifier.height(16.dp))
                ThreatSourcesSection(sources = uiState.threatSources)
            }
        }

        // SECTION 5: Recent Alerts Feed
        item {
            if (uiState.recentAlerts.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                RecentAlertsSection(alerts = uiState.recentAlerts)
            }
        }

        // SECTION 6: Privacy Statement
        item {
            Spacer(modifier = Modifier.height(16.dp))
            PrivacyStatement()
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

// ==================== SECTION 1: Header ====================
@Composable
fun SecurityStatusHeader(isProtected: Boolean) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)  // Increased height for logo
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        PureBlack,
                        PurpleGlow. copy(alpha = 0.3f),
                        ElectricPurple.copy(alpha = 0.2f)
                    )
                )
            ),
        contentAlignment = Alignment. Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // ✅ YOUR CUSTOM SHIELD LOGO
            Image(
                painter = painterResource(id = R.drawable.shield_logo),
                contentDescription = "ZeroThreat Shield",
                modifier = Modifier. size(100.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (isProtected) "PROTECTED" else "MONITORING",
                style = MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier. height(4.dp))
            Text(
                text = "ZeroThreat is actively monitoring links across apps",
                style = MaterialTheme. typography.bodyMedium,
                color = TextSecondary
            )
        }
    }
}

// ==================== SECTION 2: Metrics Grid ====================
@Composable
fun ThreatMetricsGrid(
    totalLinks:  Int,
    threatsDetected: Int,
    threatsBlocked: Int,
    safeLinks: Int
) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Text(
            text = "Security Overview",
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            MetricCard(
                title = "Links Analyzed",
                value = totalLinks.toString(),
                icon = Icons.Default.Link,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(12.dp))
            MetricCard(
                title = "Threats Detected",
                value = threatsDetected.toString(),
                icon = Icons.Default.Warning,
                modifier = Modifier.weight(1f),
                accentColor = WarningYellow
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        Row(modifier = Modifier.fillMaxWidth()) {
            MetricCard(
                title = "Threats Blocked",
                value = threatsBlocked.toString(),
                icon = Icons.Default.Block,
                modifier = Modifier.weight(1f),
                accentColor = DangerRed
            )
            Spacer(modifier = Modifier.width(12.dp))
            MetricCard(
                title = "Safe Links",
                value = safeLinks.toString(),
                icon = Icons.Default.CheckCircle,
                modifier = Modifier.weight(1f),
                accentColor = SafeGreen
            )
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    modifier:  Modifier = Modifier,
    accentColor: Color = ElectricPurple
) {
    Card(
        modifier = modifier
            .height(120.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = accentColor,
                modifier = Modifier.size(28.dp)
            )
            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }
    }
}

// ==================== SECTION 3: Trend Chart ====================
@Composable
fun ThreatTrendChart(trendData: List<Int>) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(180.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            Text(
                text = "Threat Activity (Last 7 Days)",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(12.dp))

            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
            ) {
                val maxValue = trendData.maxOrNull() ?: 1
                val pointSpacing = size.width / (trendData. size - 1)

                val path = Path()
                trendData.forEachIndexed { index, value ->
                    val x = index * pointSpacing
                    val y = size.height - (value.toFloat() / maxValue * size.height)

                    if (index == 0) {
                        path.moveTo(x, y)
                    } else {
                        path.lineTo(x, y)
                    }
                }

                drawPath(
                    path = path,
                    color = ElectricPurple,
                    style = Stroke(width = 4f)
                )

                // Draw points
                trendData.forEachIndexed { index, value ->
                    val x = index * pointSpacing
                    val y = size.height - (value.toFloat() / maxValue * size.height)
                    drawCircle(
                        color = ElectricPurpleLight,
                        radius = 6f,
                        center = Offset(x, y)
                    )
                }
            }
        }
    }
}

// ==================== SECTION 4: Threat Sources ====================
@Composable
fun ThreatSourcesSection(sources: ThreatSources) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults. cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Threat Sources",
                style = MaterialTheme. typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                SourceItem(
                    icon = Icons.Default.Message,
                    label = "SMS",
                    count = sources.sms
                )
                SourceItem(
                    icon = Icons.Default.Language,
                    label = "Browser",
                    count = sources.browser
                )
                SourceItem(
                    icon = Icons.Default.Notifications,
                    label = "Notifications",
                    count = sources.notifications
                )
            }
        }
    }
}

@Composable
fun SourceItem(icon: ImageVector, label: String, count: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = ElectricPurple,
            modifier = Modifier.size(32.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary
        )
    }
}

// ==================== SECTION 5: Recent Alerts ====================
@Composable
fun RecentAlertsSection(alerts: List<ThreatAlert>) {
    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
        Row(
            modifier = Modifier. fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recent Activity",
                style = MaterialTheme.typography.titleMedium,
                color = TextPrimary,
                fontWeight = FontWeight.SemiBold
            )
            TextButton(onClick = { /* Navigate to full logs */ }) {
                Text(text = "View All", color = ElectricPurple)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        alerts.take(5).forEach { alert ->
            AlertItem(alert = alert)
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun AlertItem(alert: ThreatAlert) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Severity Indicator
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(
                        when (alert.severity) {
                            ThreatSeverity.SAFE -> SafeGreen
                            ThreatSeverity.SUSPICIOUS -> WarningYellow
                            ThreatSeverity.PHISHING -> DangerRed
                        }
                    )
            )
            Spacer(modifier = Modifier. width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = alert.url,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextPrimary,
                    maxLines = 1
                )
                Spacer(modifier = Modifier. height(4.dp))
                Row {
                    Text(
                        text = alert.source,
                        style = MaterialTheme.typography.bodySmall,
                        color = ElectricPurple,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = alert.timeAgo,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted,
                        fontSize = 11.sp
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "View details",
                tint = TextMuted,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ==================== SECTION 6: Privacy Statement ====================
@Composable
fun PrivacyStatement() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = ElectricPurple.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier. padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Privacy",
                tint = ElectricPurple,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "All analysis is performed locally on your device.  No data is uploaded.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                lineHeight = 18.sp
            )
        }
    }
}

// ==================== EMPTY STATE ====================
@Composable
fun EmptyStateCard() {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(200.dp),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = "Ready",
                tint = ElectricPurple,
                modifier = Modifier.size(64.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "ZeroThreat is Ready",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "It will automatically analyze links you receive\nin messages, emails, and apps.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
        }
    }
}