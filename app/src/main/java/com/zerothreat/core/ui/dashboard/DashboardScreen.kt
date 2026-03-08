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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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

private const val REPORT_EMAIL = "sharoshalonmathew2026@it.ajce.in"

@Composable
fun DashboardScreen(
    viewModel: DashboardViewModel = viewModel()
) {
    val hazeState = LocalUiSurfaceState.current
    val uiState by viewModel.uiState.collectAsState()
    val urlViewModel: UrlViewModel = viewModel()
    val storedLinks by urlViewModel.recentScans.collectAsState()
    val blockedLinks by urlViewModel.blockedUrls.collectAsState()
    var selectedMetric by remember { mutableStateOf<MetricDetailType?>(null) }
    var showProfileDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val appPreferences = remember { AppPreferences(context) }
    var profileName by remember { mutableStateOf(appPreferences.profileName) }
    var profileRole by remember { mutableStateOf(appPreferences.profileRole) }
    var isBrowserRoleEnabled by remember { mutableStateOf(checkBrowserRoleEnabled(context)) }
    var isSmartModeEnabled by remember { mutableStateOf(appPreferences.smartModeEnabled) }

    LaunchedEffect(Unit) {
        isBrowserRoleEnabled = checkBrowserRoleEnabled(context)
        isSmartModeEnabled = appPreferences.smartModeEnabled
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .appBackground(hazeState),
        contentPadding = PaddingValues(bottom = 120.dp, top = 16.dp)
    ) {
        item {
            UserProfileHeader(
                userName = profileName,
                userRole = profileRole,
                onAvatarClick = { showProfileDialog = true },
                onEditClick = { showProfileDialog = true }
            )
        }

        // SECTION 1.5: Permission Status Card (Floating Glassy)
        item {
            if (!isBrowserRoleEnabled || !isSmartModeEnabled) {
                PermissionStatusCard(
                    isBrowserRoleEnabled = isBrowserRoleEnabled,
                    isSmartModeEnabled = isSmartModeEnabled,
                    context = context
                )
            }
        }

        // SECTION 2: Threat Intelligence Summary (Metrics Grid)
        item {
            ThreatMetricsGrid(
                totalLinks = uiState.totalLinksAnalyzed,
                threatsDetected = uiState.threatsDetected,
                threatsBlocked = uiState.threatsBlocked,
                safeLinks = uiState.safeLinks,
                onMetricClick = { metric -> selectedMetric = metric }
            )
        }

        // SECTION 3: Removed Threat Trend Chart as requested

        // SECTION 4: Threat Sources Overview (Floating Glassy)
        item {
            if (uiState.totalLinksAnalyzed > 0) {
                ThreatSourcesSection(sources = uiState.threatSources)
            }
        }

        // SECTION 5: Recent Alerts Feed (Floating Glassy)
        item {
            if (uiState.recentAlerts.isNotEmpty()) {
                RecentAlertsSection(alerts = uiState.recentAlerts)
            }
        }

        // SECTION 6: Privacy Statement (Floating Glassy)
        item {
            PrivacyStatement()
        }
    }

    selectedMetric?.let { metric ->
        val records = filterRecordsForMetric(metric, storedLinks, blockedLinks)
        MetricDetailsDialog(
            metric = metric,
            records = records,
            onDismiss = { selectedMetric = null }
        )
    }

    if (showProfileDialog) {
        ProfileDetailsDialog(
            userName = profileName,
            userRole = profileRole,
            scannedRecords = storedLinks,
            blockedRecords = blockedLinks,
            onSaveProfile = { newName, newRole ->
                profileName = newName
                profileRole = newRole
                appPreferences.profileName = newName
                appPreferences.profileRole = newRole
            },
            onDismiss = { showProfileDialog = false }
        )
    }
}

// ==================== SECTION 1: User Profile Header ====================
@Composable
fun UserProfileHeader(
    userName: String,
    userRole: String,
    onAvatarClick: () -> Unit,
    onEditClick: () -> Unit
) {
    val hazeState = LocalUiSurfaceState.current
    val greeting = remember {
        when (java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)) {
            in 5..11 -> "Good Morning"
            in 12..16 -> "Good Afternoon"
            in 17..20 -> "Good Evening"
            else -> "Stay Protected"
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
                    modifier = Modifier
                        .size(56.dp)
                        .clickable(onClick = onAvatarClick),
                    shape = CircleShape,
                    color = SurfaceVariant
                ) {
                    Icon(
                        imageVector = Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.padding(14.dp),
                        tint = ElectricPurpleLight
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column {
                    Text(
                        text = greeting,
                        style = MaterialTheme.typography.labelLarge,
                        color = TextSecondary
                    )
                    Text(
                        text = userName,
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = userRole,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextMuted
                    )
                }
            }

            IconButton(onClick = onEditClick) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit profile",
                    tint = ElectricPurple
                )
            }
        }
    }
}

@Composable
private fun ProfileDetailsDialog(
    userName: String,
    userRole: String,
    scannedRecords: List<AllScannedUrl>,
    blockedRecords: List<BlockedUrl>,
    onSaveProfile: (String, String) -> Unit,
    onDismiss: () -> Unit
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
    val selectableRecords = remember(scannedRecords) {
        scannedRecords
            .sortedBy { it.id }
    }

    AlertDialog(
        modifier = Modifier.appContainer(hazeState = hazeState, cornerRadius = 24.dp),
        onDismissRequest = onDismiss,
        icon = {
            Surface(
                modifier = Modifier.size(56.dp),
                shape = CircleShape,
                color = SurfaceVariant
            ) {
                Icon(
                    imageVector = Icons.Default.Person,
                    contentDescription = null,
                    tint = ElectricPurple,
                    modifier = Modifier.padding(14.dp)
                )
            }
        },
        title = {
            Text(
                text = "Profile & History",
                color = TextPrimary,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (isEditing) {
                    OutlinedTextField(
                        value = draftName,
                        onValueChange = { draftName = it },
                        label = { Text("Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = draftRole,
                        onValueChange = { draftRole = it },
                        label = { Text("Role / Bio") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            val finalName = draftName.trim().ifBlank { "ZeroThreat User" }
                            val finalRole = draftRole.trim().ifBlank { "Security Analyst" }
                            onSaveProfile(finalName, finalRole)
                            draftName = finalName
                            draftRole = finalRole
                            isEditing = false
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save Details")
                    }
                } else {
                    Text(
                        text = draftName,
                        color = TextPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = draftRole,
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall
                    )
                    OutlinedButton(
                        onClick = { isEditing = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Edit Details")
                    }
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .appContainer(hazeState = hazeState, cornerRadius = 24.dp),
                    color = Color.Transparent,
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "History Summary",
                            style = MaterialTheme.typography.labelLarge,
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Scanned URLs: ${scannedRecords.size}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        Text(
                            text = "Blocked URLs: ${blockedRecords.size}",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }

                Button(
                    onClick = {
                        scope.launch {
                            isExporting = true
                            val result = exportDatabaseHistoryPdf(
                                context = context,
                                userName = draftName,
                                scannedRecords = scannedRecords,
                                blockedRecords = blockedRecords
                            )
                            isExporting = false
                            result.onSuccess { location ->
                                Toast.makeText(
                                    context,
                                    "History PDF saved: $location",
                                    Toast.LENGTH_LONG
                                ).show()
                            }.onFailure {
                                Toast.makeText(
                                    context,
                                    "Failed to export PDF",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    },
                    enabled = !isExporting,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isExporting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    } else {
                        Icon(Icons.Default.Download, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text("Download Full History PDF")
                }

                HorizontalDivider(color = TextMuted.copy(alpha = 0.25f))

                Text(
                    text = "Report a Specific Link",
                    style = MaterialTheme.typography.titleSmall,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                OutlinedTextField(
                    value = reportUrl,
                    onValueChange = { reportUrl = it },
                    label = { Text("URL to report") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedButton(
                    onClick = { showUrlPicker = true },
                    enabled = selectableRecords.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Link, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (selectableRecords.isNotEmpty()) {
                            "Select URL From Scanned List"
                        } else {
                            "No Scanned URLs Available"
                        }
                    )
                }
                OutlinedTextField(
                    value = reportDescription,
                    onValueChange = { reportDescription = it },
                    label = { Text("Description") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
                Button(
                    onClick = {
                        sendReportEmail(
                            context = context,
                            reporterName = draftName,
                            reportUrl = reportUrl,
                            reportDescription = reportDescription
                        )
                    },
                    enabled = reportUrl.isNotBlank(),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Email, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Send Report Email")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = ElectricPurple)
            }
        },
        dismissButton = {},
        containerColor = Color.Transparent,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary
    )

    if (showUrlPicker) {
        AlertDialog(
            onDismissRequest = { showUrlPicker = false },
            title = {
                Text(
                    text = "Select URL To Report",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                if (selectableRecords.isEmpty()) {
                    Text("No scanned URLs available.", color = TextSecondary)
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 320.dp)
                    ) {
                        items(
                            items = selectableRecords,
                            key = { it.id }
                        ) { record ->
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        reportUrl = record.url
                                        showUrlPicker = false
                                    }
                                    .padding(vertical = 10.dp)
                            ) {
                                Text(
                                    text = "ID ${record.id}",
                                    color = TextMuted,
                                    style = MaterialTheme.typography.labelSmall
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = record.url,
                                    color = TextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            HorizontalDivider(color = TextMuted.copy(alpha = 0.2f))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showUrlPicker = false }) {
                    Text("Close", color = ElectricPurple)
                }
            },
            containerColor = CardBackground,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary
        )
    }
}

// ==================== SECTION 2: Metrics Grid ====================
@Composable
fun ThreatMetricsGrid(
    totalLinks: Int,
    threatsDetected: Int,
    threatsBlocked: Int,
    safeLinks: Int,
    onMetricClick: (MetricDetailType) -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            MetricCard(
                title = "Analyzed",
                value = totalLinks.toString(),
                icon = Icons.Default.Link,
                modifier = Modifier.weight(1f),
                onClick = { onMetricClick(MetricDetailType.LINKS_ANALYZED) }
            )
            MetricCard(
                title = "Detected",
                value = threatsDetected.toString(),
                icon = Icons.Default.Warning,
                modifier = Modifier.weight(1f),
                accentColor = WarningYellow,
                onClick = { onMetricClick(MetricDetailType.THREATS_DETECTED) }
            )
        }

        Row(modifier = Modifier.fillMaxWidth()) {
            MetricCard(
                title = "Blocked",
                value = threatsBlocked.toString(),
                icon = Icons.Default.Block,
                modifier = Modifier.weight(1f),
                accentColor = DangerRed,
                onClick = { onMetricClick(MetricDetailType.THREATS_BLOCKED) }
            )
            MetricCard(
                title = "Safe",
                value = safeLinks.toString(),
                icon = Icons.Default.CheckCircle,
                modifier = Modifier.weight(1f),
                accentColor = SafeGreen,
                onClick = { onMetricClick(MetricDetailType.SAFE_LINKS) }
            )
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    accentColor: Color = ElectricPurple,
    onClick: () -> Unit
) {
    val hazeState = LocalUiSurfaceState.current
    Surface(
        onClick = onClick,
        modifier = modifier
            .padding(8.dp)
            .height(130.dp)
            .appContainer(hazeState = hazeState, cornerRadius = 24.dp),
        color = Color.Transparent,
        shape = RoundedCornerShape(28.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = accentColor,
                modifier = Modifier.size(32.dp)
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
                    style = MaterialTheme.typography.labelLarge,
                    color = TextSecondary
                )
            }
        }
    }
}

// ==================== SECTION 4: Threat Sources ====================
@Composable
fun ThreatSourcesSection(sources: ThreatSources) {
    val hazeState = LocalUiSurfaceState.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .appContainer(hazeState = hazeState, cornerRadius = 24.dp),
        color = Color.Transparent,
        shape = RoundedCornerShape(32.dp)
    ) {
        Column(modifier = Modifier.padding(24.dp)) {
            Text(
                text = "Intelligence Sources",
                style = MaterialTheme.typography.titleLarge,
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                SourceItem(icon = Icons.Default.Message, label = "SMS", count = sources.sms)
                SourceItem(icon = Icons.Default.Language, label = "Web", count = sources.browser)
                SourceItem(icon = Icons.Default.Notifications, label = "Apps", count = sources.notifications)
            }
        }
    }
}

@Composable
fun SourceItem(icon: ImageVector, label: String, count: Int) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            modifier = Modifier.size(56.dp),
            color = SurfaceVariant,
            shape = RoundedCornerShape(16.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = ElectricPurpleLight,
                modifier = Modifier.padding(14.dp)
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = count.toString(),
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleLarge
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = TextSecondary
        )
    }
}

// ==================== SECTION 5: Recent Alerts ====================
@Composable
fun RecentAlertsSection(alerts: List<ThreatAlert>) {
    val hazeState = LocalUiSurfaceState.current
    Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
        Text(
            text = "Live Activity Feed",
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 4.dp, bottom = 16.dp)
        )

        alerts.take(4).forEach { alert ->
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp)
                    .appContainer(hazeState = hazeState, cornerRadius = 24.dp),
                color = Color.Transparent,
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier.padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val color = when (alert.severity) {
                        ThreatSeverity.SAFE -> SafeGreen
                        ThreatSeverity.SUSPICIOUS -> WarningYellow
                        ThreatSeverity.PHISHING -> DangerRed
                    }
                    Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color))
                    Spacer(modifier = Modifier.width(20.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = alert.url,
                            color = TextPrimary,
                            maxLines = 1,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Text(
                            text = "${alert.source} • ${alert.timeAgo}",
                            color = TextSecondary,
                            style = MaterialTheme.typography.labelLarge
                        )
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
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 20.dp)
            .appContainer(hazeState = hazeState, cornerRadius = 24.dp),
        color = Color.Transparent,
        shape = RoundedCornerShape(24.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Lock, null, tint = SafeGreen, modifier = Modifier.size(24.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = "Privacy First: All threat analysis happens locally. No browsing data leaves your device.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                lineHeight = 20.sp
            )
        }
    }
}

@Composable
private fun PermissionStatusCard(
    isBrowserRoleEnabled: Boolean,
    isSmartModeEnabled: Boolean,
    context: Context
) {
    val hazeState = LocalUiSurfaceState.current
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .appContainer(hazeState = hazeState, cornerRadius = 24.dp),
        color = Color.Transparent,
        shape = RoundedCornerShape(28.dp)
    ) {
        Row(
            modifier = Modifier.padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Error, null, tint = WarningYellow, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.width(20.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Protection Limited",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.titleSmall
                )
                Text(
                    text = "Enable Link Protection for full security.",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Button(
                onClick = { if (!isBrowserRoleEnabled) requestBrowserRole(context) },
                colors = ButtonDefaults.buttonColors(containerColor = WarningYellow),
                shape = RoundedCornerShape(14.dp),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Setup",
                    color = PureBlack,
                    fontWeight = FontWeight.Bold,
                    style = MaterialTheme.typography.labelLarge
                )
            }
        }
    }
}

enum class MetricDetailType { LINKS_ANALYZED, THREATS_DETECTED, THREATS_BLOCKED, SAFE_LINKS }
private fun filterRecordsForMetric(metric: MetricDetailType, records: List<AllScannedUrl>, blockedRecords: List<BlockedUrl>): List<AllScannedUrl> {
    return when (metric) {
        MetricDetailType.LINKS_ANALYZED -> records
        MetricDetailType.THREATS_DETECTED -> records.filter {
            it.result == PhishingResult.PHISHING || it.result == PhishingResult.SUSPICIOUS
        }
        MetricDetailType.THREATS_BLOCKED -> blockedRecords.map { b -> AllScannedUrl(b.id, b.url, b.domain, b.result, b.phishingScore, b.analysisNote, b.source, b.timestamp) }
        MetricDetailType.SAFE_LINKS -> records.filter { it.result == PhishingResult.SAFE }
    }.sortedBy { it.id }
}
@Composable
private fun MetricDetailsDialog(metric: MetricDetailType, records: List<AllScannedUrl>, onDismiss: () -> Unit) {
    val hazeState = LocalUiSurfaceState.current
    AlertDialog(
        modifier = Modifier.appContainer(hazeState = hazeState, cornerRadius = 24.dp),
        onDismissRequest = onDismiss,
        title = { Text(text = metricTitle(metric), color = TextPrimary, fontWeight = FontWeight.Bold) },
        text = {
            LazyColumn(modifier = Modifier.fillMaxWidth().height(400.dp)) {
                items(records) { item ->
                    MetricDetailItem(
                        item = item,
                        showStatusBadge = metric == MetricDetailType.THREATS_DETECTED
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close", color = ElectricPurpleLight) } },
        containerColor = Color.Transparent
    )
}
@Composable
private fun MetricDetailItem(
    item: AllScannedUrl,
    showStatusBadge: Boolean
) {
    val (statusLabel, statusColor) = threatStatusMeta(item.result)
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        if (showStatusBadge) {
            Surface(
                shape = RoundedCornerShape(999.dp),
                color = statusColor.copy(alpha = 0.16f),
                border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.4f))
            ) {
                Text(
                    text = statusLabel,
                    color = statusColor,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        Text(
            text = item.domain,
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            style = MaterialTheme.typography.titleMedium
        )
        Text(item.url, color = TextSecondary, style = MaterialTheme.typography.bodySmall, maxLines = 1)
        HorizontalDivider(modifier = Modifier.padding(top = 12.dp), color = TextMuted.copy(alpha = 0.2f))
    }
}

private fun threatStatusMeta(result: PhishingResult): Pair<String, Color> {
    return when (result) {
        PhishingResult.SAFE -> "SAFE" to SafeGreen
        PhishingResult.SUSPICIOUS -> "SUSPICIOUS" to WarningYellow
        PhishingResult.PHISHING -> "PHISHING" to DangerRed
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
    context: Context,
    userName: String,
    scannedRecords: List<AllScannedUrl>,
    blockedRecords: List<BlockedUrl>
): Result<String> = withContext(Dispatchers.IO) {
    runCatching {
        val fileName = "zerothreat_history_${SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())}.pdf"
        val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
        val generatedAtFormatter = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        val titlePaint = Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 18f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val subtitlePaint = Paint().apply {
            color = android.graphics.Color.DKGRAY
            textSize = 12f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        val sectionPaint = Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val bodyPaint = Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 11f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        val tableHeaderPaint = Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }
        val tableCellPaint = Paint().apply {
            color = android.graphics.Color.BLACK
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }
        val tableLinePaint = Paint().apply {
            color = android.graphics.Color.DKGRAY
            strokeWidth = 1f
        }
        val headerFillPaint = Paint().apply {
            color = android.graphics.Color.parseColor("#ECEFF3")
            style = Paint.Style.FILL
        }

        val scannedSorted = scannedRecords.sortedBy { it.id }
        val blockedSorted = blockedRecords.sortedBy { it.id }
        val historyRows = buildList {
            scannedSorted.forEach { record ->
                add(
                    PdfHistoryRow(
                        id = record.id,
                        timestamp = record.timestamp,
                        url = record.url,
                        result = record.result.name
                    )
                )
            }
            blockedSorted.forEach { record ->
                add(
                    PdfHistoryRow(
                        id = record.id,
                        timestamp = record.timestamp,
                        url = record.url,
                        result = record.result.name
                    )
                )
            }
        }.sortedBy { it.id }

        val document = PdfDocument()
        try {
            val pageWidth = 595
            val pageHeight = 842
            val margin = 36f
            val contentBottom = pageHeight - margin
            val lineHeight = 17f

            val tableXStart = margin
            val idColWidth = 48f
            val dateColWidth = 130f
            val urlColWidth = 275f
            val resultColWidth = pageWidth - (margin * 2) - idColWidth - dateColWidth - urlColWidth
            val tableXId = tableXStart
            val tableXDate = tableXId + idColWidth
            val tableXUrl = tableXDate + dateColWidth
            val tableXResult = tableXUrl + urlColWidth
            val tableXEnd = tableXResult + resultColWidth
            val rowHeight = 22f

            var pageNumber = 1
            var page = document.startPage(
                PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
            )
            var canvas = page.canvas
            var y = margin

            fun ellipsize(text: String, maxChars: Int): String {
                if (text.length <= maxChars) return text
                return text.take(maxChars - 3) + "..."
            }

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
                document.finishPage(page)
                pageNumber += 1
                page = document.startPage(
                    PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNumber).create()
                )
                canvas = page.canvas
                y = margin
                canvas.drawText("ZeroThreat URL Scan History (contd.)", margin, y, sectionPaint)
                y += lineHeight
                canvas.drawText("Page $pageNumber", margin, y, subtitlePaint)
                y += lineHeight + 6f
                canvas.drawText(sectionTitle, margin, y, sectionPaint)
                y += lineHeight
                drawTableHeader()
            }

            canvas.drawText("ZeroThreat URL Scan History", margin, y, titlePaint)
            y += lineHeight + 4f
            canvas.drawText("User: $userName", margin, y, subtitlePaint)
            y += lineHeight
            canvas.drawText("Generated: ${generatedAtFormatter.format(Date())}", margin, y, subtitlePaint)
            y += lineHeight
            canvas.drawText("Total Records: ${historyRows.size}", margin, y, subtitlePaint)
            y += lineHeight + 8f

            val sectionTitle = "ID | Scanned Date | URL | Result"
            canvas.drawText(sectionTitle, margin, y, sectionPaint)
            y += lineHeight

            if (historyRows.isEmpty()) {
                canvas.drawText("No history records available.", margin, y, bodyPaint)
                y += lineHeight
            } else {
                drawTableHeader()
                historyRows.forEach { row ->
                    if (y + rowHeight > contentBottom) {
                        startNewPage(sectionTitle)
                    }

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
                    put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/ZeroThreat")
                }
                val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
                    ?: error("Unable to create output file")
                resolver.openOutputStream(uri).use { stream ->
                    checkNotNull(stream) { "Unable to open output stream" }
                    document.writeTo(stream)
                }
                "Downloads/ZeroThreat/$fileName"
            } else {
                val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val outputDir = File(downloadDir, "ZeroThreat").apply { mkdirs() }
                val outputFile = File(outputDir, fileName)
                FileOutputStream(outputFile).use { stream -> document.writeTo(stream) }
                outputFile.absolutePath
            }
        } finally {
            document.close()
        }
    }
}

private data class PdfHistoryRow(
    val id: Int,
    val timestamp: Long,
    val url: String,
    val result: String
)

private fun sendReportEmail(
    context: Context,
    reporterName: String,
    reportUrl: String,
    reportDescription: String
) {
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
