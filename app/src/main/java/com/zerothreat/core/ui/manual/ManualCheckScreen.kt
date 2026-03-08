package com.zerothreat.core.ui.manual

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zerothreat.core.detector.PhishingResult
import com.zerothreat.core.ui.alerts.ThreatAlertDialog
import com.zerothreat.core.ui.components.SecurityPulseAnimation
import com.zerothreat.core.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualCheckScreen(
    viewModel: ManualCheckViewModel = viewModel()
) {
    val hazeState = LocalUiSurfaceState.current
    var urlInput by remember { mutableStateOf("") }
    val isChecking by viewModel.isChecking.collectAsState()
    val scanResult by viewModel.scanResult.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .appBackground(hazeState)
            .padding(Spacing.Screen.horizontalPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(Spacing.Screen.topSpacing))

        SecurityPulseAnimation(
            modifier = Modifier.size(86.dp),
            tint = ElectricPurple
        )

        Spacer(modifier = Modifier.height(Spacing.xxl))

        Text(
            text = "Check a Link",
            style = MaterialTheme.typography.headlineSmall,
            color = TextWhite,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(Spacing.xs))

        Text(
            text = "Paste any URL below to check if it's safe",
            style = MaterialTheme.typography.bodyMedium,
            color = TextWhite.copy(alpha = 0.85f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(Spacing.Screen.topSpacing))

        // URL Input Field
        OutlinedTextField(
            value = urlInput,
            onValueChange = { urlInput = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = {
                Text("https://example.com", color = TextMuted)
            },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Default.Link,
                    contentDescription = "URL",
                    tint = ElectricPurple
                )
            },
            trailingIcon = {
                if (urlInput.isNotEmpty()) {
                    IconButton(onClick = { urlInput = "" }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear",
                            tint = TextSecondary
                        )
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = ElectricPurple,
                unfocusedBorderColor = TextMuted,
                focusedContainerColor = CardBackground,
                unfocusedContainerColor = CardBackground,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                cursorColor = ElectricPurple,
                focusedLeadingIconColor = ElectricPurple,
                unfocusedLeadingIconColor = TextMuted
            ),
            shape = RoundedCornerShape(Spacing.Button.cornerRadius),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = {
                    if (urlInput.isNotEmpty()) {
                        viewModel.checkUrl(urlInput)
                    }
                }
            ),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(Spacing.md))

        // Check Button
        Button(
            onClick = {
                if (urlInput.isNotEmpty()) {
                    viewModel.checkUrl(urlInput)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(Spacing.Button.height),
            enabled = urlInput.isNotEmpty() && !isChecking,
            colors = ButtonDefaults.buttonColors(
                containerColor = ElectricPurple,
                contentColor = PureBlack,
                disabledContainerColor = TextMuted.copy(alpha = 0.4f),
                disabledContentColor = TextSecondary
            ),
            shape = RoundedCornerShape(Spacing.Button.cornerRadius)
        ) {
            if (isChecking) {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    color = TextPrimary,
                    strokeWidth = 2.dp
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = "Check",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Check Link",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(Spacing.Screen.topSpacing))

        // Info Card (Kept static for now)
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .appContainer(hazeState = hazeState, cornerRadius = 24.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.Transparent
            ),
            shape = RoundedCornerShape(Spacing.Card.borderRadius),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(Spacing.lg)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = "Info",
                        tint = ElectricPurple,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(Spacing.md))
                    Text(
                        text = "How it works",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(Spacing.md))

                InfoItem("✓ Checks against known phishing databases")
                InfoItem("✓ Analyzes URL patterns and keywords")
                InfoItem("✓ Provides instant threat assessment")
                InfoItem("✓ 100% private - all checks are local")
            }
        }
    }

    // Alert Dialog - Enhanced with score and description
    scanResult?.let { result ->
        val displayMessage = when {
            result.dnsCheckFailed -> {
                "${result.url}\n\n${result.description}\n\nThis domain does not exist or cannot be resolved."
            }
            result.result == PhishingResult.SAFE -> {
                val safetyPercentage = 100 - result.score
                "${result.url}\n\n${safetyPercentage}% Safe\n\n${result.description}"
            }
            else -> {
                "${result.url}\n\n${result.score}% ${
                    when(result.result) {
                        PhishingResult.SUSPICIOUS -> "Suspicious"
                        PhishingResult.PHISHING -> "Phishing"
                        else -> "Unknown"
                    }
                }\n\n${result.description}"
            }
        }

        ThreatAlertDialog(
            url = result.url,
            threatLevel = result.result,
            reason = displayMessage,
            onBlock = { viewModel.dismissAlert() },
            onIgnore = { viewModel.dismissAlert() },
            onDismiss = { viewModel.dismissAlert() }
        )
    }
}

@Composable
fun InfoItem(text: String) {
    Row(
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            lineHeight = 20.sp
        )
    }
}
