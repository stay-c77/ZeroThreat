package com.zerothreat.core.ui.manual

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.rememberNavController
import com.zerothreat.core.detector.PhishingResult
import com.zerothreat.core.ui.alerts.ThreatAlertDialog
import com.zerothreat.core.ui.components.SecurityPulseAnimation
import com.zerothreat.core.ui.theme.*
import kotlinx.coroutines.delay

private val SCAN_MESSAGES = listOf(
    "Resolving domain...",
    "Checking blacklists...",
    "Analysing URL patterns...",
    "Inspecting keywords...",
    "Calculating threat score...",
    "Finalising report..."
)

// ── Structured alert data passed to the dialog ────────────────────────────────
private data class AlertData(
    val detectionTag: String,
    val reasonBullets: List<String>
)

private fun buildAlertData(
    dnsCheckFailed: Boolean,
    result: PhishingResult,
    score: Int,
    description: String
): AlertData = when {
    dnsCheckFailed -> AlertData(
        detectionTag  = "Domain not found",
        reasonBullets = listOf(
            "This domain could not be resolved",
            "The link may be inactive or malicious"
        )
    )
    result == PhishingResult.SAFE -> AlertData(
        detectionTag  = "No threats found",
        reasonBullets = listOf(
            "No known phishing patterns detected",
            "Domain appears legitimate"
        )
    )
    result == PhishingResult.PHISHING -> AlertData(
        detectionTag  = "Phishing pattern",
        reasonBullets = bulletsFromDescription(description).ifEmpty {
            listOf("Matches known phishing signatures")
        }
    )
    else -> AlertData(
        detectionTag  = "Suspicious pattern",
        reasonBullets = bulletsFromDescription(description).ifEmpty {
            listOf("Risk score: $score%", "Unusual URL characteristics detected")
        }
    )
}

private fun bulletsFromDescription(description: String): List<String> {
    if (description.isBlank()) return emptyList()
    return description
        .split(Regex("[\\n;]"))
        .map { it.trim().trimStart('-', '•', '*').trim() }
        .filter { it.isNotBlank() }
        .take(3)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManualCheckScreen(
    navController: NavController = rememberNavController(),
    viewModel: ManualCheckViewModel = viewModel()
) {
    val hazeState = LocalUiSurfaceState.current
    var urlInput by remember { mutableStateOf("") }
    val isChecking by viewModel.isChecking.collectAsState()
    val scanResult by viewModel.scanResult.collectAsState()

    if (isChecking) {
        ScanningOverlay(url = urlInput)
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .appBackground(hazeState)
            .padding(Spacing.Screen.horizontalPadding),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(Spacing.Screen.topSpacing))
        SecurityPulseAnimation(modifier = Modifier.size(86.dp), tint = NeonTeal)
        Spacer(modifier = Modifier.height(Spacing.xxl))
        Text(
            text = "Check a Link",
            style = MaterialTheme.typography.headlineSmall,
            color = TextWhite, fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(Spacing.xs))
        Text(
            text = "Paste any URL below to check if it's safe",
            style = MaterialTheme.typography.bodyMedium,
            color = TextWhite.copy(alpha = 0.85f), textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(Spacing.Screen.topSpacing))
        OutlinedTextField(
            value = urlInput,
            onValueChange = { urlInput = it },
            modifier = Modifier.fillMaxWidth(),
            placeholder = { Text("https://example.com", color = TextMuted) },
            leadingIcon = {
                Icon(imageVector = Icons.Default.Link, contentDescription = "URL", tint = NeonTeal)
            },
            trailingIcon = {
                if (urlInput.isNotEmpty()) {
                    IconButton(onClick = { urlInput = "" }) {
                        Icon(Icons.Default.Clear, contentDescription = "Clear", tint = TextSecondary)
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = NeonTeal,
                unfocusedBorderColor = TextMuted,
                focusedContainerColor = CardBackground,
                unfocusedContainerColor = CardBackground,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                cursorColor = NeonTeal,
                focusedLeadingIconColor = NeonTeal,
                unfocusedLeadingIconColor = TextMuted
            ),
            shape = RoundedCornerShape(Spacing.Button.cornerRadius),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = { if (urlInput.isNotEmpty()) viewModel.checkUrl(urlInput) }
            ),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(Spacing.md))
        Button(
            onClick = { if (urlInput.isNotEmpty()) viewModel.checkUrl(urlInput) },
            modifier = Modifier.fillMaxWidth().height(Spacing.Button.height),
            enabled = urlInput.isNotEmpty(),
            colors = ButtonDefaults.buttonColors(
                containerColor = NeonTeal,
                contentColor = PureBlack,
                disabledContainerColor = TextMuted.copy(alpha = 0.4f),
                disabledContentColor = TextSecondary
            ),
            shape = RoundedCornerShape(Spacing.Button.cornerRadius)
        ) {
            Icon(imageVector = Icons.Default.Security, contentDescription = "Check",
                modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "Check Link", style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold)
        }
        Spacer(modifier = Modifier.height(Spacing.Screen.topSpacing))
        Card(
            modifier = Modifier.fillMaxWidth()
                .appContainer(hazeState = hazeState, cornerRadius = 24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Transparent),
            shape = RoundedCornerShape(Spacing.Card.borderRadius),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(modifier = Modifier.padding(Spacing.lg)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Info, contentDescription = "Info",
                        tint = NeonTeal, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(Spacing.md))
                    Text(text = "How it works", style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary, fontWeight = FontWeight.SemiBold)
                }
                Spacer(modifier = Modifier.height(Spacing.md))
                InfoItem("✓ Checks against known phishing databases")
                InfoItem("✓ Analyzes URL patterns and keywords")
                InfoItem("✓ Provides instant threat assessment")
                InfoItem("✓ 100% private - all checks are local")
            }
        }
    }

    // ── Result dialog ──────────────────────────────────────────────────────────
    scanResult?.let { result ->
        val alertData = buildAlertData(
            dnsCheckFailed = result.dnsCheckFailed,
            result         = result.result,
            score          = result.score,
            description    = result.description
        )

        val onDone: () -> Unit = {
            viewModel.dismissAlert()
            urlInput = ""
            navController.navigate("dashboard") {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState    = true
            }
        }

        // ✅ NEW signature — no 'reason' parameter anywhere
        ThreatAlertDialog(
            url           = result.url,
            threatLevel   = result.result,
            score         = result.score,
            detectionTag  = alertData.detectionTag,
            reasonBullets = alertData.reasonBullets,
            onBlock       = { onDone() },
            onIgnore      = { onDone() },
            onContinue    = { onDone() },
            onDismiss     = { onDone() }
        )
    }
}

// ==================== SCANNING OVERLAY ====================
@Composable
private fun ScanningOverlay(url: String) {
    var messageIndex by remember { mutableStateOf(0) }
    var messageAlpha by remember { mutableStateOf(1f) }
    val animatedAlpha by animateFloatAsState(
        targetValue = messageAlpha,
        animationSpec = tween(300, easing = EaseInOutSine),
        label = "msg_alpha"
    )
    LaunchedEffect(Unit) {
        while (true) {
            delay(800); messageAlpha = 0f
            delay(320); messageIndex = (messageIndex + 1) % SCAN_MESSAGES.size; messageAlpha = 1f
        }
    }
    val infiniteTransition = rememberInfiniteTransition(label = "scan")
    val arcRotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(900, easing = LinearEasing), RepeatMode.Restart),
        label = "arc_rotation")
    val ring1 by infiniteTransition.animateFloat(initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2000, easing = EaseOutCubic), RepeatMode.Restart),
        label = "ring1")
    val ring2 by infiniteTransition.animateFloat(initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2000, delayMillis = 600, easing = EaseOutCubic), RepeatMode.Restart),
        label = "ring2")
    val ring3 by infiniteTransition.animateFloat(initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2000, delayMillis = 1200, easing = EaseOutCubic), RepeatMode.Restart),
        label = "ring3")
    val shieldScale by infiniteTransition.animateFloat(
        initialValue = 0.94f, targetValue = 1.06f,
        animationSpec = infiniteRepeatable(tween(1000, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "shield_scale")
    val shieldAlpha by infiniteTransition.animateFloat(
        initialValue = 0.7f, targetValue = 1.0f,
        animationSpec = infiniteRepeatable(tween(1000, easing = EaseInOutSine), RepeatMode.Reverse),
        label = "shield_alpha")

    Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0F0F10)),
        contentAlignment = Alignment.Center) {
        RadarRing(progress = ring1, maxRadius = 200f, color = NeonTeal)
        RadarRing(progress = ring2, maxRadius = 200f, color = NeonTeal)
        RadarRing(progress = ring3, maxRadius = 200f, color = NeonTeal)
        Canvas(modifier = Modifier.size(180.dp)) {
            val sw = 5.dp.toPx(); val inset = sw / 2f; val arcSize = size.width - sw
            drawArc(color = NeonTeal.copy(alpha = 0.12f), startAngle = 0f, sweepAngle = 360f,
                useCenter = false, topLeft = Offset(inset, inset), size = Size(arcSize, arcSize),
                style = Stroke(width = sw, cap = StrokeCap.Round))
            drawArc(brush = Brush.sweepGradient(listOf(Color.Transparent, NeonTeal, CyberTeal)),
                startAngle = arcRotation, sweepAngle = 120f,
                useCenter = false, topLeft = Offset(inset, inset), size = Size(arcSize, arcSize),
                style = Stroke(width = sw, cap = StrokeCap.Round))
        }
        Box(modifier = Modifier.size((72 * shieldScale).dp)
            .background(Brush.radialGradient(listOf(NeonTeal.copy(alpha = 0.18f), Color.Transparent)), CircleShape),
            contentAlignment = Alignment.Center) {
            Surface(modifier = Modifier.size(64.dp), shape = CircleShape, color = Color.Transparent,
                border = androidx.compose.foundation.BorderStroke(1.5.dp, NeonTeal.copy(alpha = shieldAlpha * 0.8f))) {}
            Icon(imageVector = Icons.Default.Shield, contentDescription = null,
                tint = NeonTeal.copy(alpha = shieldAlpha), modifier = Modifier.size(36.dp))
        }
        Column(modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 120.dp),
            horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "Scanning", color = TextPrimary,
                fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, letterSpacing = 1.sp)
            Spacer(modifier = Modifier.height(6.dp))
            Text(text = url.take(42) + if (url.length > 42) "…" else "",
                color = NeonTeal.copy(alpha = 0.7f), style = MaterialTheme.typography.labelSmall,
                textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 32.dp))
            Spacer(modifier = Modifier.height(20.dp))
            Text(text = SCAN_MESSAGES[messageIndex], color = TextSecondary,
                style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center,
                modifier = Modifier.alpha(animatedAlpha))
            Spacer(modifier = Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(3) { i ->
                    val dotAlpha by infiniteTransition.animateFloat(
                        initialValue = 0.2f, targetValue = 1f,
                        animationSpec = infiniteRepeatable(
                            tween(600, delayMillis = i * 200, easing = EaseInOutSine), RepeatMode.Reverse),
                        label = "dot_$i")
                    Box(modifier = Modifier.size(7.dp).alpha(dotAlpha)
                        .background(NeonTeal, CircleShape))
                }
            }
        }
    }
}

@Composable
private fun RadarRing(progress: Float, maxRadius: Float, color: Color) {
    val radius = progress * maxRadius
    val alpha = (1f - progress).coerceIn(0f, 1f)
    Canvas(modifier = Modifier.fillMaxSize()) {
        drawCircle(color = color.copy(alpha = alpha * 0.35f),
            radius = radius.dp.toPx(), center = Offset(size.width / 2f, size.height / 2f),
            style = Stroke(width = 2.dp.toPx()))
    }
}

@Composable
fun InfoItem(text: String) {
    Row(modifier = Modifier.padding(vertical = 4.dp)) {
        Text(text = text, style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary, lineHeight = 20.sp)
    }
}