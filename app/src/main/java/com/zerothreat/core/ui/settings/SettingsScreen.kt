package com.zerothreat.core.ui.settings

import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zerothreat.core.browser.rememberExternalDefaultBrowser
import com.zerothreat.core.data.AppPreferences
import com.zerothreat.core.ui.theme.CardBackground
import com.zerothreat.core.ui.theme.DangerRed
import com.zerothreat.core.ui.theme.DarkBackground
import com.zerothreat.core.ui.theme.ElectricPurple
import com.zerothreat.core.ui.theme.LocalUiSurfaceState
import com.zerothreat.core.ui.theme.PeachAccent
import com.zerothreat.core.ui.theme.PureBlack
import com.zerothreat.core.ui.theme.PureWhite
import com.zerothreat.core.ui.theme.SafeGreen
import com.zerothreat.core.ui.theme.SurfaceVariant
import com.zerothreat.core.ui.theme.TextMuted
import com.zerothreat.core.ui.theme.TextPrimary
import com.zerothreat.core.ui.theme.TextSecondary
import com.zerothreat.core.ui.theme.TextWhite
import com.zerothreat.core.ui.theme.appContainer
import com.zerothreat.core.ui.theme.appBackground
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Color

private data class BrowserOption(
    val packageName: String,
    val label: String
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    appPreferences: AppPreferences,
    onNavigateBack: () -> Unit
) {
    val hazeState = LocalUiSurfaceState.current
    val context = LocalContext.current
    val uiScope = rememberCoroutineScope()
    var clickProtectionEnabled by remember { mutableStateOf(appPreferences.smartModeEnabled) }
    var linkHandlerPromptEnabled by remember { mutableStateOf(appPreferences.linkHandlerPromptEnabled) }
    var notificationMonitoring by remember { mutableStateOf(appPreferences.notificationMonitoring) }
    var monitorWhatsapp by remember { mutableStateOf(appPreferences.monitorWhatsapp) }
    var monitorInstagram by remember { mutableStateOf(appPreferences.monitorInstagram) }
    var monitorGmail by remember { mutableStateOf(appPreferences.monitorGmail) }
    var monitorTelegram by remember { mutableStateOf(appPreferences.monitorTelegram) }
    var monitorMessages by remember { mutableStateOf(appPreferences.monitorMessages) }
    val browserOptions = remember(context) { getAvailableBrowsers(context) }
    var showBrowserPicker by remember { mutableStateOf(false) }
    var preferredBrowserPackage by remember {
        mutableStateOf(appPreferences.preferredBrowserPackage.orEmpty())
    }
    val preferredBrowserLabel = browserOptions.firstOrNull {
        it.packageName == preferredBrowserPackage
    }?.label ?: "System default"

    var showClearConfirmDialog by remember { mutableStateOf(false) }

    // App disable dialog state
    var showAppDisableDialog by remember { mutableStateOf(false) }
    var appToDisable by remember { mutableStateOf("") }

    // Check browser role status
    var isBrowserRoleGranted by remember { mutableStateOf(checkBrowserRoleGranted(context)) }

    // Refresh permission status when screen is visible
    androidx.compose.runtime.DisposableEffect(Unit) {
        isBrowserRoleGranted = checkBrowserRoleGranted(context)
        onDispose { }
    }

    val popupShape = RoundedCornerShape(22.dp)

    Scaffold(
        modifier = Modifier.appBackground(hazeState),
        topBar = {
            TopAppBar(
                modifier = Modifier.appContainer(
                    hazeState = hazeState,
                    cornerRadius = 24.dp,
                    thin = true
                ),
                title = {
                    Text(
                        "Settings",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        containerColor = DarkBackground
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp)
        ) {
            // Permission Warning Banner
            if (!isBrowserRoleGranted) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .appContainer(hazeState = hazeState, cornerRadius = 24.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = Color.Transparent
                        ),
                        shape = RoundedCornerShape(12.dp),
                        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Warning",
                                    tint = DangerRed,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Permission Required",
                                        color = DangerRed,
                                        fontWeight = FontWeight.Bold,
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        text = "Link protection is not active. Set ZeroThreat as your default browser.",
                                        color = TextSecondary,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    requestBrowserRoleOrSettings(context, appPreferences)
                                    // Refresh status after a delay
                                    uiScope.launch {
                                        delay(1000)
                                        isBrowserRoleGranted = checkBrowserRoleGranted(context)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = SafeGreen,
                                    contentColor = TextWhite
                                )
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Shield,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Enable Link Protection Now")
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            item {
                SectionHeader("Click Link Protection")
                Spacer(modifier = Modifier.height(12.dp))
            }

            item {
                SettingsToggleCard(
                    icon = Icons.Default.AutoAwesome,
                    title = "Scan Clicked Links",
                    description = "Show risk popup before opening links",
                    checked = clickProtectionEnabled,
                    onCheckedChange = {
                        clickProtectionEnabled = it
                        appPreferences.smartModeEnabled = it
                        if (it) {
                            requestBrowserRoleOrSettings(context, appPreferences)
                        }
                    }
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            item {
                SettingsToggleCard(
                    icon = Icons.Default.AutoAwesome,
                    title = "Ask On First Open",
                    description = "Show setup prompt to enable link handling",
                    checked = linkHandlerPromptEnabled,
                    onCheckedChange = {
                        linkHandlerPromptEnabled = it
                        appPreferences.linkHandlerPromptEnabled = it
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            item {
                SettingsActionCard(
                    icon = Icons.Default.AutoAwesome,
                    title = "Set As Link Scanner",
                    description = "Allow ZeroThreat to receive clicked links before browser opens",
                    actionText = "Set",
                    onAction = { requestBrowserRoleOrSettings(context, appPreferences) }
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            item {
                SettingsActionCard(
                    icon = Icons.Default.AutoAwesome,
                    title = "Preferred Browser",
                    description = "Current: $preferredBrowserLabel",
                    actionText = "Choose",
                    onAction = { showBrowserPicker = true }
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            item {
                SectionHeader("Notification Listener")
                Spacer(modifier = Modifier.height(12.dp))
            }

            item {
                SettingsToggleCard(
                    icon = Icons.Default.Notifications,
                    title = "Notification Monitoring",
                    description = "Scan links that appear in app notifications",
                    checked = notificationMonitoring,
                    onCheckedChange = {
                        if (it) {
                            openNotificationAccessSettings(context)
                        }
                        notificationMonitoring = it
                        appPreferences.notificationMonitoring = it
                    }
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            item {
                SectionHeader("Supported Apps")
                Spacer(modifier = Modifier.height(12.dp))
            }

            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .appContainer(hazeState = hazeState, cornerRadius = 24.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SettingsToggleItem(
                            icon = Icons.AutoMirrored.Filled.Chat,
                            title = "WhatsApp",
                            description = "Enable WhatsApp checks",
                            checked = monitorWhatsapp,
                            onCheckedChange = {
                                if (!it) {
                                    // Turning OFF - show dialog
                                    appToDisable = "WhatsApp"
                                    showAppDisableDialog = true
                                } else {
                                    // Turning ON - enable both
                                    monitorWhatsapp = true
                                    appPreferences.monitorWhatsapp = true
                                    appPreferences.whatsappNotificationEnabled = true
                                    appPreferences.whatsappOnClickEnabled = true
                                }
                            }
                        )
                        HorizontalDivider(color = TextMuted.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 12.dp))
                        SettingsToggleItem(
                            icon = Icons.Default.PhotoCamera,
                            title = "Instagram",
                            description = "Enable Instagram checks",
                            checked = monitorInstagram,
                            onCheckedChange = {
                                if (!it) {
                                    // Turning OFF - show dialog
                                    appToDisable = "Instagram"
                                    showAppDisableDialog = true
                                } else {
                                    // Turning ON - enable both
                                    monitorInstagram = true
                                    appPreferences.monitorInstagram = true
                                    appPreferences.instagramNotificationEnabled = true
                                    appPreferences.instagramOnClickEnabled = true
                                }
                            }
                        )
                        HorizontalDivider(color = TextMuted.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 12.dp))
                        SettingsToggleItem(
                            icon = Icons.Default.Email,
                            title = "Gmail",
                            description = "Enable Gmail checks",
                            checked = monitorGmail,
                            onCheckedChange = {
                                if (!it) {
                                    // Turning OFF - show dialog
                                    appToDisable = "Gmail"
                                    showAppDisableDialog = true
                                } else {
                                    // Turning ON - enable both
                                    monitorGmail = true
                                    appPreferences.monitorGmail = true
                                    appPreferences.gmailNotificationEnabled = true
                                    appPreferences.gmailOnClickEnabled = true
                                }
                            }
                        )
                        HorizontalDivider(color = TextMuted.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 12.dp))
                        SettingsToggleItem(
                            icon = Icons.AutoMirrored.Filled.Send,
                            title = "Telegram",
                            description = "Enable Telegram checks",
                            checked = monitorTelegram,
                            onCheckedChange = {
                                if (!it) {
                                    // Turning OFF - show dialog
                                    appToDisable = "Telegram"
                                    showAppDisableDialog = true
                                } else {
                                    // Turning ON - enable both
                                    monitorTelegram = true
                                    appPreferences.monitorTelegram = true
                                    appPreferences.telegramNotificationEnabled = true
                                    appPreferences.telegramOnClickEnabled = true
                                }
                            }
                        )
                        HorizontalDivider(color = TextMuted.copy(alpha = 0.2f), modifier = Modifier.padding(vertical = 12.dp))
                        SettingsToggleItem(
                            icon = Icons.AutoMirrored.Filled.Message,
                            title = "Messages / SMS",
                            description = "Enable Messages checks",
                            checked = monitorMessages,
                            onCheckedChange = {
                                if (!it) {
                                    // Turning OFF - show dialog
                                    appToDisable = "Messages"
                                    showAppDisableDialog = true
                                } else {
                                    // Turning ON - enable both
                                    monitorMessages = true
                                    appPreferences.monitorMessages = true
                                    appPreferences.messagesNotificationEnabled = true
                                    appPreferences.messagesOnClickEnabled = true
                                }
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(24.dp))
            }

            item {
                SectionHeader("Privacy & Data")
                Spacer(modifier = Modifier.height(12.dp))
            }

            item {
                SettingsActionCard(
                    icon = Icons.Default.Delete,
                    title = "Clear Threat History",
                    description = "Delete all stored scan logs and reset database",
                    actionText = "Clear",
                    onAction = { showClearConfirmDialog = true },
                    isDestructive = true
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
        }
    }

    // Browser Picker Dialog
    if (showBrowserPicker) {
        BrowserPickerDialog(
            browsers = browserOptions,
            selectedPackage = preferredBrowserPackage,
            onSelectSystemDefault = {
                preferredBrowserPackage = ""
                appPreferences.preferredBrowserPackage = null
                showBrowserPicker = false
            },
            onSelectBrowser = { packageName ->
                preferredBrowserPackage = packageName
                appPreferences.preferredBrowserPackage = packageName
                showBrowserPicker = false
            },
            onDismiss = { showBrowserPicker = false },
            popupShape = popupShape
        )
    }

    // App Disable Dialog
    if (showAppDisableDialog) {
        AlertDialog(
            modifier = Modifier.appContainer(hazeState = hazeState, cornerRadius = 24.dp),
            onDismissRequest = {
                showAppDisableDialog = false
                appToDisable = ""
            },
            title = { Text("Disable $appToDisable Protection", color = TextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "Choose which protection feature to disable:",
                        color = TextPrimary,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "• Notification: Disable scanning URLs in notifications from $appToDisable",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "• onClick: Disable scanning when you click URLs from $appToDisable",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Text(
                        "• Both: Completely disable $appToDisable protection",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            },
            confirmButton = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(
                        onClick = {
                            // Disable Notification only
                            when (appToDisable) {
                                "WhatsApp" -> appPreferences.whatsappNotificationEnabled = false
                                "Instagram" -> appPreferences.instagramNotificationEnabled = false
                                "Gmail" -> appPreferences.gmailNotificationEnabled = false
                                "Telegram" -> appPreferences.telegramNotificationEnabled = false
                                "Messages" -> appPreferences.messagesNotificationEnabled = false
                            }
                            showAppDisableDialog = false
                            Toast.makeText(context, "$appToDisable notification scanning disabled", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.textButtonColors(
                            containerColor = PureWhite,
                            contentColor = PureBlack
                        )
                    ) {
                        Text("Disable Notification")
                    }
                    TextButton(
                        onClick = {
                            // Disable onClick only
                            when (appToDisable) {
                                "WhatsApp" -> appPreferences.whatsappOnClickEnabled = false
                                "Instagram" -> appPreferences.instagramOnClickEnabled = false
                                "Gmail" -> appPreferences.gmailOnClickEnabled = false
                                "Telegram" -> appPreferences.telegramOnClickEnabled = false
                                "Messages" -> appPreferences.messagesOnClickEnabled = false
                            }
                            showAppDisableDialog = false
                            Toast.makeText(context, "$appToDisable onClick scanning disabled", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.textButtonColors(
                            containerColor = PureWhite,
                            contentColor = PureBlack
                        )
                    ) {
                        Text("Disable onClick")
                    }
                    TextButton(
                        onClick = {
                            // Disable both
                            when (appToDisable) {
                                "WhatsApp" -> {
                                    monitorWhatsapp = false
                                    appPreferences.monitorWhatsapp = false
                                    appPreferences.whatsappNotificationEnabled = false
                                    appPreferences.whatsappOnClickEnabled = false
                                }
                                "Instagram" -> {
                                    monitorInstagram = false
                                    appPreferences.monitorInstagram = false
                                    appPreferences.instagramNotificationEnabled = false
                                    appPreferences.instagramOnClickEnabled = false
                                }
                                "Gmail" -> {
                                    monitorGmail = false
                                    appPreferences.monitorGmail = false
                                    appPreferences.gmailNotificationEnabled = false
                                    appPreferences.gmailOnClickEnabled = false
                                }
                                "Telegram" -> {
                                    monitorTelegram = false
                                    appPreferences.monitorTelegram = false
                                    appPreferences.telegramNotificationEnabled = false
                                    appPreferences.telegramOnClickEnabled = false
                                }
                                "Messages" -> {
                                    monitorMessages = false
                                    appPreferences.monitorMessages = false
                                    appPreferences.messagesNotificationEnabled = false
                                    appPreferences.messagesOnClickEnabled = false
                                }
                            }
                            showAppDisableDialog = false
                            Toast.makeText(context, "$appToDisable protection completely disabled", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.textButtonColors(
                            containerColor = PureWhite,
                            contentColor = DangerRed
                        )
                    ) {
                        Text("Disable Both")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAppDisableDialog = false
                    appToDisable = ""
                }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = Color.Transparent,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            shape = popupShape,
            tonalElevation = 0.dp
        )
    }

    // Clear Database Confirmation Dialog
    if (showClearConfirmDialog) {
        AlertDialog(
            modifier = Modifier.appContainer(hazeState = hazeState, cornerRadius = 24.dp),
            onDismissRequest = { showClearConfirmDialog = false },
            title = { Text("Clear All Data?", color = TextPrimary) },
            text = {
                Text(
                    "This will permanently delete all scan history, threat logs, and database records. IDs will be reset to 1.\n\nThis action cannot be undone.",
                    color = TextSecondary
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        clearDatabaseWithIdReset(context)
                        showClearConfirmDialog = false
                        Toast.makeText(context, "Database cleared successfully", Toast.LENGTH_SHORT).show()
                    }
                ) {
                    Text("Clear All", color = DangerRed)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirmDialog = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            containerColor = Color.Transparent,
            titleContentColor = TextPrimary,
            textContentColor = TextSecondary,
            shape = popupShape,
            tonalElevation = 0.dp
        )
    }
}

private fun openNotificationAccessSettings(context: Context) {
    try {
        val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
    }
}

private fun checkBrowserRoleGranted(context: Context): Boolean {
    return try {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
        val roleManager = context.getSystemService(RoleManager::class.java) ?: return false
        roleManager.isRoleAvailable(RoleManager.ROLE_BROWSER) &&
                roleManager.isRoleHeld(RoleManager.ROLE_BROWSER)
    } catch (e: Exception) {
        Log.e("SettingsScreen", "Error checking browser role", e)
        false
    }
}

private fun clearDatabaseWithIdReset(context: Context) {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            val db = com.zerothreat.core.data.db.AppDatabase.getDatabase(context)
            val repo = com.zerothreat.core.data.repository.UrlRepository(db.urlDao())
            repo.clearHistory()
            Log.d("SettingsScreen", "Database cleared and IDs reset successfully")
        } catch (e: Exception) {
            Log.e("SettingsScreen", "Failed to clear database", e)
        }
    }
}

private fun requestBrowserRoleOrSettings(context: Context, appPreferences: AppPreferences) {
    rememberExternalDefaultBrowser(context, appPreferences)

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        val roleManager = context.getSystemService(RoleManager::class.java)
        if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_BROWSER)) {
            if (roleManager.isRoleHeld(RoleManager.ROLE_BROWSER)) {
                Toast.makeText(context, "ZeroThreat is already set as browser.", Toast.LENGTH_SHORT)
                    .show()
                return
            }
            try {
                val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_BROWSER).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                return
            } catch (e: Exception) {
                Log.w("SettingsScreen", "Browser role request failed, opening settings fallback", e)
            }
        }
    }

    if (openLinkHandlerSettings(context)) return
    Toast.makeText(
        context,
        "Open Android Settings > Default apps > Browser app and choose ZeroThreat.",
        Toast.LENGTH_LONG
    ).show()
}

private fun openLinkHandlerSettings(context: Context): Boolean {
    val intents = listOf(
        Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        },
        Intent(Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        },
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${context.packageName}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        },
        Intent(Settings.ACTION_SETTINGS).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    )

    for (candidate in intents) {
        if (candidate.resolveActivity(context.packageManager) == null) continue
        try {
            context.startActivity(candidate)
            return true
        } catch (e: Exception) {
            Log.w("SettingsScreen", "Failed opening settings action: ${candidate.action}", e)
        }
    }

    return false
}

private fun getAvailableBrowsers(context: Context): List<BrowserOption> {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com")).apply {
        addCategory(Intent.CATEGORY_BROWSABLE)
    }
    return context.packageManager
        .queryIntentActivities(intent, PackageManager.MATCH_ALL)
        .mapNotNull { resolveInfo ->
            val activityInfo = resolveInfo.activityInfo ?: return@mapNotNull null
            if (activityInfo.packageName == context.packageName) {
                return@mapNotNull null
            }
            val label = resolveInfo.loadLabel(context.packageManager)
                ?.toString()
                ?.takeIf { it.isNotBlank() }
                ?: activityInfo.packageName
            BrowserOption(
                packageName = activityInfo.packageName,
                label = label
            )
        }
        .distinctBy { it.packageName }
        .sortedBy { it.label.lowercase() }
}

@Composable
private fun BrowserPickerDialog(
    browsers: List<BrowserOption>,
    selectedPackage: String,
    onSelectSystemDefault: () -> Unit,
    onSelectBrowser: (String) -> Unit,
    onDismiss: () -> Unit,
    popupShape: RoundedCornerShape
) {
    val hazeState = LocalUiSurfaceState.current
    AlertDialog(
        modifier = Modifier.appContainer(hazeState = hazeState, cornerRadius = 24.dp),
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Preferred Browser",
                color = TextPrimary,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                if (browsers.isEmpty()) {
                    Text(
                        text = "No browser apps detected. Set a default browser in Android settings.",
                        color = TextSecondary
                    )
                } else {
                    TextButton(
                        onClick = onSelectSystemDefault,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        val selectedMarker = if (selectedPackage.isBlank()) " (selected)" else ""
                        Text(
                            text = "System default$selectedMarker",
                            color = ElectricPurple
                        )
                    }
                    browsers.forEach { browser ->
                        TextButton(
                            onClick = { onSelectBrowser(browser.packageName) },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val selectedMarker = if (browser.packageName == selectedPackage) {
                                " (selected)"
                            } else {
                                ""
                            }
                            Text(
                                text = browser.label + selectedMarker,
                                color = ElectricPurple
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = TextSecondary)
            }
        },
        containerColor = Color.Transparent,
        titleContentColor = TextPrimary,
        textContentColor = TextSecondary,
        shape = popupShape,
        tonalElevation = 0.dp
    )
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = TextWhite,
        fontWeight = FontWeight.Bold
    )
}

@Composable
private fun SettingsToggleCard(
    icon: ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    val hazeState = LocalUiSurfaceState.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .appContainer(hazeState = hazeState, cornerRadius = 24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = title, tint = PeachAccent, modifier = Modifier.size(32.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(description, style = MaterialTheme.typography.bodySmall, color = TextSecondary, lineHeight = 18.sp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = TextWhite,
                    checkedTrackColor = SafeGreen,
                    uncheckedThumbColor = PureBlack,
                    uncheckedTrackColor = PureWhite
                )
            )
        }
    }
}

@Composable
private fun SettingsToggleItem(
    icon: ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = title, tint = PeachAccent, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, color = TextPrimary, fontWeight = FontWeight.Medium)
            Text(description, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = TextWhite,
                checkedTrackColor = SafeGreen,
                uncheckedThumbColor = PureBlack,
                uncheckedTrackColor = PureWhite
            )
        )
    }
}

@Composable
private fun SettingsActionCard(
    icon: ImageVector,
    title: String,
    description: String,
    actionText: String,
    onAction: () -> Unit,
    isDestructive: Boolean = false
) {
    val hazeState = LocalUiSurfaceState.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onAction)
            .appContainer(hazeState = hazeState, cornerRadius = 24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isDestructive) DangerRed else PeachAccent,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                Spacer(modifier = Modifier.height(4.dp))
                Text(description, style = MaterialTheme.typography.bodySmall, color = TextSecondary, lineHeight = 18.sp)
            }
            Spacer(modifier = Modifier.width(12.dp))
            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isDestructive) PureWhite else SurfaceVariant,
                    contentColor = if (isDestructive) DangerRed else PeachAccent
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(text = actionText, style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}
