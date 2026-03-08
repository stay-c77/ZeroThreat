package com.zerothreat.core.ui.alerts

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.zerothreat.core.detector.PhishingResult
import com.zerothreat.core.ui.components.SecurityPulseAnimation
import com.zerothreat.core.ui.theme.*

@Composable
fun ThreatAlertDialog(
    url: String,
    threatLevel: PhishingResult,
    reason: String = "",
    onBlock: () -> Unit,
    onIgnore: () -> Unit,
    onDismiss: () -> Unit
) {
    val hazeState = LocalUiSurfaceState.current
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
                .padding(Spacing.lg)
                .appContainer(hazeState = hazeState, cornerRadius = 24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.Transparent
            ),
            shape = RoundedCornerShape(Spacing.Dialog.cardCornerRadius),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.padding(Spacing.Dialog.contentPadding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                SecurityPulseAnimation(
                    modifier = Modifier.size(Spacing.Dialog.iconSize),
                    tint = when (threatLevel) {
                        PhishingResult.SAFE -> SafeGreen
                        PhishingResult.SUSPICIOUS -> WarningYellow
                        PhishingResult.PHISHING -> DangerRed
                    }
                )

                Spacer(modifier = Modifier.height(Spacing.Dialog.iconSpacing))

                Text(
                    text = when (threatLevel) {
                        PhishingResult.SAFE -> "Link is Safe"
                        PhishingResult.SUSPICIOUS -> "Suspicious Link Detected"
                        PhishingResult.PHISHING -> "PHISHING ALERT"
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    color = when (threatLevel) {
                        PhishingResult.SAFE -> SafeGreen
                        PhishingResult.SUSPICIOUS -> WarningYellow
                        PhishingResult.PHISHING -> DangerRed
                    },
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(Spacing.Dialog.titleSpacing))

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .appContainer(hazeState = hazeState, cornerRadius = 24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(Spacing.Dialog.urlCardRadius)
                ) {
                    Text(
                        text = url,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary,
                        modifier = Modifier.padding(Spacing.md),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(Spacing.Dialog.urlCardSpacing))

                if (reason.isNotEmpty()) {
                    Text(
                        text = reason,
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )
                    Spacer(modifier = Modifier.height(Spacing.Dialog.descriptionSpacing))
                } else {
                    Spacer(modifier = Modifier.height(Spacing.Dialog.descriptionSpacing))
                }

                Button(
                    onClick = onBlock,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(Spacing.Button.height),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (threatLevel == PhishingResult.SAFE) SafeGreen else DangerRed,
                        contentColor = if (threatLevel == PhishingResult.SAFE) PureBlack else TextWhite
                    ),
                    shape = RoundedCornerShape(Spacing.Button.cornerRadius)
                ) {
                    Text(
                        text = if (threatLevel == PhishingResult.SAFE) "OK" else "Block & Close",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (threatLevel != PhishingResult.SAFE) {
                    Spacer(modifier = Modifier.height(Spacing.Button.verticalSpacing))

                    Button(
                        onClick = onIgnore,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SurfaceVariant,
                            contentColor = ElectricPurple
                        )
                    ) {
                        Text("Continue Anyway")
                    }
                }
            }
        }
    }
}
