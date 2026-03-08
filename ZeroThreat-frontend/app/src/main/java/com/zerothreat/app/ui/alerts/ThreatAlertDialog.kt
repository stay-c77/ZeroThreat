package com.zerothreat.app.ui.alerts

import androidx.compose.foundation.background
import androidx.compose. foundation.layout.*
import androidx. compose.foundation.shape.RoundedCornerShape
import androidx. compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx. compose.ui. unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window. DialogProperties
import com.zerothreat.app.ui.theme.*

enum class ThreatLevel {
    SAFE, SUSPICIOUS, PHISHING
}

@Composable
fun ThreatAlertDialog(
    url:  String,
    threatLevel: ThreatLevel,
    reason:  String,
    onBlock: () -> Unit,
    onViewSafely: () -> Unit,
    onIgnore: () -> Unit,
    onReport: () -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = CardBackground
            ),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(
                modifier = Modifier. padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = when (threatLevel) {
                        ThreatLevel. SAFE -> Icons.Default.CheckCircle
                        ThreatLevel.SUSPICIOUS -> Icons.Default.Warning
                        ThreatLevel.PHISHING -> Icons. Default.Dangerous
                    },
                    contentDescription = "Threat Level",
                    tint = when (threatLevel) {
                        ThreatLevel. SAFE -> SafeGreen
                        ThreatLevel.SUSPICIOUS -> WarningYellow
                        ThreatLevel.PHISHING -> DangerRed
                    },
                    modifier = Modifier. size(64.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = when (threatLevel) {
                        ThreatLevel.SAFE -> "Link is Safe"
                        ThreatLevel.SUSPICIOUS -> "Suspicious Link Detected"
                        ThreatLevel.PHISHING -> "⚠️ PHISHING ALERT"
                    },
                    style = MaterialTheme. typography.headlineSmall,
                    color = when (threatLevel) {
                        ThreatLevel.SAFE -> SafeGreen
                        ThreatLevel.SUSPICIOUS -> WarningYellow
                        ThreatLevel.PHISHING -> DangerRed
                    },
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    modifier = Modifier. fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = DarkBackground
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = url,
                        style = MaterialTheme.typography. bodyMedium,
                        color = TextPrimary,
                        modifier = Modifier.padding(12.dp),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = reason,
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary,
                    textAlign = TextAlign.Center,
                    lineHeight = 20.sp
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onBlock,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DangerRed
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Block & Close", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedButton(
                    onClick = onIgnore,
                    modifier = Modifier. fillMaxWidth(),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = TextSecondary
                    )
                ) {
                    Text("Continue Anyway")
                }
            }
        }
    }
}