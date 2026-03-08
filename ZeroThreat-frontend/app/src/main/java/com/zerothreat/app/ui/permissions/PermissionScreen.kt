package com.zerothreat.app.ui.permissions

import android.content.Intent
import android.provider.Settings
import androidx.compose. foundation.background
import androidx.compose.foundation. layout.*
import androidx.compose. foundation.shape.RoundedCornerShape
import androidx.compose.material. icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit. sp
import com.zerothreat.app. ui.theme.*

@Composable
fun PermissionRequestScreen(
    onPermissionsGranted: () -> Unit,
    onSkip:  () -> Unit
) {
    val context = LocalContext.current
    var notificationPermissionGranted by remember { mutableStateOf(false) }
    var vpnPermissionGranted by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(24.dp),
        horizontalAlignment = Alignment. CenterHorizontally
    ) {
        Spacer(modifier = Modifier. height(32.dp))

        Icon(
            imageVector = Icons. Default.Security,
            contentDescription = "Permissions",
            tint = ElectricPurple,
            modifier = Modifier.size(80.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Enable Smart Protection",
            style = MaterialTheme.typography.headlineMedium,
            color = TextPrimary,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "ZeroThreat needs these permissions to protect you automatically",
            style = MaterialTheme. typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign. Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Notification Listener Permission
        PermissionCard(
            icon = Icons.Default.Notifications,
            title = "Notification Access",
            description = "Scan notifications for phishing links in real-time",
            isGranted = notificationPermissionGranted,
            onRequestPermission = {
                val intent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)
                context.startActivity(intent)
                // Toggle state when button is clicked
                notificationPermissionGranted = true
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // VPN Permission
        PermissionCard(
            icon = Icons.Default.VpnKey,
            title = "Network Monitoring",
            description = "Monitor clicked links before they open in browser",
            isGranted = vpnPermissionGranted,
            onRequestPermission = {
                vpnPermissionGranted = true
            }
        )

        Spacer(modifier = Modifier. weight(1f))

        // Privacy note
        Card(
            modifier = Modifier. fillMaxWidth(),
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
                    text = "All data stays on your device.  We never upload anything.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    lineHeight = 18.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Continue button
        Button(
            onClick = {
                onPermissionsGranted()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = ElectricPurple
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "Continue",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        TextButton(onClick = onSkip) {
            Text(
                text = "Skip for now",
                color = TextSecondary
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun PermissionCard(
    icon: ImageVector,
    title: String,
    description: String,
    isGranted: Boolean,
    onRequestPermission: () -> Unit
) {
    Card(
        modifier = Modifier. fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = CardBackground),
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
                modifier = Modifier.size(40.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography. titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier. height(4.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary,
                    lineHeight = 18.sp
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            if (isGranted) {
                Icon(
                    imageVector = Icons. Default.CheckCircle,
                    contentDescription = "Granted",
                    tint = SafeGreen,
                    modifier = Modifier.size(28.dp)
                )
            } else {
                OutlinedButton(
                    onClick = onRequestPermission,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = ElectricPurple
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Enable", fontSize = 12.sp)
                }
            }
        }
    }
}