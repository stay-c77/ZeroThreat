package com.zerothreat.core.ui.dashboard

import android.app.role.RoleManager
import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.rememberNavController
import com.zerothreat.core.data.AppPreferences
import com.zerothreat.core.data.db.AllScannedUrl
import com.zerothreat.core.data.db.BlockedUrl
import com.zerothreat.core.detector.PhishingResult
import com.zerothreat.core.ui.theme.*
import com.zerothreat.core.ui.viewmodel.UrlViewModel
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val REPORT_EMAIL = "sharoshalonmathew2026@it.ajce.in"

// ==================== MAIN SCREEN ====================
@Composable
fun DashboardScreen(
    navController: NavController = rememberNavController(),
    viewModel: DashboardViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val urlViewModel: UrlViewModel = viewModel()
    val storedLinks by urlViewModel.recentScans.collectAsState()
    val blockedLinks by urlViewModel.blockedUrls.collectAsState()
    var selectedMetric by remember { mutableStateOf<MetricDetailType?>(null) }
    var showProfileDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val appPreferences = remember { AppPreferences(context) }
    var profileName by remember { mutableStateOf(appPreferences.profileName) }
    var isBrowserRoleEnabled by remember { mutableStateOf(checkBrowserRoleEnabled(context)) }
    var isSmartModeEnabled by remember { mutableStateOf(appPreferences.smartModeEnabled) }

    LaunchedEffect(Unit) {
        isBrowserRoleEnabled = checkBrowserRoleEnabled(context)
        isSmartModeEnabled = appPreferences.smartModeEnabled
    }

    val isProtected = isBrowserRoleEnabled && isSmartModeEnabled

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0F0F10)),
        contentPadding = PaddingValues(bottom = 120.dp, top = 16.dp)
    ) {
        item {
            UserGreetingCard(userName = profileName,
                onAvatarClick = { showProfileDialog = true },
                onEditClick = { showProfileDialog = true })
        }
        item { ProtectionStatusCard(isProtected = isProtected) }
        item {
            if (!isBrowserRoleEnabled || !isSmartModeEnabled) {
                PermissionStatusCard(isBrowserRoleEnabled = isBrowserRoleEnabled,
                    isSmartModeEnabled = isSmartModeEnabled, context = context)
            }
        }
        item {
            ThreatMetricsGrid(totalLinks = uiState.totalLinksAnalyzed,
                threatsDetected = uiState.threatsDetected,
                threatsBlocked = uiState.threatsBlocked,
                safeLinks = uiState.safeLinks,
                onMetricClick = { metric -> selectedMetric = metric })
        }
        item { QuickActionStrip(navController = navController) }
        item { ThreatSourcesSection(sources = uiState.threatSources) }
        item { RecentAlertsSection(alerts = uiState.recentAlerts) }
        item { PrivacyStatement() }
    }

    selectedMetric?.let { metric ->
        val records = filterRecordsForMetric(metric, storedLinks, blockedLinks)
        MetricDetailsDialog(metric = metric, records = records,
            onDismiss = { selectedMetric = null })
    }

    if (showProfileDialog) {
        ProfileDetailsDialog(userName = profileName,
            scannedRecords = storedLinks, blockedRecords = blockedLinks,
            onSaveProfile = { newName ->
                profileName = newName
                appPreferences.profileName = newName
            },
            onDismiss = { showProfileDialog = false })
    }
}

// ==================== SECTION 1A: User Greeting Card ====================
@Composable
fun UserGreetingCard(
    userName: String,
    onAvatarClick: () -> Unit, onEditClick: () -> Unit
) {
    val greeting = remember {
        when (java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)) {
            in 5..11  -> "Good Morning"
            in 12..16 -> "Good Afternoon"
            in 17..20 -> "Good Evening"
            else      -> "Stay Protected"
        }
    }
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        shape = RoundedCornerShape(20.dp), color = CardBackground,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable { onEditClick() }
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(modifier = Modifier.size(44.dp).clickable { onAvatarClick() },
                    shape = CircleShape, color = SurfaceVariant,
                    border = androidx.compose.foundation.BorderStroke(1.dp, NeonTeal.copy(alpha = 0.4f))) {
                    Icon(imageVector = Icons.Default.Person, contentDescription = null,
                        modifier = Modifier.padding(10.dp), tint = NeonTeal)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(text = "$greeting,", color = TextSecondary,
                        style = MaterialTheme.typography.labelMedium)
                    Text(text = userName, color = TextPrimary, fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium)
                }
            }
            Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null,
                tint = TextSecondary, modifier = Modifier.size(20.dp))
        }
    }
}

// ==================== SECTION 1B: Protection Status Card ====================
@Composable
fun ProtectionStatusCard(isProtected: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "shield_pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.2f, targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = EaseInOutSine), repeatMode = RepeatMode.Reverse),
        label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.92f, targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400, easing = EaseInOutSine), repeatMode = RepeatMode.Reverse),
        label = "scale")

    if (isProtected) {
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
            contentAlignment = Alignment.Center) {
            Box(modifier = Modifier.size(200.dp).background(
                brush = Brush.radialGradient(colors = listOf(
                    NeonTeal.copy(alpha = pulseAlpha * 0.25f), Color.Transparent)),
                shape = CircleShape))
            Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp),
                color = CardBackground,
                border = androidx.compose.foundation.BorderStroke(1.5.dp, NeonTeal.copy(alpha = 0.6f))) {
                Column(modifier = Modifier.fillMaxWidth()
                    .padding(vertical = 20.dp, horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(modifier = Modifier.size((52 * pulseScale).dp)
                        .background(color = NeonTeal.copy(alpha = 0.12f), shape = CircleShape),
                        contentAlignment = Alignment.Center) {
                        Icon(imageVector = Icons.Default.Shield, contentDescription = null,
                            tint = NeonTeal.copy(alpha = pulseAlpha + 0.3f),
                            modifier = Modifier.size(28.dp))
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(text = "● PROTECTED", color = NeonTeal, fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.titleMedium,
                        letterSpacing = 3.sp, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "ZeroThreat is actively monitoring your links",
                        color = TextSecondary, style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center)
                }
            }
        }
    }
}

// ==================== SECTION 2: Threat Score Ring ====================
@Composable
fun ThreatMetricsGrid(
    totalLinks: Int, threatsDetected: Int,
    threatsBlocked: Int, safeLinks: Int,
    onMetricClick: (MetricDetailType) -> Unit
) {
    val safeScore = if (totalLinks > 0)
        ((safeLinks.toFloat() / totalLinks.toFloat()) * 100).toInt() else 100

    val targetSweep = if (totalLinks > 0)
        (safeLinks.toFloat() / totalLinks.toFloat()) * 360f else 360f

    val ringColor = when {
        safeScore >= 80 -> NeonTeal
        safeScore >= 50 -> WarningYellow
        else            -> DangerRed
    }

    val animatedSweep = remember { Animatable(0f) }
    val animatedScore = remember { Animatable(0f) }

    LaunchedEffect(targetSweep) {
        animatedSweep.snapTo(0f)
        animatedSweep.animateTo(360f, animationSpec = tween(900, easing = EaseOutCubic))
        animatedSweep.animateTo(targetSweep, animationSpec = tween(600, easing = EaseInOutCubic))
    }
    LaunchedEffect(safeScore) {
        animatedScore.snapTo(0f)
        animatedScore.animateTo(safeScore.toFloat(), animationSpec = tween(1200, easing = EaseOutCubic))
    }

    val displayScore = animatedScore.value.toInt()

    Surface(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        shape = RoundedCornerShape(28.dp), color = CardBackground,
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)) {
        Column(modifier = Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Box(modifier = Modifier.size(170.dp), contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 18.dp.toPx()
                    val inset = strokeWidth / 2f
                    val arcSize = size.width - strokeWidth
                    drawArc(color = BorderColor, startAngle = -90f, sweepAngle = 360f,
                        useCenter = false, topLeft = Offset(inset, inset),
                        size = Size(arcSize, arcSize),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round))
                    drawArc(brush = Brush.sweepGradient(colors = listOf(
                        ringColor.copy(alpha = 0.4f), ringColor, ringColor)),
                        startAngle = -90f, sweepAngle = animatedSweep.value, useCenter = false,
                        topLeft = Offset(inset, inset), size = Size(arcSize, arcSize),
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Round))
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "$displayScore%", color = ringColor,
                        fontWeight = FontWeight.ExtraBold,
                        style = MaterialTheme.typography.headlineLarge)
                    Text(text = "SAFETY SCORE", color = TextSecondary,
                        style = MaterialTheme.typography.labelSmall, letterSpacing = 1.5.sp)
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(text = "$totalLinks scanned · $threatsDetected threats",
                color = TextSecondary, style = MaterialTheme.typography.bodySmall)
            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider(color = BorderColor)
            Spacer(modifier = Modifier.height(16.dp))
            // ── 4 stat pills — equal width, coloured box style ──
            Row(modifier = Modifier.fillMaxWidth()) {
                StatPill("Analyzed", totalLinks, NeonTeal)
                { onMetricClick(MetricDetailType.LINKS_ANALYZED) }
                StatPill("Detected", threatsDetected, WarningYellow)
                { onMetricClick(MetricDetailType.THREATS_DETECTED) }
                StatPill("Blocked", threatsBlocked, DangerRed)
                { onMetricClick(MetricDetailType.THREATS_BLOCKED) }
                StatPill("Safe", safeLinks, SafeGreen)
                { onMetricClick(MetricDetailType.SAFE_LINKS) }
            }
        }
    }
}

// ── StatPill: coloured box with tinted bg + bold border — matches screenshot ──
@Composable
private fun RowScope.StatPill(label: String, value: Int, color: Color, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .weight(1f)
            .padding(horizontal = 4.dp),
        shape = RoundedCornerShape(16.dp),
        color = color.copy(alpha = 0.10f),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, color.copy(alpha = 0.55f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                text = value.toString(),
                color = color,
                fontWeight = FontWeight.ExtraBold,
                style = MaterialTheme.typography.headlineSmall
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = label,
                color = TextSecondary,
                style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun MetricCard(title: String, value: String, icon: ImageVector,
               modifier: Modifier = Modifier, accentColor: Color = NeonTeal, onClick: () -> Unit) {
    Surface(onClick = onClick, modifier = modifier.padding(8.dp).height(130.dp),
        color = CardBackground, shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)) {
        Column(modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween) {
            Icon(imageVector = icon, contentDescription = title,
                tint = accentColor, modifier = Modifier.size(32.dp))
            Column {
                Text(text = value, style = MaterialTheme.typography.headlineMedium,
                    color = TextPrimary, fontWeight = FontWeight.Bold)
                Text(text = title, style = MaterialTheme.typography.labelLarge,
                    color = TextSecondary)
            }
        }
    }
}

@Composable
fun UserProfileHeader(userName: String,
                      onAvatarClick: () -> Unit, onEditClick: () -> Unit) {
    UserGreetingCard(userName, onAvatarClick, onEditClick)
}

// ==================== SECTION 3: Quick Action Strip ====================
@Composable
fun QuickActionStrip(navController: NavController) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // ── Check a Link ──
        Surface(
            modifier = Modifier
                .weight(1f)
                .clickable {
                    navController.navigate("manual") {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
            shape = RoundedCornerShape(18.dp), color = CardBackground,
            border = androidx.compose.foundation.BorderStroke(1.dp, NeonTeal.copy(alpha = 0.5f))
        ) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center) {
                Box(modifier = Modifier.size(36.dp)
                    .background(color = NeonTeal.copy(alpha = 0.12f), shape = CircleShape),
                    contentAlignment = Alignment.Center) {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null,
                        tint = NeonTeal, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(text = "Check a Link", color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyMedium)
                    Text(text = "Scan now", color = NeonTeal,
                        style = MaterialTheme.typography.labelSmall)
                }
            }
        }

        // ── View History ──
        Surface(
            modifier = Modifier
                .weight(1f)
                .clickable {
                    navController.navigate("database") {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
            shape = RoundedCornerShape(18.dp), color = CardBackground,
            border = androidx.compose.foundation.BorderStroke(1.dp, CyberTeal.copy(alpha = 0.4f))
        ) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center) {
                Box(modifier = Modifier.size(36.dp)
                    .background(color = CyberTeal.copy(alpha = 0.12f), shape = CircleShape),
                    contentAlignment = Alignment.Center) {
                    Icon(imageVector = Icons.Default.History, contentDescription = null,
                        tint = CyberTeal, modifier = Modifier.size(18.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(text = "View History", color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        style = MaterialTheme.typography.bodyMedium)
                    Text(text = "All scans", color = CyberTeal,
                        style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}

// ==================== SECTION 4: Intelligence Sources ====================
@Composable
fun ThreatSourcesSection(sources: ThreatSources) {
    Surface(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        color = CardBackground, shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(32.dp)
                    .background(color = NeonTeal.copy(alpha = 0.1f), shape = CircleShape),
                    contentAlignment = Alignment.Center) {
                    Icon(imageVector = Icons.Default.Radar, contentDescription = null,
                        tint = NeonTeal, modifier = Modifier.size(16.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(text = "Intelligence Sources", style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(20.dp))
            val total = (sources.sms + sources.browser + sources.notifications).coerceAtLeast(1)
            SourceRow(Icons.Default.Message, "SMS", sources.sms, sources.sms.toFloat() / total)
            Spacer(modifier = Modifier.height(14.dp))
            SourceRow(Icons.Default.Language, "Web", sources.browser, sources.browser.toFloat() / total)
            Spacer(modifier = Modifier.height(14.dp))
            SourceRow(Icons.Default.Notifications, "Apps", sources.notifications, sources.notifications.toFloat() / total)
            if (sources.sms == 0 && sources.browser == 0 && sources.notifications == 0) {
                Spacer(modifier = Modifier.height(16.dp))
                EmptyStateNote("No sources detected yet. Start browsing to see data.")
            }
        }
    }
}

@Composable
private fun SourceRow(icon: ImageVector, label: String, count: Int, fraction: Float) {
    val animatedFraction by animateFloatAsState(targetValue = fraction,
        animationSpec = tween(900, easing = EaseOutCubic), label = "source_bar")
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Icon(imageVector = icon, contentDescription = label,
            tint = NeonTeal, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = label, color = TextPrimary,
            style = MaterialTheme.typography.bodySmall, modifier = Modifier.width(38.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Box(modifier = Modifier.weight(1f).height(6.dp)
            .clip(RoundedCornerShape(3.dp)).background(SurfaceVariant)) {
            Box(modifier = Modifier.fillMaxHeight()
                .fillMaxWidth(animatedFraction.coerceIn(0f, 1f))
                .clip(RoundedCornerShape(3.dp))
                .background(Brush.horizontalGradient(colors = listOf(CyberTeal, NeonTeal))))
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = count.toString(), color = TextSecondary,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.width(28.dp), textAlign = TextAlign.End)
    }
}

@Composable
fun SourceItem(icon: ImageVector, label: String, count: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(modifier = Modifier.size(56.dp), color = SurfaceVariant,
            shape = RoundedCornerShape(16.dp)) {
            Icon(imageVector = icon, contentDescription = label,
                tint = NeonTeal, modifier = Modifier.padding(14.dp))
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(count.toString(), color = TextPrimary, fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleLarge)
        Text(label, style = MaterialTheme.typography.labelLarge, color = TextSecondary)
    }
}

// ==================== SECTION 5: Live Activity Feed ====================
@Composable
fun RecentAlertsSection(alerts: List<ThreatAlert>) {
    Surface(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        color = CardBackground, shape = RoundedCornerShape(24.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderColor)) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(32.dp)
                    .background(color = NeonTeal.copy(alpha = 0.1f), shape = CircleShape),
                    contentAlignment = Alignment.Center) {
                    Icon(imageVector = Icons.Default.Bolt, contentDescription = null,
                        tint = NeonTeal, modifier = Modifier.size(16.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Text(text = "Live Activity Feed", style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary, fontWeight = FontWeight.Bold)
            }
            Spacer(modifier = Modifier.height(16.dp))
            if (alerts.isEmpty()) {
                EmptyStateNote("No recent threats detected. Your device looks clean ✓")
            } else {
                alerts.take(5).forEachIndexed { index, alert ->
                    val accentColor = when (alert.severity) {
                        ThreatSeverity.SAFE       -> SafeGreen
                        ThreatSeverity.SUSPICIOUS -> WarningYellow
                        ThreatSeverity.PHISHING   -> DangerRed
                    }
                    val isNew = index == 0 && (
                            alert.timeAgo == "Just now" ||
                                    (alert.timeAgo.endsWith("m ago") &&
                                            alert.timeAgo.removeSuffix("m ago").trim()
                                                .toIntOrNull()?.let { it <= 5 } == true))
                    if (index > 0) Spacer(modifier = Modifier.height(8.dp))
                    Surface(modifier = Modifier.fillMaxWidth(),
                        color = SurfaceVariant, shape = RoundedCornerShape(14.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp, accentColor.copy(alpha = 0.25f))) {
                        Row(modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.width(4.dp).height(68.dp)
                                .background(color = accentColor, shape = RoundedCornerShape(
                                    topStart = 14.dp, bottomStart = 14.dp)))
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f).padding(vertical = 12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(text = alert.url, color = TextPrimary,
                                        fontWeight = FontWeight.SemiBold, maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f, fill = false))
                                    if (isNew) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        NewPulseBadge()
                                    }
                                }
                                Spacer(modifier = Modifier.height(5.dp))
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(shape = RoundedCornerShape(6.dp),
                                        color = accentColor.copy(alpha = 0.12f),
                                        border = androidx.compose.foundation.BorderStroke(
                                            1.dp, accentColor.copy(alpha = 0.3f))) {
                                        Text(text = alert.source, color = accentColor,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp))
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(text = alert.timeAgo, color = TextSecondary,
                                        style = MaterialTheme.typography.labelSmall)
                                }
                            }
                            Icon(Icons.Default.ChevronRight, null, tint = TextSecondary,
                                modifier = Modifier.padding(end = 12.dp).size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NewPulseBadge() {
    val infiniteTransition = rememberInfiniteTransition(label = "new_badge")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.5f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = EaseInOutSine), repeatMode = RepeatMode.Reverse),
        label = "new_alpha")
    Surface(shape = RoundedCornerShape(4.dp),
        color = DangerRed.copy(alpha = alpha * 0.2f),
        border = androidx.compose.foundation.BorderStroke(1.dp, DangerRed.copy(alpha = alpha))) {
        Text(text = "NEW", color = DangerRed.copy(alpha = alpha),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp,
            modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp))
    }
}

@Composable
private fun EmptyStateNote(message: String) {
    Row(modifier = Modifier.fillMaxWidth()
        .background(color = SurfaceVariant, shape = RoundedCornerShape(10.dp))
        .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = Icons.Default.Info, contentDescription = null,
            tint = NeonTeal.copy(alpha = 0.6f), modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = message, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
    }
}

// ==================== SECTION 6: Privacy Footer ====================
@Composable
fun PrivacyStatement() {
    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 20.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center) {
        Icon(Icons.Default.Lock, null, tint = NeonTeal.copy(alpha = 0.5f),
            modifier = Modifier.size(13.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text(text = "Local-only analysis · No data leaves your device",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary.copy(alpha = 0.6f))
    }
}

// ==================== Permission Warning Card ====================
@Composable
private fun PermissionStatusCard(isBrowserRoleEnabled: Boolean,
                                 isSmartModeEnabled: Boolean, context: Context) {
    Surface(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
        color = CardBackground, shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, WarningYellow.copy(alpha = 0.4f))) {
        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Warning, null, tint = WarningYellow, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.width(14.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = "Protection Limited", color = TextPrimary, fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.bodyMedium)
                Text(text = "Enable Link Protection for full security.",
                    color = TextSecondary, style = MaterialTheme.typography.bodySmall)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Button(onClick = { if (!isBrowserRoleEnabled) requestBrowserRole(context) },
                colors = ButtonDefaults.buttonColors(containerColor = WarningYellow),
                shape = RoundedCornerShape(12.dp),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)) {
                Text(text = "Setup", color = PureBlack, fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelMedium)
            }
        }
    }
}

// ==================== Profile Dialog ====================
@Composable
private fun ProfileDetailsDialog(
    userName: String,
    scannedRecords: List<AllScannedUrl>, blockedRecords: List<BlockedUrl>,
    onSaveProfile: (String) -> Unit, onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var draftName by remember(userName) { mutableStateOf(userName) }
    var isEditing by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }
    var reportUrl by remember { mutableStateOf("") }
    var reportDescription by remember { mutableStateOf("") }
    var showUrlPicker by remember { mutableStateOf(false) }
    val selectableRecords = remember(scannedRecords) { scannedRecords.sortedBy { it.id } }

    AlertDialog(onDismissRequest = onDismiss,
        icon = {
            Surface(modifier = Modifier.size(56.dp), shape = CircleShape, color = SurfaceVariant) {
                Icon(imageVector = Icons.Default.Person, contentDescription = null,
                    tint = NeonTeal, modifier = Modifier.padding(14.dp))
            }
        },
        title = {
            Text("Profile & History", color = TextPrimary,
                style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (isEditing) {
                    OutlinedTextField(value = draftName, onValueChange = { draftName = it },
                        label = { Text("Name") }, singleLine = true,
                        modifier = Modifier.fillMaxWidth())
                    Button(onClick = {
                        val finalName = draftName.trim().ifBlank { "ZeroThreat User" }
                        onSaveProfile(finalName)
                        draftName = finalName
                        isEditing = false
                    }, modifier = Modifier.fillMaxWidth()) { Text("Save Details") }
                } else {
                    Text(draftName, color = TextPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold)
                    OutlinedButton(onClick = { isEditing = true },
                        modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Edit, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Edit Details")
                    }
                }
                Surface(modifier = Modifier.fillMaxWidth(), color = SurfaceVariant,
                    shape = RoundedCornerShape(12.dp)) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("History Summary", style = MaterialTheme.typography.labelLarge,
                            color = TextPrimary, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Scanned URLs: ${scannedRecords.size}",
                            style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        Text("Blocked URLs: ${blockedRecords.size}",
                            style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                }
                Button(onClick = {
                    scope.launch {
                        isExporting = true
                        val result = exportDatabaseHistoryPdf(context, draftName,
                            scannedRecords, blockedRecords)
                        isExporting = false
                        result.onSuccess { location ->
                            Toast.makeText(context, "PDF saved: $location", Toast.LENGTH_LONG).show()
                        }.onFailure {
                            Toast.makeText(context, "Failed to export PDF", Toast.LENGTH_LONG).show()
                        }
                    }
                }, enabled = !isExporting, modifier = Modifier.fillMaxWidth()) {
                    if (isExporting) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        Spacer(modifier = Modifier.width(8.dp))
                    } else {
                        Icon(Icons.Default.Download, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("Download Full History PDF")
                }
                HorizontalDivider(color = BorderColor)
                Text("Report a Specific Link", style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary, fontWeight = FontWeight.SemiBold)
                OutlinedTextField(value = reportUrl, onValueChange = { reportUrl = it },
                    label = { Text("URL to report") }, singleLine = true,
                    modifier = Modifier.fillMaxWidth())
                OutlinedButton(onClick = { showUrlPicker = true },
                    enabled = selectableRecords.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Link, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (selectableRecords.isNotEmpty())
                        "Select URL From Scanned List" else "No Scanned URLs Available")
                }
                OutlinedTextField(value = reportDescription,
                    onValueChange = { reportDescription = it },
                    label = { Text("Description") }, minLines = 3,
                    modifier = Modifier.fillMaxWidth())
                Button(onClick = {
                    sendReportEmail(context, draftName, reportUrl, reportDescription)
                }, enabled = reportUrl.isNotBlank(), modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Email, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Send Report Email")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close", color = NeonTeal) }
        },
        dismissButton = {},
        containerColor = CardBackground,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary
    )

    if (showUrlPicker) {
        AlertDialog(onDismissRequest = { showUrlPicker = false },
            title = { Text("Select URL To Report", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                if (selectableRecords.isEmpty()) {
                    Text("No scanned URLs available.", color = TextSecondary)
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp)) {
                        items(items = selectableRecords, key = { it.id }) { record ->
                            Column(modifier = Modifier.fillMaxWidth()
                                .clickable { reportUrl = record.url; showUrlPicker = false }
                                .padding(vertical = 10.dp)) {
                                Text("ID ${record.id}", color = TextSecondary,
                                    style = MaterialTheme.typography.labelSmall)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(record.url, color = TextPrimary, maxLines = 1,
                                    overflow = TextOverflow.Ellipsis)
                            }
                            HorizontalDivider(color = BorderColor)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showUrlPicker = false }) { Text("Close", color = NeonTeal) }
            },
            containerColor = CardBackground,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary)
    }
}

// ==================== Metric Details ====================
enum class MetricDetailType { LINKS_ANALYZED, THREATS_DETECTED, THREATS_BLOCKED, SAFE_LINKS }

private fun filterRecordsForMetric(metric: MetricDetailType,
                                   records: List<AllScannedUrl>, blockedRecords: List<BlockedUrl>): List<AllScannedUrl> {
    return when (metric) {
        MetricDetailType.LINKS_ANALYZED   -> records
        MetricDetailType.THREATS_DETECTED -> records.filter {
            it.result == PhishingResult.PHISHING || it.result == PhishingResult.SUSPICIOUS }
        MetricDetailType.THREATS_BLOCKED  -> blockedRecords.map { b ->
            AllScannedUrl(b.id, b.url, b.domain, b.result, b.phishingScore,
                b.analysisNote, b.source, b.timestamp) }
        MetricDetailType.SAFE_LINKS       -> records.filter { it.result == PhishingResult.SAFE }
    }.sortedBy { it.id }
}

@Composable
private fun MetricDetailsDialog(metric: MetricDetailType,
                                records: List<AllScannedUrl>, onDismiss: () -> Unit) {
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text(metricTitle(metric), color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(modifier = Modifier.fillMaxWidth().height(400.dp)) {
                items(records) { item ->
                    MetricDetailItem(item, metric == MetricDetailType.THREATS_DETECTED)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close", color = NeonTeal) } },
        containerColor = CardBackground)
}

@Composable
private fun MetricDetailItem(item: AllScannedUrl, showStatusBadge: Boolean) {
    val (statusLabel, statusColor) = threatStatusMeta(item.result)
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        if (showStatusBadge) {
            Surface(shape = RoundedCornerShape(999.dp),
                color = statusColor.copy(alpha = 0.16f),
                border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.4f))) {
                Text(statusLabel, color = statusColor,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
        Text(item.domain, color = TextPrimary, fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium)
        Text(item.url, color = TextSecondary,
            style = MaterialTheme.typography.bodySmall, maxLines = 1)
        HorizontalDivider(modifier = Modifier.padding(top = 12.dp), color = BorderColor)
    }
}

// ==================== Helpers ====================
private fun threatStatusMeta(result: PhishingResult): Pair<String, Color> {
    return when (result) {
        PhishingResult.SAFE       -> "SAFE"       to SafeGreen
        PhishingResult.SUSPICIOUS -> "SUSPICIOUS" to WarningYellow
        PhishingResult.PHISHING   -> "PHISHING"   to DangerRed
    }
}

private fun metricTitle(metric: MetricDetailType) = metric.name.replace("_", " ")

private fun checkBrowserRoleEnabled(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
    val rm = context.getSystemService(Context.ROLE_SERVICE) as? RoleManager ?: return false
    return rm.isRoleHeld(RoleManager.ROLE_BROWSER)
}

private fun requestBrowserRole(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val rm = context.getSystemService(Context.ROLE_SERVICE) as? RoleManager
        val intent = rm?.createRequestRoleIntent(RoleManager.ROLE_BROWSER)
        intent?.let { context.startActivity(it.apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }) }
    }
}

private suspend fun exportDatabaseHistoryPdf(
    context: Context, userName: String,
    scannedRecords: List<AllScannedUrl>, blockedRecords: List<BlockedUrl>
): Result<String> = withContext(Dispatchers.IO) {
    runCatching {
        val fileName = "zerothreat_history_${
            SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.pdf"
        val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val generatedAtFormatter = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        val titlePaint = Paint().apply { color = android.graphics.Color.BLACK; textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
        val subtitlePaint = Paint().apply { color = android.graphics.Color.DKGRAY; textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL) }
        val sectionPaint = Paint().apply { color = android.graphics.Color.BLACK; textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
        val bodyPaint = Paint().apply { color = android.graphics.Color.BLACK; textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL) }
        val tableHeaderPaint = Paint().apply { color = android.graphics.Color.BLACK; textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
        val tableCellPaint = Paint().apply { color = android.graphics.Color.BLACK; textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL) }
        val tableLinePaint = Paint().apply {
            color = android.graphics.Color.DKGRAY; strokeWidth = 1f }
        val headerFillPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#ECEFF3"); style = Paint.Style.FILL }

        val scannedSorted = scannedRecords.sortedBy { it.id }
        val blockedSorted = blockedRecords.sortedBy { it.id }
        val historyRows = buildList {
            scannedSorted.forEach { add(PdfHistoryRow(it.id, it.timestamp, it.url, it.result.name)) }
            blockedSorted.forEach { add(PdfHistoryRow(it.id, it.timestamp, it.url, it.result.name)) }
        }.sortedBy { it.id }

        val document = PdfDocument()
        try {
            val pageWidth = 595; val pageHeight = 842
            val margin = 36f; val contentBottom = pageHeight - margin; val lineHeight = 17f
            val tableXStart = margin; val idColWidth = 48f; val dateColWidth = 130f
            val urlColWidth = 275f
            val resultColWidth = pageWidth - (margin * 2) - idColWidth - dateColWidth - urlColWidth
            val tableXId = tableXStart; val tableXDate = tableXId + idColWidth
            val tableXUrl = tableXDate + dateColWidth; val tableXResult = tableXUrl + urlColWidth
            val tableXEnd = tableXResult + resultColWidth; val rowHeight = 22f
            var pageNumber = 1
            var page = document.startPage(
                PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
            var canvas = page.canvas; var y = margin

            fun ellipsize(text: String, max: Int) =
                if (text.length <= max) text else text.take(max - 3) + "..."

            fun drawTableHeader() {
                canvas.drawRect(tableXStart, y, tableXEnd, y + rowHeight, headerFillPaint)
                canvas.drawLine(tableXStart, y, tableXEnd, y, tableLinePaint)
                canvas.drawLine(tableXStart, y + rowHeight, tableXEnd, y + rowHeight, tableLinePaint)
                canvas.drawLine(tableXStart, y, tableXStart, y + rowHeight, tableLinePaint)
                canvas.drawLine(tableXDate, y, tableXDate, y + rowHeight, tableLinePaint)
                canvas.drawLine(tableXUrl, y, tableXUrl, y + rowHeight, tableLinePaint)
                canvas.drawLine(tableXResult, y, tableXResult, y + rowHeight, tableLinePaint)
                canvas.drawLine(tableXEnd, y, tableXEnd, y + rowHeight, tableLinePaint)
                val textY = y + 15f
                canvas.drawText("ID", tableXId + 6f, textY, tableHeaderPaint)
                canvas.drawText("Scanned Date", tableXDate + 6f, textY, tableHeaderPaint)
                canvas.drawText("URL", tableXUrl + 6f, textY, tableHeaderPaint)
                canvas.drawText("Result", tableXResult + 6f, textY, tableHeaderPaint)
                y += rowHeight
            }

            fun startNewPage(sectionTitle: String) {
                document.finishPage(page); pageNumber += 1
                page = document.startPage(
                    PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
                canvas = page.canvas; y = margin
                canvas.drawText("ZeroThreat URL Scan History (contd.)", margin, y, sectionPaint)
                y += lineHeight
                canvas.drawText("Page $pageNumber", margin, y, subtitlePaint)
                y += lineHeight + 6f
                canvas.drawText(sectionTitle, margin, y, sectionPaint)
                y += lineHeight; drawTableHeader()
            }

            canvas.drawText("ZeroThreat URL Scan History", margin, y, titlePaint)
            y += lineHeight + 4f
            canvas.drawText("User: $userName", margin, y, subtitlePaint); y += lineHeight
            canvas.drawText("Generated: ${generatedAtFormatter.format(Date())}", margin, y, subtitlePaint); y += lineHeight
            canvas.drawText("Total Records: ${historyRows.size}", margin, y, subtitlePaint); y += lineHeight + 8f
            val sectionTitle = "ID | Scanned Date | URL | Result"
            canvas.drawText(sectionTitle, margin, y, sectionPaint); y += lineHeight
            if (historyRows.isEmpty()) {
                canvas.drawText("No history records available.", margin, y, bodyPaint); y += lineHeight
            } else {
                drawTableHeader()
                historyRows.forEach { row ->
                    if (y + rowHeight > contentBottom) startNewPage(sectionTitle)
                    canvas.drawLine(tableXStart, y, tableXEnd, y, tableLinePaint)
                    canvas.drawLine(tableXStart, y + rowHeight, tableXEnd, y + rowHeight, tableLinePaint)
                    canvas.drawLine(tableXStart, y, tableXStart, y + rowHeight, tableLinePaint)
                    canvas.drawLine(tableXDate, y, tableXDate, y + rowHeight, tableLinePaint)
                    canvas.drawLine(tableXUrl, y, tableXUrl, y + rowHeight, tableLinePaint)
                    canvas.drawLine(tableXResult, y, tableXResult, y + rowHeight, tableLinePaint)
                    canvas.drawLine(tableXEnd, y, tableXEnd, y + rowHeight, tableLinePaint)
                    val rowTextY = y + 15f
                    canvas.drawText(row.id.toString(), tableXId + 6f, rowTextY, tableCellPaint)
                    canvas.drawText(dateFormatter.format(Date(row.timestamp)), tableXDate + 6f, rowTextY, tableCellPaint)
                    canvas.drawText(ellipsize(row.url, 54), tableXUrl + 6f, rowTextY, tableCellPaint)
                    canvas.drawText(ellipsize(row.result, 11), tableXResult + 6f, rowTextY, tableCellPaint)
                    y += rowHeight
                }
            }
            document.finishPage(page)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val resolver = context.contentResolver
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH,
                        "${Environment.DIRECTORY_DOWNLOADS}/ZeroThreat")
                }
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: error("Unable to create output file")
                resolver.openOutputStream(uri).use { stream ->
                    checkNotNull(stream) { "Unable to open output stream" }
                    document.writeTo(stream)
                }
                "Downloads/ZeroThreat/$fileName"
            } else {
                val downloadDir = Environment
                    .getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val outputDir = File(downloadDir, "ZeroThreat").apply { mkdirs() }
                val outputFile = File(outputDir, fileName)
                FileOutputStream(outputFile).use { document.writeTo(it) }
                outputFile.absolutePath
            }
        } finally { document.close() }
    }
}

private data class PdfHistoryRow(
    val id: Int, val timestamp: Long, val url: String, val result: String)

private fun sendReportEmail(context: Context, reporterName: String,
                            reportUrl: String, reportDescription: String) {
    val safeReporter = reporterName.trim().ifBlank { "ZeroThreat User" }
    val safeDescription = reportDescription.trim().ifBlank { "No additional description provided." }
    val body = buildString {
        appendLine("Hello ZeroThreat Team,")
        appendLine()
        appendLine("A URL report was submitted from the app.")
        appendLine("Reporter: $safeReporter")
        appendLine("URL: ${reportUrl.trim()}")
        appendLine()
        appendLine("Description:")
        appendLine(safeDescription)
    }
    val emailIntent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$REPORT_EMAIL")).apply {
        putExtra(Intent.EXTRA_EMAIL, arrayOf(REPORT_EMAIL))
        putExtra(Intent.EXTRA_SUBJECT, "ZeroThreat URL Report")
        putExtra(Intent.EXTRA_TEXT, body)
    }
    try {
        context.startActivity(emailIntent)
    } catch (_: ActivityNotFoundException) {
        Toast.makeText(context, "No email app found on this device.", Toast.LENGTH_LONG).show()
    }
}