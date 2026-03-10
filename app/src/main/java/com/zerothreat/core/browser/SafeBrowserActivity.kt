package com.zerothreat.core.browser

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Browser
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shield
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.zerothreat.core.data.AppPreferences
import com.zerothreat.core.data.db.AppDatabase
import com.zerothreat.core.data.repository.UrlRepository
import com.zerothreat.core.detector.DnsCheckResult
import com.zerothreat.core.detector.DnsChecker
import com.zerothreat.core.detector.PhishingDetector
import com.zerothreat.core.detector.PhishingResult
import com.zerothreat.core.ui.alerts.ThreatAlertDialog
import com.zerothreat.core.ui.theme.*
import java.net.URI
import kotlin.concurrent.thread
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.runBlocking

// ── Scan state passed to UI ───────────────────────────────────────────────────
private sealed class ScanState {
    object Scanning : ScanState()
    data class Safe(
        val url: String,
        val domain: String,
        val report: PhishingDetector.AnalysisReport
    ) : ScanState()
    data class Threat(
        val url: String,
        val domain: String,
        val report: PhishingDetector.AnalysisReport
    ) : ScanState()
    data class DnsFailed(
        val url: String,
        val domain: String,
        val dnsResult: DnsCheckResult,
        val source: String
    ) : ScanState()
    object ScanError : ScanState()
}

private val SCAN_MESSAGES = listOf(
    "Resolving domain...",
    "Checking blacklists...",
    "Analysing URL patterns...",
    "Inspecting keywords...",
    "Calculating threat score...",
    "Finalising report..."
)

// ── Helper: turn a raw description string into bullet points ──────────────────
private fun bulletsFromDescription(description: String): List<String> {
    if (description.isBlank()) return emptyList()
    return description
        .split(Regex("[\\n;]"))
        .map { it.trim().trimStart('-', '•', '*').trim() }
        .filter { it.isNotBlank() }
        .take(3)
}

class SafeBrowserActivity : AppCompatActivity() {

    private val appPreferences by lazy { AppPreferences(this) }
    private val urlRepository  by lazy { UrlRepository(AppDatabase.getDatabase(this).urlDao()) }
    private val dnsChecker = DnsChecker()

    private val _scanState = MutableStateFlow<ScanState>(ScanState.Scanning)
    private val scanState  = _scanState.asStateFlow()

    private var pendingUrl    = ""
    private var pendingSource = "click_scan"

    private data class BrowserOption(
        val packageName: String,
        val activityName: String,
        val label: String
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val incoming = intent.dataString
        if (incoming.isNullOrBlank()) { finish(); return }

        setContent {
            ZeroThreatScanScreen(
                scanState    = scanState.collectAsState().value,
                onOpenLink   = { url -> showBrowserChoiceDialog(url) },
                onBlock      = { url, domain, report, source -> blockUrlAndFinish(url, domain, report, source) },
                onBlockDns   = { url, domain, dnsResult, source -> blockUrlDueToNonExistence(url, domain, dnsResult, source) },
                onOpenAnyway = { url -> showBrowserChoiceDialog(url) },
                onExit       = { finish() }
            )
        }

        handleUrl(incoming)
    }

    // ── URL handling ──────────────────────────────────────────────────────────

    private fun handleUrl(rawUrl: String) {
        val url = when {
            rawUrl.startsWith("http://")   -> rawUrl.replace("http://", "https://")
            !rawUrl.startsWith("https://") -> "https://$rawUrl"
            else                           -> rawUrl
        }

        val domain = try { Uri.parse(url).host ?: "" } catch (_: Exception) { "" }

        if (domain.isBlank()) { openInExternalBrowser(url); finish(); return }

        val sourcePackage        = resolveSourcePackage(intent)
        val normalizedBlockedUrl = normalizeBlockedUrl(url)

        if (isBlockedUrl(normalizedBlockedUrl)) {
            Toast.makeText(this, "This URL is blocked by ZeroThreat", Toast.LENGTH_LONG).show()
            finish(); return
        }

        if (!appPreferences.smartModeEnabled) { openInExternalBrowser(url); finish(); return }

        if (sourcePackage != null && !isOnClickEnabledForApp(sourcePackage)) {
            Log.d("CLICK_SCAN", "onClick disabled for $sourcePackage, bypassing scan")
            openInExternalBrowser(url); finish(); return
        }

        val analysisSource = sourcePackage
            ?.takeIf { enabledSourcePackages().contains(it) }
            ?: "click_scan"

        pendingUrl    = url
        pendingSource = analysisSource

        Log.d("CLICK_SCAN", "url=$url source=${sourcePackage ?: "none"} analysisSource=$analysisSource")

        thread(name = "zt-click-scan") {
            try {
                val dnsResult = runBlocking { dnsChecker.checkUrlExists(url) }

                if (!dnsResult.exists) {
                    Log.w("CLICK_SCAN", "DNS failed: $url – ${dnsResult.message}")
                    logDnsFailureScan(url, domain, analysisSource, dnsResult)
                    runOnUiThread {
                        if (isFinishing || isDestroyed) return@runOnUiThread
                        _scanState.value = ScanState.DnsFailed(url, domain, dnsResult, analysisSource)
                    }
                    return@thread
                }

                val report = PhishingDetector.analyzeDetailed(this, url, source = analysisSource)

                runBlocking(Dispatchers.IO) {
                    try {
                        urlRepository.logScan(
                            url          = url,
                            domain       = domain.lowercase(),
                            result       = report.result,
                            source       = analysisSource,
                            phishingScore = report.score,
                            analysisNote = report.description
                        )
                    } catch (e: Exception) {
                        Log.e("CLICK_SCAN", "Failed to save click scan result", e)
                    }
                }

                runOnUiThread {
                    if (isFinishing || isDestroyed) return@runOnUiThread
                    _scanState.value = when (report.result) {
                        PhishingResult.SAFE                          -> ScanState.Safe(url, domain, report)
                        PhishingResult.SUSPICIOUS, PhishingResult.PHISHING -> ScanState.Threat(url, domain, report)
                    }
                }

            } catch (e: Exception) {
                Log.e("CLICK_SCAN", "Unexpected scan failure", e)
                runBlocking(Dispatchers.IO) {
                    try {
                        urlRepository.logScan(
                            url          = url,
                            domain       = domain.lowercase(),
                            result       = PhishingResult.SUSPICIOUS,
                            source       = analysisSource,
                            threatType   = "Scan error",
                            phishingScore = 50,
                            analysisNote = e.message ?: "Scan failed unexpectedly"
                        )
                    } catch (_: Exception) {}
                }
                runOnUiThread {
                    if (isFinishing || isDestroyed) return@runOnUiThread
                    _scanState.value = ScanState.ScanError
                }
            }
        }
    }

    // ── Browser / block helpers ───────────────────────────────────────────────

    private fun showBrowserChoiceDialog(url: String) {
        val availableBrowsers = getAvailableBrowsers(url)
        if (availableBrowsers.isEmpty()) { openInExternalBrowser(url); finish(); return }
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Select Browser")
            .setMessage("Which browser would you like to open this link with?")
            .setItems(availableBrowsers.map { it.label }.toTypedArray()) { _, which ->
                launchInSelectedBrowser(url, availableBrowsers[which])
                finish()
            }
            .setNegativeButton("Cancel") { _, _ -> finish() }
            .setOnCancelListener { finish() }
            .show()
    }

    private fun blockUrlAndFinish(
        url: String, domain: String,
        report: PhishingDetector.AnalysisReport, source: String
    ) {
        val normalizedUrl = normalizeBlockedUrl(url)
        runBlocking(Dispatchers.IO) {
            urlRepository.blockUrl(
                url = normalizedUrl, domain = domain.lowercase(),
                result = report.result, source = source,
                phishingScore = report.score, analysisNote = report.description
            )
        }
        Toast.makeText(this, "Link blocked", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun blockUrlDueToNonExistence(
        url: String, domain: String,
        dnsResult: DnsCheckResult, source: String
    ) {
        val normalizedUrl = normalizeBlockedUrl(url)
        runBlocking(Dispatchers.IO) {
            urlRepository.blockUrl(
                url = normalizedUrl, domain = domain.lowercase(),
                result = PhishingResult.SUSPICIOUS, source = source,
                phishingScore = dnsResult.score, analysisNote = dnsResult.message
            )
        }
        Toast.makeText(this, "Domain blocked - ${dnsResult.message}", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun isBlockedUrl(url: String): Boolean =
        runBlocking(Dispatchers.IO) { urlRepository.isUrlBlocked(url) }

    private fun logDnsFailureScan(
        url: String, domain: String, source: String, dnsResult: DnsCheckResult
    ) {
        runBlocking(Dispatchers.IO) {
            try {
                urlRepository.logScan(
                    url = url, domain = domain.lowercase(),
                    result = PhishingResult.SUSPICIOUS, source = source,
                    threatType = "Non-existent domain",
                    phishingScore = dnsResult.score, analysisNote = dnsResult.message
                )
            } catch (e: Exception) {
                Log.e("CLICK_SCAN", "Failed to save DNS-failed click scan", e)
            }
        }
    }

    private fun normalizeBlockedUrl(rawUrl: String): String {
        val raw = rawUrl.trim()
        if (raw.isBlank()) return raw
        return try {
            val withScheme = if (
                raw.startsWith("http://", ignoreCase = true) ||
                raw.startsWith("https://", ignoreCase = true)
            ) raw else "https://$raw"
            val uri   = URI(withScheme)
            val host  = uri.host?.lowercase()?.removePrefix("www.") ?: return raw.lowercase()
            val path  = (uri.rawPath ?: "").ifBlank { "/" }
            val query = uri.rawQuery?.let { "?$it" } ?: ""
            val normalized = "https://$host$path$query"
            if (path == "/" && query.isEmpty()) "https://$host" else normalized
        } catch (_: Exception) { raw.lowercase() }
    }

    private fun getAvailableBrowsers(url: String): List<BrowserOption> {
        val viewIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
        }
        return packageManager
            .queryIntentActivities(viewIntent, PackageManager.MATCH_DEFAULT_ONLY)
            .mapNotNull { resolveInfo ->
                val activityInfo = resolveInfo.activityInfo ?: return@mapNotNull null
                if (activityInfo.packageName == packageName) return@mapNotNull null
                val label = resolveInfo.loadLabel(packageManager)?.toString()
                    ?.takeIf { it.isNotBlank() } ?: activityInfo.packageName
                BrowserOption(activityInfo.packageName, activityInfo.name, label)
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }

    private fun launchInSelectedBrowser(url: String, browser: BrowserOption) {
        val explicitIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            setClassName(browser.packageName, browser.activityName)
        }
        try { startActivity(explicitIntent); return } catch (_: Exception) {}
        openInExternalBrowser(url)
    }

    private fun openInExternalBrowser(url: String) {
        val viewIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addCategory(Intent.CATEGORY_BROWSABLE)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val availableBrowserPackages = packageManager
            .queryIntentActivities(viewIntent, PackageManager.MATCH_ALL)
            .mapNotNull { it.activityInfo?.packageName }
            .filter { it != packageName }.distinct()

        val targetPackage =
            appPreferences.preferredBrowserPackage?.takeIf { it in availableBrowserPackages }
                ?: resolveExternalDefaultBrowserPackage(this)?.takeIf { it in availableBrowserPackages }
                ?: appPreferences.lastKnownDefaultBrowserPackage?.takeIf { it in availableBrowserPackages }
                ?: availableBrowserPackages.firstOrNull()

        if (targetPackage != null) {
            try { startActivity(Intent(viewIntent).apply { `package` = targetPackage }); return }
            catch (_: Exception) {}
        }
        try {
            startActivity(
                Intent.createChooser(
                    Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                        addCategory(Intent.CATEGORY_BROWSABLE)
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }, "Open link with"
                ).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            )
        } catch (_: Exception) {}
    }

    private fun enabledSourcePackages(): Set<String> {
        val packages = mutableSetOf<String>()
        if (appPreferences.monitorWhatsapp)  { packages.add("com.whatsapp"); packages.add("com.whatsapp.w4b") }
        if (appPreferences.monitorInstagram)   packages.add("com.instagram.android")
        if (appPreferences.monitorGmail)       packages.add("com.google.android.gm")
        if (appPreferences.monitorTelegram)  { packages.add("org.telegram.messenger"); packages.add("org.telegram.messenger.web") }
        if (appPreferences.monitorMessages)  {
            packages.add("com.google.android.apps.messaging")
            packages.add("com.android.mms")
            packages.add("com.samsung.android.messaging")
        }
        return packages
    }

    private fun resolveSourcePackage(intent: Intent): String? {
        val appId = intent.getStringExtra(Browser.EXTRA_APPLICATION_ID)
        if (!appId.isNullOrBlank()) return appId
        val referrerUri = intent.getParcelableExtra<Uri>(Intent.EXTRA_REFERRER)
        parseReferrerPackage(referrerUri)?.let { return it }
        val referrerName = intent.getStringExtra(Intent.EXTRA_REFERRER_NAME)
        if (!referrerName.isNullOrBlank()) parseReferrerPackage(Uri.parse(referrerName))?.let { return it }
        parseReferrerPackage(referrer)?.let { return it }
        return null
    }

    private fun parseReferrerPackage(uri: Uri?): String? {
        if (uri == null) return null
        return when (uri.scheme) {
            "android-app" -> uri.host
            "package"     -> uri.schemeSpecificPart
            else          -> null
        }
    }

    private fun isOnClickEnabledForApp(packageName: String): Boolean = when (packageName) {
        "com.whatsapp"                        -> appPreferences.whatsappOnClickEnabled
        "com.instagram.android"               -> appPreferences.instagramOnClickEnabled
        "com.google.android.gm"               -> appPreferences.gmailOnClickEnabled
        "org.telegram.messenger"              -> appPreferences.telegramOnClickEnabled
        "com.google.android.apps.messaging",
        "com.android.mms"                     -> appPreferences.messagesOnClickEnabled
        else                                  -> true
    }
}

// ==================== COMPOSE UI =============================================

@Composable
private fun ZeroThreatScanScreen(
    scanState: ScanState,
    onOpenLink: (String) -> Unit,
    onBlock: (String, String, PhishingDetector.AnalysisReport, String) -> Unit,
    onBlockDns: (String, String, DnsCheckResult, String) -> Unit,
    onOpenAnyway: (String) -> Unit,
    onExit: () -> Unit
) {
    MaterialTheme {
        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF0F0F10))) {

            val urlForDisplay = when (scanState) {
                is ScanState.Safe      -> scanState.url
                is ScanState.Threat    -> scanState.url
                is ScanState.DnsFailed -> scanState.url
                else                   -> ""
            }
            ScanningOverlay(url = urlForDisplay)

            when (scanState) {

                // ── Safe ──────────────────────────────────────────────────────
                is ScanState.Safe -> {
                    ThreatAlertDialog(
                        url          = scanState.url,
                        threatLevel  = PhishingResult.SAFE,
                        score        = scanState.report.score,
                        detectionTag = "No threats found",
                        reasonBullets = listOf(
                            "No known phishing patterns detected",
                            "Domain appears legitimate"
                        ),
                        onBlock    = { onOpenLink(scanState.url) },   // "OK" → open
                        onIgnore   = { onExit() },
                        onContinue = { onOpenLink(scanState.url) },
                        onDismiss  = { onExit() }
                    )
                }

                // ── Threat (Suspicious or Phishing) ───────────────────────────
                is ScanState.Threat -> {
                    val detectionTag = when (scanState.report.result) {
                        PhishingResult.PHISHING   -> "Phishing pattern"
                        PhishingResult.SUSPICIOUS -> "Suspicious pattern"
                        else                      -> "Unknown"
                    }
                    val bullets = bulletsFromDescription(scanState.report.description).ifEmpty {
                        listOf("Risk score: ${scanState.report.score}%")
                    }
                    ThreatAlertDialog(
                        url          = scanState.url,
                        threatLevel  = scanState.report.result,
                        score        = scanState.report.score,
                        detectionTag = detectionTag,
                        reasonBullets = bullets,
                        onBlock    = { onBlock(scanState.url, scanState.domain, scanState.report, "click_scan") },
                        onIgnore   = { onExit() },
                        onContinue = { onOpenLink(scanState.url) },   // "Continue Anyway" → open
                        onDismiss  = { onExit() }
                    )
                }

                // ── DNS Failed ────────────────────────────────────────────────
                is ScanState.DnsFailed -> {
                    ThreatAlertDialog(
                        url          = scanState.url,
                        threatLevel  = PhishingResult.SUSPICIOUS,
                        score        = scanState.dnsResult.score,
                        detectionTag = "Domain not found",
                        reasonBullets = listOf(
                            "This domain could not be resolved",
                            "The link may be inactive or malicious"
                        ),
                        onBlock    = { onBlockDns(scanState.url, scanState.domain, scanState.dnsResult, scanState.source) },
                        onIgnore   = { onExit() },
                        onContinue = { onOpenAnyway(scanState.url) }, // "Continue Anyway" → open
                        onDismiss  = { onExit() }
                    )
                }

                // ── Scan Error ────────────────────────────────────────────────
                is ScanState.ScanError -> {
                    ThreatAlertDialog(
                        url          = "unknown",
                        threatLevel  = PhishingResult.SUSPICIOUS,
                        score        = 0,
                        detectionTag = "Scan error",
                        reasonBullets = listOf("Unable to complete the scan", "Please try again"),
                        onBlock    = { onExit() },
                        onIgnore   = { onExit() },
                        onContinue = { onExit() },
                        onDismiss  = { onExit() }
                    )
                }

                ScanState.Scanning -> { /* scanning overlay only, no dialog */ }
            }
        }
    }
}

// ==================== SCANNING OVERLAY =======================================

@Composable
private fun ScanningOverlay(url: String) {
    var messageIndex by remember { mutableStateOf(0) }
    var messageAlpha by remember { mutableStateOf(1f) }
    val animatedAlpha by animateFloatAsState(
        targetValue   = messageAlpha,
        animationSpec = tween(300, easing = EaseInOutSine),
        label         = "msg_alpha"
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
    val ring1 by infiniteTransition.animateFloat(0f, 1f,
        infiniteRepeatable(tween(2000, easing = EaseOutCubic), RepeatMode.Restart), "ring1")
    val ring2 by infiniteTransition.animateFloat(0f, 1f,
        infiniteRepeatable(tween(2000, delayMillis = 600, easing = EaseOutCubic), RepeatMode.Restart), "ring2")
    val ring3 by infiniteTransition.animateFloat(0f, 1f,
        infiniteRepeatable(tween(2000, delayMillis = 1200, easing = EaseOutCubic), RepeatMode.Restart), "ring3")
    val shieldScale by infiniteTransition.animateFloat(0.94f, 1.06f,
        infiniteRepeatable(tween(1000, easing = EaseInOutSine), RepeatMode.Reverse), "shield_scale")
    val shieldAlpha by infiniteTransition.animateFloat(0.7f, 1.0f,
        infiniteRepeatable(tween(1000, easing = EaseInOutSine), RepeatMode.Reverse), "shield_alpha")

    Box(
        modifier = Modifier.fillMaxSize().background(Color(0xFF0F0F10)),
        contentAlignment = Alignment.Center
    ) {
        RadarRing(ring1, 200f, NeonTeal)
        RadarRing(ring2, 200f, NeonTeal)
        RadarRing(ring3, 200f, NeonTeal)

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

        Box(
            modifier = Modifier
                .size((72 * shieldScale).dp)
                .background(Brush.radialGradient(listOf(NeonTeal.copy(alpha = 0.18f), Color.Transparent)), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Surface(Modifier.size(64.dp), CircleShape, Color.Transparent,
                border = androidx.compose.foundation.BorderStroke(1.5.dp, NeonTeal.copy(alpha = shieldAlpha * 0.8f))) {}
            Icon(Icons.Default.Shield, null, Modifier.size(36.dp), NeonTeal.copy(alpha = shieldAlpha))
        }

        Column(
            Modifier.align(Alignment.BottomCenter).padding(bottom = 120.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("Scanning", color = TextPrimary, fontWeight = FontWeight.ExtraBold, fontSize = 22.sp, letterSpacing = 1.sp)
            Spacer(Modifier.height(6.dp))
            if (url.isNotBlank()) {
                Text(
                    text = url.take(42) + if (url.length > 42) "…" else "",
                    color = NeonTeal.copy(alpha = 0.7f),
                    style = MaterialTheme.typography.labelSmall,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
            }
            Spacer(Modifier.height(20.dp))
            Text(SCAN_MESSAGES[messageIndex], color = TextSecondary,
                style = MaterialTheme.typography.bodySmall, textAlign = TextAlign.Center,
                modifier = Modifier.alpha(animatedAlpha))
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                repeat(3) { i ->
                    val dotAlpha by infiniteTransition.animateFloat(0.2f, 1f,
                        infiniteRepeatable(tween(600, delayMillis = i * 200, easing = EaseInOutSine), RepeatMode.Reverse),
                        "dot_$i")
                    Box(Modifier.size(7.dp).alpha(dotAlpha).background(NeonTeal, CircleShape))
                }
            }
        }
    }
}

@Composable
private fun RadarRing(progress: Float, maxRadius: Float, color: Color) {
    val radius = progress * maxRadius
    val alpha  = (1f - progress).coerceIn(0f, 1f)
    Canvas(Modifier.fillMaxSize()) {
        drawCircle(color = color.copy(alpha = alpha * 0.35f),
            radius = radius.dp.toPx(), center = Offset(size.width / 2f, size.height / 2f),
            style = Stroke(width = 2.dp.toPx()))
    }
}