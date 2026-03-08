package com.zerothreat.app.ui.mode

import androidx.compose.foundation.background
import androidx.compose.foundation. layout.*
import androidx.compose.foundation. shape.RoundedCornerShape
import androidx.compose.material. icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui. unit.dp
import androidx.compose.ui.unit.sp
import com.zerothreat.app.data.AppPreferences
import com.zerothreat.app.ui.theme.*

enum class AppMode {
    MANUAL, SMART
}

@Composable
fun ModeSelectionScreen(
    appPreferences: AppPreferences,
    onModeSelected: (AppMode) -> Unit
) {
    var selectedMode by remember { mutableStateOf<AppMode? >(null) }
    var notificationMonitoring by remember { mutableStateOf(false) }
    var linkMonitoring by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier. height(32.dp))

        Text(
            text = "Choose Your Protection Mode",
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier. height(8.dp))

        Text(
            text = "You can change this anytime in settings",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Manual Mode Card
        ModeCard(
            icon = Icons.Default.TouchApp,
            title = "Manual Mode",
            description = "Paste URLs manually to check if they're safe.  Perfect for privacy-conscious users.",
            isSelected = selectedMode == AppMode.MANUAL,
            onClick = { selectedMode = AppMode.MANUAL }
        )

        Spacer(modifier = Modifier. height(16.dp))

        // Smart Mode Card
        ModeCard(
            icon = Icons. Default.AutoAwesome,
            title = "Smart Mode",
            description = "Automatic real-time protection across all apps. Enable what you need below.",
            isSelected = selectedMode == AppMode.SMART,
            onClick = { selectedMode = AppMode.SMART }
        )

        // Smart Mode Options
        if (selectedMode == AppMode.SMART) {
            Spacer(modifier = Modifier. height(16.dp))

            Card(
                modifier = Modifier. fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = CardBackground
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Smart Mode Options",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Notification Monitoring Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Notifications,
                            contentDescription = "Notifications",
                            tint = ElectricPurple,
                            modifier = Modifier. size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Notification Monitoring",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Scan incoming notifications",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                        Switch(
                            checked = notificationMonitoring,
                            onCheckedChange = { notificationMonitoring = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = TextPrimary,
                                checkedTrackColor = ElectricPurple
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Divider(color = TextMuted. copy(alpha = 0.2f))

                    Spacer(modifier = Modifier. height(16.dp))

                    // Link Monitoring Toggle
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Link,
                            contentDescription = "Links",
                            tint = ElectricPurple,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Link Monitoring (VPN)",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextPrimary,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Monitor clicked links",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                        Switch(
                            checked = linkMonitoring,
                            onCheckedChange = { linkMonitoring = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = TextPrimary,
                                checkedTrackColor = ElectricPurple
                            )
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier. weight(1f))

        // Continue Button
        Button(
            onClick = {
                selectedMode?. let { mode ->
                    // Save preferences based on mode
                    when (mode) {
                        AppMode. MANUAL -> {
                            appPreferences.smartModeEnabled = false
                            appPreferences.notificationMonitoring = false
                            appPreferences.linkMonitoring = false
                        }
                        AppMode. SMART -> {
                            appPreferences.smartModeEnabled = true
                            appPreferences.notificationMonitoring = notificationMonitoring
                            appPreferences.linkMonitoring = linkMonitoring
                        }
                    }
                    onModeSelected(mode)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = selectedMode != null,
            colors = ButtonDefaults. buttonColors(
                containerColor = ElectricPurple,
                disabledContainerColor = TextMuted
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "Continue",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun ModeCard(
    icon: ImageVector,
    title: String,
    description:  String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier. fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) ElectricPurple. copy(alpha = 0.15f) else CardBackground
        ),
        shape = RoundedCornerShape(16.dp),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(2.dp, ElectricPurple)
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = if (isSelected) ElectricPurple else TextSecondary,
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = if (isSelected) ElectricPurple else TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    lineHeight = 20.sp
                )
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = ElectricPurple,
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    }
}