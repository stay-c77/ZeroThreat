package com.zerothreat.core.ui.alerts

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
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

// ── Helpers ───────────────────────────────────────────────────────────────────

private fun parseDomain(url: String): String =
    runCatching { Uri.parse(url).host?.removePrefix("www.") ?: url }.getOrDefault(url)

private fun parsePath(url: String, maxLen: Int = 38): String {
    val raw = runCatching {
        val u = Uri.parse(url)
        val p = u.path ?: ""
        val q = u.query
        if (q != null) "$p?$q" else p
    }.getOrDefault("")
    return if (raw.length <= maxLen) raw else raw.take(maxLen - 1) + "…"
}

private fun headline(threatLevel: PhishingResult, score: Int): String = when (threatLevel) {
    PhishingResult.SAFE       -> "Link is Safe"
    PhishingResult.PHISHING   -> "Phishing Link Detected"
    PhishingResult.SUSPICIOUS -> when {
        score < 40  -> "Potentially Suspicious Link"
        score < 70  -> "Suspicious Link Detected"
        else        -> "High Risk Link Detected"
    }
}

enum class ThreatAlertMode {
    DEFAULT,
    DNS_NOT_FOUND
}

// ── Composable ────────────────────────────────────────────────────────────────

@Composable
fun ThreatAlertDialog(
    url: String,
    threatLevel: PhishingResult,
    score: Int = 0,
    detectionTag: String = "",
    reasonBullets: List<String> = emptyList(),
    alertMode: ThreatAlertMode = ThreatAlertMode.DEFAULT,
    onBlock: () -> Unit,
    onIgnore: () -> Unit,
    onContinue: () -> Unit = onIgnore,
    onDismiss: () -> Unit
) {
    val hazeState = LocalUiSurfaceState.current

    var continueConfirmPending by remember { mutableStateOf(false) }

    val accentColor = when (threatLevel) {
        PhishingResult.SAFE       -> SafeGreen
        PhishingResult.SUSPICIOUS -> WarningYellow
        PhishingResult.PHISHING   -> DangerRed
    }
    val isDnsNotFound = alertMode == ThreatAlertMode.DNS_NOT_FOUND

    val domain = parseDomain(url)
    val path   = parsePath(url)

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
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            shape = RoundedCornerShape(Spacing.Dialog.cardCornerRadius),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.padding(Spacing.Dialog.contentPadding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                // ── Icon ──────────────────────────────────────────────────────
                SecurityPulseAnimation(
                    modifier = Modifier.size(Spacing.Dialog.iconSize),
                    tint = accentColor
                )

                Spacer(modifier = Modifier.height(Spacing.Dialog.iconSpacing))

                // ── Level 1: Adaptive headline ────────────────────────────────
                Text(
                    text = if (isDnsNotFound) "Domain not found" else headline(threatLevel, score),
                    style = MaterialTheme.typography.headlineSmall,
                    color = accentColor,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(Spacing.Dialog.titleSpacing))

                // ── Level 2: URL card — domain large, path small ──────────────
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .appContainer(hazeState = hazeState, cornerRadius = 12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                    shape = RoundedCornerShape(Spacing.Dialog.urlCardRadius)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = Spacing.md, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = domain,
                            style = MaterialTheme.typography.titleMedium,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center,
                            maxLines = 1
                        )
                        if (path.isNotBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = path,
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary,
                                textAlign = TextAlign.Center,
                                maxLines = 1
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // ── Risk score badge ──────────────────────────────────────────
                if (!isDnsNotFound && threatLevel != PhishingResult.SAFE && score > 0) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = accentColor.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.35f))
                    ) {
                        Text(
                            text = "Risk Score: $score%",
                            style = MaterialTheme.typography.labelMedium,
                            color = accentColor,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // ── Detection tag ─────────────────────────────────────────────
                if (!isDnsNotFound && detectionTag.isNotEmpty()) {
                    Text(
                        text = "Detection: $detectionTag",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        textAlign = TextAlign.Center,
                        letterSpacing = 0.3.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                }

                // ── Reason bullets ────────────────────────────────────────────
                if (!isDnsNotFound && reasonBullets.isNotEmpty()) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.Start
                    ) {
                        Text(
                            text = "Reason",
                            style = MaterialTheme.typography.labelMedium,
                            color = TextSecondary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        reasonBullets.forEach { bullet ->
                            Text(
                                text = "• $bullet",
                                style = MaterialTheme.typography.bodySmall,
                                color = TextSecondary,
                                lineHeight = 20.sp
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(Spacing.Dialog.descriptionSpacing))
                } else {
                    Spacer(modifier = Modifier.height(Spacing.Dialog.descriptionSpacing))
                }

                // ── Buttons ───────────────────────────────────────────────────
                if (isDnsNotFound) {

                    OutlinedButton(
                        onClick = onIgnore,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(Spacing.Button.height),
                        shape = RoundedCornerShape(Spacing.Button.cornerRadius),
                        border = BorderStroke(1.dp, TextSecondary.copy(alpha = 0.35f))
                    ) {
                        Text(
                            text = "Close",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextSecondary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                } else if (threatLevel == PhishingResult.SAFE) {

                    // Safe: Continue and Close buttons
                    Button(
                        onClick = onContinue,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(Spacing.Button.height),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SafeGreen,
                            contentColor = PureBlack
                        ),
                        shape = RoundedCornerShape(Spacing.Button.cornerRadius)
                    ) {
                        Text(
                            text = "Continue",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    Spacer(modifier = Modifier.height(Spacing.Button.verticalSpacing))

                    OutlinedButton(
                        onClick = onIgnore,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(Spacing.Button.height),
                        shape = RoundedCornerShape(Spacing.Button.cornerRadius),
                        border = BorderStroke(1.dp, TextSecondary.copy(alpha = 0.35f))
                    ) {
                        Text(
                            text = "Close",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextSecondary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                } else {

                    // ── Primary: Block Link ───────────────────────────────────
                    Button(
                        onClick = onBlock,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(Spacing.Button.height),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentColor,
                            contentColor = if (threatLevel == PhishingResult.SUSPICIOUS) PureBlack else TextWhite
                        ),
                        shape = RoundedCornerShape(Spacing.Button.cornerRadius)
                    ) {
                        Text(
                            text = "Block Link",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(Spacing.Button.verticalSpacing))

                    // ── Secondary: Close (outlined) ───────────────────────────
                    OutlinedButton(
                        onClick = onIgnore,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(Spacing.Button.height),
                        shape = RoundedCornerShape(Spacing.Button.cornerRadius),
                        border = BorderStroke(1.dp, TextSecondary.copy(alpha = 0.35f))
                    ) {
                        Text(
                            text = "Close",
                            style = MaterialTheme.typography.titleSmall,
                            color = TextSecondary,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // ── Tertiary: Continue Anyway — hidden for PHISHING ───────
                    if (threatLevel != PhishingResult.PHISHING) {
                        Spacer(modifier = Modifier.height(4.dp))

                        if (continueConfirmPending) {
                            // Double-tap confirm row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Are you sure? ",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = TextSecondary
                                )
                                TextButton(
                                    onClick = {
                                        continueConfirmPending = false
                                        onContinue()
                                    },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                                ) {
                                    Text(
                                        text = "Yes, open it",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = DangerRed,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                TextButton(
                                    onClick = { continueConfirmPending = false },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                                ) {
                                    Text(
                                        text = "Cancel",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = TextSecondary
                                    )
                                }
                            }
                        } else {
                            TextButton(
                                onClick = { continueConfirmPending = true },
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            ) {
                                Text(
                                    text = "Continue Anyway",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = TextSecondary.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}