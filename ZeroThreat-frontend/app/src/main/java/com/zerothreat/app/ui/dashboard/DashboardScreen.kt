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
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
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

private const val REPORT_EMAIL = "sharonshalon94@gmail.com"

// ── Protection state enum (UI-only, no backend touch) ────────────────────────
private enum class ProtectionStatus { PROTECTED, PARTIAL, UNPROTECTED }

private fun resolveProtectionStatus(
    isBrowserRoleEnabled: Boolean,
    isSmartModeEnabled: Boolean
): ProtectionStatus = when {
    isBrowserRoleEnabled && isSmartModeEnabled  -> ProtectionStatus.PROTECTED
    isBrowserRoleEnabled || isSmartModeEnabled  -> ProtectionStatus.PARTIAL
    else                                         -> ProtectionStatus.UNPROTECTED
}

// ── Dashboard root ─────────────────────────────────────────────────────────────
@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = viewModel()
) {
    val hazeState        = LocalUiSurfaceState.current
    val uiState          by viewModel.uiState.collectAsState()
    val urlViewModel: UrlViewModel = viewModel()
    val storedLinks      by urlViewModel.recentScans.collectAsState()
    val blockedLinks     by urlViewModel.blockedUrls.collectAsState()
    var selectedMetric   by remember { mutableStateOf<MetricDetailType?>(null) }
    var showProfileDialog by remember { mutableStateOf(false) }
    val context          = LocalContext.current
    val appPreferences   = remember { AppPreferences(context) }
    var profileName      by remember { mutableStateOf(appPreferences.profileName) }
    var profileRole      by remember { mutableStateOf(appPreferences.profileRole) }
    var isBrowserRoleEnabled by remember { mutableStateOf(checkBrowserRoleEnabled(context)) }
    var isSmartModeEnabled   by remember { mutableStateOf(appPreferences.smartModeEnabled) }

    // ── Stats ring expand state ───────────────────────────────────────────────
    var isRingExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isBrowserRoleEnabled = checkBrowserRoleEnabled(context)
        isSmartModeEnabled   = appPreferences.smartModeEnabled
    }

    val protectionStatus = resolveProtectionStatus(isBrowserRoleEnabled, isSmartModeEnabled)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF011B1A), Color(0xFF042F2E))
                )
            ),
        contentPadding = PaddingValues(bottom = 120.dp, top = 0.dp)
    ) {

        // ── SECTION 1: Hero Protection Banner ─────────────────────────────────
        item {
            HeroProtectionBanner(
                protectionStatus  = protectionStatus,
                userName          = profileName,
                userRole          = profileRole,
                onUserNameClick   = { showProfileDialog = true },
                onSetupClick      = { requestBrowserRole(context) }
            )
        }

        // ── SECTION 2: Threat Score Ring ──────────────────────────────────────
        item {
            ThreatScoreRingSection(
                totalLinks      = uiState.totalLinksAnalyzed,
                safeLinks       = uiState.safeLinks,
                threatsDetected = uiState.threatsDetected,
                threatsBlocked  = uiState.threatsBlocked,
                isExpanded      = isRingExpanded,
                onRingClick     = { isRingExpanded = !isRingExpanded },
                onMetricClick   = { metric -> selectedMetric = metric }
            )
        }

        // ── SECTION 4: Threat Sources Overview ────────────────────────────────
        item {
            if (uiState.totalLinksAnalyzed > 0) {
                ThreatSourcesSection(sources = uiState.threatSources)
            }
        }

        // ── SECTION 5: Recent Alerts Feed ─────────────────────────────────────
        item {
            if (uiState.recentAlerts.isNotEmpty()) {
                RecentAlertsSection(alerts = uiState.recentAlerts)
            }
        }

        // ── SECTION 6: Privacy Statement ──────────────────────────────────────
        item {
            PrivacyStatement()
        }
    }

    selectedMetric?.let { metric ->
        val records = filterRecordsForMetric(metric, storedLinks, blockedLinks)
        MetricDetailsDialog(
            metric   = metric,
            records  = records,
            onDismiss = { selectedMetric = null }
        )
    }

    if (showProfileDialog) {
        ProfileDetailsDialog(
            userName       = profileName,
            userRole       = profileRole,
            scannedRecords = storedLinks,
            blockedRecords = blockedLinks,
            onSaveProfile  = { newName, newRole ->
                profileName              = newName
                profileRole              = newRole
                appPreferences.profileName = newName
                appPreferences.profileRole = newRole
            },
            onDismiss = { showProfileDialog = false }
        )
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// SECTION 1 — Hero Protection Banner
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun HeroProtectionBanner(
    protectionStatus : ProtectionStatus,
    userName         : String,
    userRole         : String,
    onUserNameClick  : () -> Unit,
    onSetupClick     : () -> Unit
) {
    // ── Resolve colors & copy per state ──────────────────────────────────────
    val statusColor = when (protectionStatus) {
        ProtectionStatus.PROTECTED   -> ProtectedColor
        ProtectionStatus.PARTIAL     -> PartialColor
        ProtectionStatus.UNPROTECTED -> UnprotectedColor
    }
    val statusLabel = when (protectionStatus) {
        ProtectionStatus.PROTECTED   -> "PROTECTED"
        ProtectionStatus.PARTIAL     -> "PARTIAL PROTECTION"
        ProtectionStatus.UNPROTECTED -> "NOT PROTECTED"
    }
    val statusSubtitle = when (protectionStatus) {
        ProtectionStatus.PROTECTED   -> "ZeroThreat is actively monitoring your links"
        ProtectionStatus.PARTIAL     -> "Some protections are disabled — tap Setup to fix"
        ProtectionStatus.UNPROTECTED -> "Link protection is off — enable it now"
    }
    val statusIcon = when (protectionStatus) {
        ProtectionStatus.PROTECTED   -> Icons.Default.Shield
        ProtectionStatus.PARTIAL     -> Icons.Default.ShieldMoon  // fallback below if missing
        ProtectionStatus.UNPROTECTED -> Icons.Default.GppBad
    }

    // ── Greeting ─────────────────────────────────────────────────────────────
    val greeting = remember {
        when (java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)) {
            in 5..11  -> "Good Morning"
            in 12..16 -> "Good Afternoon"
            in 17..20 -> "Good Evening"
            else      -> "Stay Protected"
        }
    }

    // ── Pulse animation ───────────────────────────────────────────────────────
    val infinitePulse = rememberInfiniteTransition(label = "shield_pulse")
    val pulseScale by infinitePulse.animateFloat(
        initialValue   = 1f,
        targetValue    = 1.08f,
        animationSpec  = infiniteRepeatable(
            animation  = tween(1400, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    val pulseAlpha by infinitePulse.animateFloat(
        initialValue   = 0.18f,
        targetValue    = 0.38f,
        animationSpec  = infiniteRepeatable(
            animation  = tween(1400, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        SidebarDeep,
                        Color(0xFF011B1A)
                    )
                )
            )
            .padding(horizontal = 20.dp, vertical = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp, bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // ── User profile row ──────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onUserNameClick)
                    .padding(bottom = 28.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Avatar circle
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(
                                color = NeonTealDim,
                                shape = CircleShape
                            )
                            .border(
                                width = 1.5.dp,
                                color = NeonTeal.copy(alpha = 0.45f),
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector     = Icons.Default.Person,
                            contentDescription = null,
                            tint            = NeonTeal,
                            modifier        = Modifier.size(22.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text  = greeting,
                            style = MaterialTheme.typography.labelMedium,
                            color = TextMuted
                        )
                        Text(
                            text       = userName,
                            style      = MaterialTheme.typography.titleMedium,
                            color      = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text  = userRole,
                            style = MaterialTheme.typography.labelSmall,
                            color = TextMuted
                        )
                    }
                }
                // Edit icon
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(
                            color  = NeonTealDim,
                            shape  = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector        = Icons.Default.Edit,
                        contentDescription = "Edit profile",
                        tint               = NeonTeal,
                        modifier           = Modifier.size(16.dp)
                    )
                }
            }

            // ── Shield icon with pulse glow ───────────────────────────────────
            Box(
                contentAlignment = Alignment.Center,
                modifier         = Modifier.size(120.dp)
            ) {
                // Outer glow ring (animated alpha)
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .background(
                            color  = statusColor.copy(alpha = pulseAlpha),
                            shape  = CircleShape
                        )
                )
                // Inner icon background
                Box(
                    modifier = Modifier
                        .scale(pulseScale)
                        .size(80.dp)
                        .background(
                            color  = statusColor.copy(alpha = 0.13f),
                            shape  = CircleShape
                        )
                        .border(
                            width  = 1.5.dp,
                            color  = statusColor.copy(alpha = 0.55f),
                            shape  = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector        = Icons.Default.Shield,
                        contentDescription = "Protection status",
                        tint               = statusColor,
                        modifier           = Modifier.size(38.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // ── Status label ──────────────────────────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                // Dot indicator
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(color = statusColor, shape = CircleShape)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text       = statusLabel,
                    style      = MaterialTheme.typography.titleLarge,
                    color      = statusColor,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text      = statusSubtitle,
                style     = MaterialTheme.typography.bodySmall,
                color     = TextMuted,
                textAlign = TextAlign.Center,
                modifier  = Modifier.padding(horizontal = 32.dp)
            )

            // ── Setup CTA (only when not fully protected) ─────────────────────
            if (protectionStatus != ProtectionStatus.PROTECTED) {
                Spacer(modifier = Modifier.height(20.dp))
                Button(
                    onClick = onSetupClick,
                    colors  = ButtonDefaults.buttonColors(
                        containerColor = statusColor,
                        contentColor   = SidebarDeep
                    ),
                    shape   = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 28.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector        = Icons.Default.Settings,
                        contentDescription = null,
                        modifier           = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text       = "Enable Full Protection",
                        fontWeight = FontWeight.Bold,
                        style      = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }

    // Bottom divider glow
    HorizontalDivider(
        color     = BorderTeal,
        thickness = 1.dp
    )
}

// ══════════════════════════════════════════════════════════════════════════════
// SECTION 2 — Threat Score Ring
// ══════════════════════════════════════════════════════════════════════════════

@Composable
fun ThreatScoreRingSection(
    totalLinks      : Int,
    safeLinks       : Int,
    threatsDetected : Int,
    threatsBlocked  : Int,
    isExpanded      : Boolean,
    onRingClick     : () -> Unit,
    onMetricClick   : (MetricDetailType) -> Unit
) {
    val hasData = totalLinks > 0

    // ── Score calculation ─────────────────────────────────────────────────────
    val scorePercent: Float = if (hasData) {
        (safeLinks.toFloat() / totalLinks.toFloat()) * 100f
    } else 0f

    // ── Ring color based on score ──────────────────────────────────────────────
    val ringColor = when {
        !hasData          -> BorderTeal
        scorePercent >= 80 -> ProtectedColor
        scorePercent >= 50 -> PartialColor
        else               -> UnprotectedColor
    }

    // ── Animated arc sweep (count-up on first load only) ─────────────────────
    var hasAnimated by remember { mutableStateOf(false) }
    val animatedSweep = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        if (!hasAnimated) {
            hasAnimated = true
            animatedSweep.animateTo(
                targetValue   = if (hasData) (scorePercent / 100f) * 300f else 0f,
                animationSpec = tween(durationMillis = 700, easing = EaseOutCubic)
            )
        }
    }

    // Re-animate if data changes after first load (e.g., new scan completes)
    // but only if already animated once (won't replay on tab switch)
    LaunchedEffect(totalLinks) {
        if (hasAnimated) {
            animatedSweep.animateTo(
                targetValue   = if (hasData) (scorePercent / 100f) * 300f else 0f,
                animationSpec = tween(durationMillis = 700, easing = EaseOutCubic)
            )
        }
    }

    // ── Expand animation for stats row ───────────────────────────────────────
    val expandTransition = updateTransition(targetState = isExpanded, label = "expand")
    val statsAlpha by expandTransition.animateFloat(
        label         = "stats_alpha",
        transitionSpec = { tween(300) }
    ) { if (it) 1f else 0f }
    val statsHeight by expandTransition.animateDp(
        label         = "stats_height",
        transitionSpec = { tween(300) }
    ) { if (it) 110.dp else 0.dp }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        // Section label
        Text(
            text      = "Threat Score",
            style     = MaterialTheme.typography.labelLarge,
            color     = TextMuted,
            letterSpacing = 1.5.sp
        )

        Spacer(modifier = Modifier.height(20.dp))

        // ── Ring + centre text ────────────────────────────────────────────────
        Box(
            contentAlignment = Alignment.Center,
            modifier         = Modifier
                .size(200.dp)
                .clickable(onClick = onRingClick)
        ) {
            // Ring drawn on canvas
            val trackColor  = BorderTeal
            val ringStroke  = 14.dp
            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokePx  = ringStroke.toPx()
                val diameter  = size.minDimension - strokePx
                val topLeft   = Offset(strokePx / 2f, strokePx / 2f)
                val arcSize   = Size(diameter, diameter)
                val startAngle = 120f   // arc starts bottom-left
                val maxSweep  = 300f    // 300° arc

                // Background track
                drawArc(
                    color      = trackColor,
                    startAngle = startAngle,
                    sweepAngle = maxSweep,
                    useCenter  = false,
                    topLeft    = topLeft,
                    size       = arcSize,
                    style      = Stroke(width = strokePx, cap = StrokeCap.Round)
                )

                // Filled arc (animated)
                if (animatedSweep.value > 0f) {
                    drawArc(
                        brush      = Brush.sweepGradient(
                            colors = listOf(
                                ringColor.copy(alpha = 0.6f),
                                ringColor,
                                ringColor
                            )
                        ),
                        startAngle = startAngle,
                        sweepAngle = animatedSweep.value,
                        useCenter  = false,
                        topLeft    = topLeft,
                        size       = arcSize,
                        style      = Stroke(width = strokePx, cap = StrokeCap.Round)
                    )
                }
            }

            // Centre content
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                if (hasData) {
                    // Animated score number
                    val displayScore = (animatedSweep.value / 300f * 100f).toInt()
                    Text(
                        text       = "$displayScore%",
                        style      = MaterialTheme.typography.displaySmall,
                        color      = ringColor,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text  = if (scorePercent >= 80) "Safe browsing" else if (scorePercent >= 50) "Moderate risk" else "High risk",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextMuted
                    )
                } else {
                    Text(
                        text       = "--",
                        style      = MaterialTheme.typography.displaySmall,
                        color      = TextMuted,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text  = "No scans yet",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextMuted
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Tap hint
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector        = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        contentDescription = null,
                        tint               = NeonTeal.copy(alpha = 0.6f),
                        modifier           = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text  = if (isExpanded) "Less" else "Details",
                        style = MaterialTheme.typography.labelSmall,
                        color = NeonTeal.copy(alpha = 0.6f)
                    )
                }
            }
        }

        // Sub-label below ring
        Spacer(modifier = Modifier.height(8.dp))
        if (hasData) {
            Text(
                text  = "$totalLinks scanned · $threatsDetected threats",
                style = MaterialTheme.typography.labelMedium,
                color = TextMuted
            )
        } else {
            Text(
                text  = "Scan your first link to see your score",
                style = MaterialTheme.typography.labelMedium,
                color = TextMuted
            )
        }

        // ── Expandable stats row ──────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(statsHeight)
        ) {
            if (statsAlpha > 0f) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 20.dp)
                        .graphicsLayer(alpha = statsAlpha),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    MiniStatPill(
                        label     = "Scanned",
                        value     = totalLinks.toString(),
                        color     = NeonTeal,
                        onClick   = { onMetricClick(MetricDetailType.LINKS_ANALYZED) }
                    )
                    MiniStatPill(
                        label     = "Threats",
                        value     = threatsDetected.toString(),
                        color     = WarningYellow,
                        onClick   = { onMetricClick(MetricDetailType.THREATS_DETECTED) }
                    )
                    MiniStatPill(
                        label     = "Blocked",
                        value     = threatsBlocked.toString(),
                        color     = DangerRed,
                        onClick   = { onMetricClick(MetricDetailType.THREATS_BLOCKED) }
                    )
                    MiniStatPill(
                        label     = "Safe",
                        value     = safeLinks.toString(),
                        color     = SafeGreenTrue,
                        onClick   = { onMetricClick(MetricDetailType.SAFE_LINKS) }
                    )
                }
            }
        }
    }

    HorizontalDivider(color = BorderTeal, thickness = 1.dp)
}

@Composable
private fun MiniStatPill(
    label   : String,
    value   : String,
    color   : Color,
    onClick : () -> Unit
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .background(color.copy(alpha = 0.10f))
            .border(
                width  = 1.dp,
                color  = color.copy(alpha = 0.30f),
                shape  = RoundedCornerShape(14.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text       = value,
            style      = MaterialTheme.typography.titleMedium,
            color      = color,
            fontWeight = FontWeight.Bold
        )
        Text(
            text  = label,
            style = MaterialTheme.typography.labelSmall,
            color = TextMuted
        )
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// SECTION 1 (original): UserProfileHeader — KEPT for backward compat
// (no longer rendered in main flow, but kept so other files that may call it compile)
// ══════════════════════════════════════════════════════════════════════════════
@Composable
fun UserProfileHeader(
    userName    : String,
    userRole    : String,
    onAvatarClick : () -> Unit,
    onEditClick : () -> Unit
) {
    val hazeState = LocalUiSurfaceState.current
    val greeting = remember {
        when (java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)) {
            in 5..11  -> "Good Morning"
            in 12..16 -> "Good Afternoon"
            in 17..20 -> "Good Evening"
            else      -> "Stay Protected"
        }
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .appContainer(hazeState = hazeState, cornerRadius = 24.dp),
        shape = RoundedCornerShape(24.dp),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    modifier  = Modifier.size(56.dp).clickable(onClick = onAvatarClick),
                    shape     = CircleShape,
                    color     = SurfaceVariant
                ) {
                    Icon(
                        imageVector        = Icons.Default.Person,
                        contentDescription = null,
                        modifier           = Modifier.padding(14.dp),
                        tint               = ElectricPurpleLight
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(text = greeting,  style = MaterialTheme.typography.labelLarge,  color = TextSecondary)
                    Text(text = userName,  style = MaterialTheme.typography.titleLarge,  color = TextPrimary, fontWeight = FontWeight.Bold)
                    Text(text = userRole,  style = MaterialTheme.typography.bodySmall,   color = TextMuted)
                }
            }
            IconButton(onClick = onEditClick) {
                Icon(imageVector = Icons.Default.Edit, contentDescription = "Edit profile", tint = ElectricPurple)
            }
        }
    }
}

// ══════════════════════════════════════════════════════════════════════════════
// Everything below this line is IDENTICAL to the original DashboardScreen.kt
// ══════════════════════════════════════════════════════════════════════════════

@Composable
private fun ProfileDetailsDialog(
    userName       : String,
    userRole       : String,
    scannedRecords : List<AllScannedUrl>,
    blockedRecords : List<BlockedUrl>,
    onSaveProfile  : (String, String) -> Unit,
    onDismiss      : () -> Unit
) {
    val context = LocalContext.current
    val hazeState = LocalUiSurfaceState.current
    val scope = rememberCoroutineScope()
    var draftName by remember(userName) { mutableStateOf(userName) }
    var draftRole by remember(userRole) { mutableStateOf(userRole) }
    var isEditing by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }
    var reportUrl by remember { mutableStateOf("") }
    var reportDescription by remember { mutableStateOf("") }
    var showUrlPicker by remember { mutableStateOf(false) }
    val selectableRecords = remember(scannedRecords) { scannedRecords.sortedBy { it.id } }

    AlertDialog(
        modifier = Modifier.appContainer(hazeState = hazeState, cornerRadius = 24.dp),
        onDismissRequest = onDismiss,
        icon = {
            Surface(modifier = Modifier.size(56.dp), shape = CircleShape, color = SurfaceVariant) {
                Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = ElectricPurple, modifier = Modifier.padding(14.dp))
            }
        },
        title = {
            Text(text = "Profile & History", color = TextPrimary, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isEditing) {
                    OutlinedTextField(value = draftName, onValueChange = { draftName = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = draftRole, onValueChange = { draftRole = it }, label = { Text("Role / Bio") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Button(
                        onClick = {
                            val finalName = draftName.trim().ifBlank { "ZeroThreat User" }
                            val finalRole = draftRole.trim().ifBlank { "Security Analyst" }
                            onSaveProfile(finalName, finalRole)
                            draftName = finalName; draftRole = finalRole; isEditing = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Save Details") }
                } else {
                    Text(text = draftName, color = TextPrimary, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(text = draftRole, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                    OutlinedButton(onClick = { isEditing = true }, modifier = Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.Edit, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Edit Details")
                    }
                }
                Surface(
                    modifier = Modifier.fillMaxWidth().appContainer(hazeState = hazeState, cornerRadius = 24.dp),
                    color    = Color.Transparent,
                    shape    = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text("History Summary", style = MaterialTheme.typography.labelLarge, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Scanned URLs: ${scannedRecords.size}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        Text("Blocked URLs: ${blockedRecords.size}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                }
                Button(
                    onClick  = {
                        scope.launch {
                            isExporting = true
                            val result  = exportDatabaseHistoryPdf(context, draftName, scannedRecords, blockedRecords)
                            isExporting = false
                            result.onSuccess { Toast.makeText(context, "History PDF saved: $it", Toast.LENGTH_LONG).show() }
                                .onFailure { Toast.makeText(context, "Failed to export PDF", Toast.LENGTH_LONG).show() }
                        }
                    },
                    enabled  = !isExporting,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isExporting) { CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp); Spacer(modifier = Modifier.width(8.dp)) }
                    else { Icon(Icons.Default.Download, contentDescription = null); Spacer(modifier = Modifier.width(8.dp)) }
                    Text("Download Full History PDF")
                }
                HorizontalDivider(color = TextMuted.copy(alpha = 0.25f))
                Text("Report a Specific Link", style = MaterialTheme.typography.titleSmall, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                OutlinedTextField(value = reportUrl, onValueChange = { reportUrl = it }, label = { Text("URL to report") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedButton(onClick = { showUrlPicker = true }, enabled = selectableRecords.isNotEmpty(), modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Link, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (selectableRecords.isNotEmpty()) "Select URL From Scanned List" else "No Scanned URLs Available")
                }
                OutlinedTextField(value = reportDescription, onValueChange = { reportDescription = it }, label = { Text("Description") }, minLines = 3, modifier = Modifier.fillMaxWidth())
                Button(
                    onClick  = { sendReportEmail(context, draftName, reportUrl, reportDescription) },
                    enabled  = reportUrl.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Email, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Send Report Email")
                }
            }
        },
        confirmButton    = { TextButton(onClick = onDismiss) { Text("Close", color = ElectricPurple) } },
        dismissButton    = {},
        containerColor   = Color.Transparent,
        titleContentColor  = TextPrimary,
        textContentColor   = TextSecondary
    )

    if (showUrlPicker) {
        AlertDialog(
            onDismissRequest = { showUrlPicker = false },
            title   = { Text("Select URL To Report", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text    = {
                if (selectableRecords.isEmpty()) {
                    Text("No scanned URLs available.", color = TextSecondary)
                } else {
                    LazyColumn(modifier = Modifier.fillMaxWidth().heightIn(max = 320.dp)) {
                        items(items = selectableRecords, key = { it.id }) { record ->
                            Column(
                                modifier = Modifier.fillMaxWidth().clickable { reportUrl = record.url; showUrlPicker = false }.padding(vertical = 10.dp)
                            ) {
                                Text("ID ${record.id}", color = TextMuted, style = MaterialTheme.typography.labelSmall)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(record.url, color = TextPrimary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            HorizontalDivider(color = TextMuted.copy(alpha = 0.2f))
                        }
                    }
                }
            },
            confirmButton  = { TextButton(onClick = { showUrlPicker = false }) { Text("Close", color = ElectricPurple) } },
            containerColor = CardBackground,
            titleContentColor = TextPrimary,
            textContentColor  = TextSecondary
        )
    }
}

// ==================== SECTION 2: Metrics Grid (original — kept for compat) ====================
@Composable
fun ThreatMetricsGrid(
    totalLinks      : Int,
    threatsDetected : Int,
    threatsBlocked  : Int,
    safeLinks       : Int,
    onMetricClick   : (MetricDetailType) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            MetricCard(title = "Analyzed",  value = totalLinks.toString(),      icon = Icons.Default.Link,        modifier = Modifier.weight(1f), onClick = { onMetricClick(MetricDetailType.LINKS_ANALYZED) })
            MetricCard(title = "Detected",  value = threatsDetected.toString(), icon = Icons.Default.Warning,     modifier = Modifier.weight(1f), accentColor = WarningYellow, onClick = { onMetricClick(MetricDetailType.THREATS_DETECTED) })
        }
        Row(modifier = Modifier.fillMaxWidth()) {
            MetricCard(title = "Blocked",   value = threatsBlocked.toString(),  icon = Icons.Default.Block,       modifier = Modifier.weight(1f), accentColor = DangerRed,     onClick = { onMetricClick(MetricDetailType.THREATS_BLOCKED) })
            MetricCard(title = "Safe",      value = safeLinks.toString(),       icon = Icons.Default.CheckCircle, modifier = Modifier.weight(1f), accentColor = SafeGreen,     onClick = { onMetricClick(MetricDetailType.SAFE_LINKS) })
        }
    }
}

@Composable
fun MetricCard(
    title       : String,
    value       : String,
    icon        : ImageVector,
    modifier    : Modifier = Modifier,
    accentColor : Color    = ElectricPurple,
    onClick     : () -> Unit
) {
    val hazeState = LocalUiSurfaceState.current
    Surface(
        onClick  = onClick,
        modifier = modifier.padding(8.dp).height(130.dp).appContainer(hazeState = hazeState, cornerRadius = 24.dp),
        color    = Color.Transparent,
        shape    = RoundedCornerShape(28.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.SpaceBetween) {
            Icon(imageVector = icon, contentDescription = title, tint = accentColor, modifier = Modifier.size(32.dp))
            Column {
                Text(text = value, style = MaterialTheme.typography.headlineMedium, color = TextPrimary, fontWeight = FontWeight.Bold)
                Text(text = title, style = MaterialTheme.typography.labelLarge,     color = TextSecondary)
            }
        }
    }
}

// ==================== SECTION 4: Threat Sources ====================
@Composable
fun ThreatSourcesSection(sources: ThreatSources) {
    val hazeState = LocalUiSurfaceState.current
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp).appContainer(hazeState = hazeState, cornerRadius = 24.dp),
        color    = Color.Transparent,
        shape    = RoundedCornerShape(32.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text("Intelligence Sources", style = MaterialTheme.typography.titleLarge, color = TextPrimary, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(24.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                SourceItem(icon = Icons.Default.Message,       label = "SMS",  count = sources.sms)
                SourceItem(icon = Icons.Default.Language,      label = "Web",  count = sources.browser)
                SourceItem(icon = Icons.Default.Notifications, label = "Apps", count = sources.notifications)
            }
        }
    }
}

@Composable
fun SourceItem(icon: ImageVector, label: String, count: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(modifier = Modifier.size(56.dp), color = SurfaceVariant, shape = RoundedCornerShape(16.dp)) {
            Icon(imageVector = icon, contentDescription = label, tint = ElectricPurpleLight, modifier = Modifier.padding(14.dp))
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(text = count.toString(), color = TextPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
        Text(text = label, style = MaterialTheme.typography.labelLarge, color = TextSecondary)
    }
}

// ==================== SECTION 5: Recent Alerts ====================
@Composable
fun RecentAlertsSection(alerts: List<ThreatAlert>) {
    val hazeState = LocalUiSurfaceState.current
    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
        Text("Live Activity Feed", style = MaterialTheme.typography.titleLarge, color = TextPrimary, fontWeight = FontWeight.Bold, modifier = Modifier.padding(start = 4.dp, bottom = 16.dp))
        alerts.take(4).forEach { alert ->
            Surface(
                modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp).appContainer(hazeState = hazeState, cornerRadius = 24.dp),
                color    = Color.Transparent,
                shape    = RoundedCornerShape(20.dp)
            ) {
                Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
                    val color = when (alert.severity) {
                        ThreatSeverity.SAFE       -> SafeGreen
                        ThreatSeverity.SUSPICIOUS -> WarningYellow
                        ThreatSeverity.PHISHING   -> DangerRed
                    }
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color))
                    Spacer(modifier = Modifier.width(20.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(text = alert.url, color = TextPrimary, maxLines = 1, style = MaterialTheme.typography.bodyLarge)
                        Text(text = "${alert.source} • ${alert.timeAgo}", color = TextSecondary, style = MaterialTheme.typography.labelLarge)
                    }
                    Icon(Icons.Default.ChevronRight, null, tint = TextMuted, modifier = Modifier.size(24.dp))
                }
            }
        }
    }
}

// ==================== SECTION 6: Privacy Statement ====================
@Composable
fun PrivacyStatement() {
    val hazeState = LocalUiSurfaceState.current
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 20.dp).appContainer(hazeState = hazeState, cornerRadius = 24.dp),
        color    = Color.Transparent,
        shape    = RoundedCornerShape(24.dp)
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Lock, null, tint = SafeGreen, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text  = "Privacy First: All threat analysis happens locally. No browsing data leaves your device.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
private fun PermissionStatusCard(
    isBrowserRoleEnabled : Boolean,
    isSmartModeEnabled   : Boolean,
    context              : Context
) {
    val hazeState = LocalUiSurfaceState.current
    Surface(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp).appContainer(hazeState = hazeState, cornerRadius = 24.dp),
        color    = Color.Transparent,
        shape    = RoundedCornerShape(28.dp)
    ) {
        Row(modifier = Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Error, null, tint = WarningYellow, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.width(20.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text("Protection Limited", color = TextPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                Text("Enable Link Protection for full security.", color = TextSecondary, style = MaterialTheme.typography.bodyMedium)
            }
            Button(
                onClick        = { if (!isBrowserRoleEnabled) requestBrowserRole(context) },
                colors         = ButtonDefaults.buttonColors(containerColor = WarningYellow),
                shape          = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text("Setup", color = PureBlack, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

enum class MetricDetailType { LINKS_ANALYZED, THREATS_DETECTED, THREATS_BLOCKED, SAFE_LINKS }

private fun filterRecordsForMetric(metric: MetricDetailType, records: List<AllScannedUrl>, blockedRecords: List<BlockedUrl>): List<AllScannedUrl> {
    return when (metric) {
        MetricDetailType.LINKS_ANALYZED   -> records
        MetricDetailType.THREATS_DETECTED -> records.filter { it.result == PhishingResult.PHISHING || it.result == PhishingResult.SUSPICIOUS }
        MetricDetailType.THREATS_BLOCKED  -> blockedRecords.map { b -> AllScannedUrl(b.id, b.url, b.domain, b.result, b.phishingScore, b.analysisNote, b.source, b.timestamp) }
        MetricDetailType.SAFE_LINKS       -> records.filter { it.result == PhishingResult.SAFE }
    }.sortedBy { it.id }
}

@Composable
private fun MetricDetailsDialog(metric: MetricDetailType, records: List<AllScannedUrl>, onDismiss: () -> Unit) {
    val hazeState = LocalUiSurfaceState.current
    AlertDialog(
        modifier       = Modifier.appContainer(hazeState = hazeState, cornerRadius = 24.dp),
        onDismissRequest = onDismiss,
        title          = { Text(text = metricTitle(metric), color = TextPrimary, fontWeight = FontWeight.Bold) },
        text           = {
            LazyColumn(modifier = Modifier.fillMaxWidth().height(400.dp)) {
                items(records) { item -> MetricDetailItem(item = item, showStatusBadge = metric == MetricDetailType.THREATS_DETECTED) }
            }
        },
        confirmButton  = { TextButton(onClick = onDismiss) { Text("Close", color = ElectricPurpleLight) } },
        containerColor = Color.Transparent
    )
}

@Composable
private fun MetricDetailItem(item: AllScannedUrl, showStatusBadge: Boolean) {
    val (statusLabel, statusColor) = threatStatusMeta(item.result)
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        if (showStatusBadge) {
            Surface(
                shape  = RoundedCornerShape(999.dp),
                color  = statusColor.copy(alpha = 0.16f),
                border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.4f))
            ) {
                Text(text = statusLabel, color = statusColor, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
        Text(text = item.domain, color = TextPrimary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
        Text(item.url, color = TextSecondary, style = MaterialTheme.typography.bodySmall, maxLines = 1)
        HorizontalDivider(modifier = Modifier.padding(top = 12.dp), color = TextMuted.copy(alpha = 0.2f))
    }
}

private fun threatStatusMeta(result: PhishingResult): Pair<String, Color> = when (result) {
    PhishingResult.SAFE       -> "SAFE"       to SafeGreenTrue
    PhishingResult.SUSPICIOUS -> "SUSPICIOUS" to WarningYellow
    PhishingResult.PHISHING   -> "PHISHING"   to DangerRed
}

private fun metricTitle(metric: MetricDetailType) = metric.name.replace("_", " ")

private fun checkBrowserRoleEnabled(context: Context): Boolean {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
    val rm = context.getSystemService(Context.ROLE_SERVICE) as? RoleManager ?: return false
    return rm.isRoleHeld(RoleManager.ROLE_BROWSER)
}

private fun requestBrowserRole(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val rm     = context.getSystemService(Context.ROLE_SERVICE) as? RoleManager
        val intent = rm?.createRequestRoleIntent(RoleManager.ROLE_BROWSER)
        intent?.let { context.startActivity(it.apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }) }
    }
}

private suspend fun exportDatabaseHistoryPdf(
    context        : Context,
    userName       : String,
    scannedRecords : List<AllScannedUrl>,
    blockedRecords : List<BlockedUrl>
): Result<String> = withContext(Dispatchers.IO) {
    runCatching {
        val fileName             = "zerothreat_history_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.pdf"
        val dateFormatter        = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val generatedAtFormatter = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        val titlePaint       = Paint().apply { color = android.graphics.Color.BLACK; textSize = 18f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
        val subtitlePaint    = Paint().apply { color = android.graphics.Color.DKGRAY; textSize = 12f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL) }
        val sectionPaint     = Paint().apply { color = android.graphics.Color.BLACK; textSize = 14f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
        val bodyPaint        = Paint().apply { color = android.graphics.Color.BLACK; textSize = 11f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL) }
        val tableHeaderPaint = Paint().apply { color = android.graphics.Color.BLACK; textSize = 10f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD) }
        val tableCellPaint   = Paint().apply { color = android.graphics.Color.BLACK; textSize = 10f; typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL) }
        val tableLinePaint   = Paint().apply { color = android.graphics.Color.DKGRAY; strokeWidth = 1f }
        val headerFillPaint  = Paint().apply { color = android.graphics.Color.parseColor("#ECEFF3"); style = Paint.Style.FILL }

        val scannedSorted = scannedRecords.sortedBy { it.id }
        val blockedSorted = blockedRecords.sortedBy { it.id }
        val historyRows   = buildList {
            scannedSorted.forEach { add(PdfHistoryRow(it.id, it.timestamp, it.url, it.result.name)) }
            blockedSorted.forEach { add(PdfHistoryRow(it.id, it.timestamp, it.url, it.result.name)) }
        }.sortedBy { it.id }

        val document = PdfDocument()
        try {
            val pageWidth = 595; val pageHeight = 842; val margin = 36f
            val contentBottom = pageHeight - margin; val lineHeight = 17f
            val tableXStart = margin; val idColWidth = 48f; val dateColWidth = 130f; val urlColWidth = 275f
            val resultColWidth = pageWidth - (margin * 2) - idColWidth - dateColWidth - urlColWidth
            val tableXId = tableXStart; val tableXDate = tableXId + idColWidth
            val tableXUrl = tableXDate + dateColWidth; val tableXResult = tableXUrl + urlColWidth
            val tableXEnd = tableXResult + resultColWidth; val rowHeight = 22f
            var pageNumber = 1
            var page   = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
            var canvas = page.canvas; var y = margin

            fun ellipsize(text: String, maxChars: Int) = if (text.length <= maxChars) text else text.take(maxChars - 3) + "..."
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
                page   = document.startPage(PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create())
                canvas = page.canvas; y = margin
                canvas.drawText("ZeroThreat URL Scan History (contd.)", margin, y, sectionPaint); y += lineHeight
                canvas.drawText("Page $pageNumber", margin, y, subtitlePaint); y += lineHeight + 6f
                canvas.drawText(sectionTitle, margin, y, sectionPaint); y += lineHeight
                drawTableHeader()
            }

            canvas.drawText("ZeroThreat URL Scan History", margin, y, titlePaint); y += lineHeight + 4f
            canvas.drawText("User: $userName", margin, y, subtitlePaint); y += lineHeight
            canvas.drawText("Generated: ${generatedAtFormatter.format(Date())}", margin, y, subtitlePaint); y += lineHeight
            canvas.drawText("Total Records: ${historyRows.size}", margin, y, subtitlePaint); y += lineHeight + 8f

            val sectionTitle = "ID | Scanned Date | URL | Result"
            canvas.drawText(sectionTitle, margin, y, sectionPaint); y += lineHeight
            if (historyRows.isEmpty()) { canvas.drawText("No history records available.", margin, y, bodyPaint); y += lineHeight }
            else {
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
                val values   = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "application/pdf")
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/ZeroThreat")
                }
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: error("Unable to create output file")
                resolver.openOutputStream(uri).use { stream -> checkNotNull(stream) { "Unable to open output stream" }; document.writeTo(stream) }
                "Downloads/ZeroThreat/$fileName"
            } else {
                val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val outputDir   = File(downloadDir, "ZeroThreat").apply { mkdirs() }
                val outputFile  = File(outputDir, fileName)
                FileOutputStream(outputFile).use { stream -> document.writeTo(stream) }
                outputFile.absolutePath
            }
        } finally { document.close() }
    }
}

private data class PdfHistoryRow(val id: Int, val timestamp: Long, val url: String, val result: String)

private fun sendReportEmail(context: Context, reporterName: String, reportUrl: String, reportDescription: String) {
    val safeReporter    = reporterName.trim().ifBlank { "ZeroThreat User" }
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
    try { context.startActivity(emailIntent) }
    catch (_: ActivityNotFoundException) { Toast.makeText(context, "No email app found on this device.", Toast.LENGTH_LONG).show() }
}