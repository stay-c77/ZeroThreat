package com.zerothreat.app.ui.settings

import androidx. compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx. compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose. material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx. compose.ui.unit.sp
import com.zerothreat. app.data.AppPreferences
import com.zerothreat. app.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    appPreferences:  AppPreferences,
    onNavigateBack: () -> Unit,
    onNavigateToPrivacy: () -> Unit,
    onNavigateToAbout: () -> Unit,
    onNavigateToHelp: () -> Unit
) {
    var smartModeEnabled by remember { mutableStateOf(appPreferences.smartModeEnabled) }
    var notificationMonitoring by remember { mutableStateOf(appPreferences.notificationMonitoring) }
    var linkMonitoring by remember { mutableStateOf(appPreferences.linkMonitoring) }
    var autoUpdate by remember { mutableStateOf(true) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Settings",
                        color = TextPrimary,
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = TextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults. topAppBarColors(
                    containerColor = DarkBackground
                )
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
            // Protection Mode Section
            item {
                SectionHeader("Protection Mode")
                Spacer(modifier = Modifier.height(12.dp))
            }

            item {
                SettingsToggleCard(
                    icon = Icons.Default.AutoAwesome,
                    title = "Smart Mode",
                    description = "Automatic protection across all apps",
                    checked = smartModeEnabled,
                    onCheckedChange = {
                        smartModeEnabled = it
                        appPreferences. smartModeEnabled = it
                        if (! it) {
                            // Turn off monitoring when smart mode is disabled
                            notificationMonitoring = false
                            linkMonitoring = false
                            appPreferences.notificationMonitoring = false
                            appPreferences.linkMonitoring = false
                        }
                    }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            if (smartModeEnabled) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = CardBackground
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            SettingsToggleItem(
                                icon = Icons. Default.Notifications,
                                title = "Notification Monitoring",
                                description = "Scan incoming notifications",
                                checked = notificationMonitoring,
                                onCheckedChange = {
                                    notificationMonitoring = it
                                    appPreferences.notificationMonitoring = it
                                }
                            )

                            Divider(
                                color = TextMuted. copy(alpha = 0.2f),
                                modifier = Modifier. padding(vertical = 12.dp)
                            )

                            SettingsToggleItem(
                                icon = Icons. Default.VpnKey,
                                title = "Link Monitoring (VPN)",
                                description = "Monitor clicked links",
                                checked = linkMonitoring,
                                onCheckedChange = {
                                    linkMonitoring = it
                                    appPreferences. linkMonitoring = it
                                }
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }

            // Detection Settings Section
            item {
                SectionHeader("Detection Settings")
                Spacer(modifier = Modifier.height(12.dp))
            }

            item {
                SettingsActionCard(
                    icon = Icons.Default.Update,
                    title = "Update Blacklist Database",
                    description = "Last updated: 2 hours ago",
                    actionText = "Update Now",
                    onAction = { /* Update blacklist */ }
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            item {
                SettingsToggleCard(
                    icon = Icons.Default.CloudDownload,
                    title = "Auto-Update Blacklist",
                    description = "Update daily when connected to WiFi",
                    checked = autoUpdate,
                    onCheckedChange = { autoUpdate = it }
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            // Privacy & Data Section
            item {
                SectionHeader("Privacy & Data")
                Spacer(modifier = Modifier.height(12.dp))
            }

            item {
                SettingsActionCard(
                    icon = Icons.Default.Delete,
                    title = "Clear Threat History",
                    description = "Delete all stored threat logs",
                    actionText = "Clear",
                    onAction = { /* Clear history */ },
                    isDestructive = true
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            item {
                SettingsNavigationCard(
                    icon = Icons.Default. PrivacyTip,
                    title = "Privacy Policy",
                    description = "Learn how we protect your data",
                    onClick = onNavigateToPrivacy
                )
                Spacer(modifier = Modifier.height(24.dp))
            }

            // About Section
            item {
                SectionHeader("About")
                Spacer(modifier = Modifier.height(12.dp))
            }

            item {
                SettingsNavigationCard(
                    icon = Icons.Default.Info,
                    title = "About ZeroThreat",
                    description = "Version 1.0.0",
                    onClick = onNavigateToAbout
                )
                Spacer(modifier = Modifier. height(12.dp))
            }

            item {
                SettingsNavigationCard(
                    icon = Icons.Default.Help,
                    title = "Help & Support",
                    description = "FAQs and troubleshooting",
                    onClick = onNavigateToHelp
                )
            }
        }
    }
}

@Composable
fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = PeachAccent,
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp
    )
}

@Composable
fun SettingsToggleCard(
    icon: ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange:  (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = CardBackground
        ),
        shape = RoundedCornerShape(16.dp),
        border = androidx.compose.foundation.BorderStroke(1.5.dp, PeachAccent.copy(alpha = if(checked) 0.5f else 0.2f))
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
                tint = PeachAccent,
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    lineHeight = 18.sp
                )
            }

            Spacer(modifier = Modifier. width(12.dp))

            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = TextPrimary,
                    checkedTrackColor = PeachAccent,
                    uncheckedThumbColor = TextSecondary,
                    uncheckedTrackColor = TextMuted.copy(alpha = 0.5f)
                )
            )
        }
    }
}

@Composable
fun SettingsToggleItem(
    icon: ImageVector,
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier. fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = ElectricPurple,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = TextPrimary,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary,
                fontSize = 12.sp
            )
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = TextPrimary,
                checkedTrackColor = ElectricPurple
            )
        )
    }
}

@Composable
fun SettingsActionCard(
    icon: ImageVector,
    title: String,
    description:  String,
    actionText: String,
    onAction: () -> Unit,
    isDestructive: Boolean = false
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = CardBackground
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment. CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isDestructive) DangerRed else ElectricPurple,
                modifier = Modifier. size(32.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    lineHeight = 18.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            OutlinedButton(
                onClick = onAction,
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = if (isDestructive) DangerRed else ElectricPurple
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(actionText, fontSize = 13.sp)
            }
        }
    }
}

@Composable
fun SettingsNavigationCard(
    icon:  ImageVector,
    title: String,
    description: String,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = CardBackground
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment. CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = ElectricPurple,
                modifier = Modifier.size(32.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier. weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme. typography.bodySmall,
                    color = TextSecondary,
                    lineHeight = 18.sp
                )
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Navigate",
                tint = TextMuted,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}