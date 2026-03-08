package com.zerothreat.core.browser

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Browser
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.zerothreat.core.R
import com.zerothreat.core.data.AppPreferences
import com.zerothreat.core.data.db.AppDatabase
import com.zerothreat.core.data.repository.UrlRepository
import com.zerothreat.core.detector.DnsChecker
import com.zerothreat.core.detector.DnsCheckResult
import com.zerothreat.core.detector.PhishingDetector
import com.zerothreat.core.detector.PhishingResult
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import java.net.URI
import kotlin.concurrent.thread
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking

class SafeBrowserActivity : AppCompatActivity() {

    private val appPreferences by lazy { AppPreferences(this) }
    private val urlRepository by lazy { UrlRepository(AppDatabase.getDatabase(this).urlDao()) }
    private val dnsChecker = DnsChecker()
    private var scanningDialog: AlertDialog? = null

    private data class BrowserOption(
        val packageName: String,
        val activityName: String,
        val label: String
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val incoming = intent.dataString
        if (incoming.isNullOrBlank()) {
            finish()
            return
        }

        handleUrl(incoming)
    }

    override fun onDestroy() {
        hideScanningDialog()
        super.onDestroy()
    }

    private fun handleUrl(rawUrl: String) {
        val url = if (rawUrl.startsWith("http://")) {
            rawUrl.replace("http://", "https://")
        } else if (!rawUrl.startsWith("https://")) {
            "https://$rawUrl"
        } else {
            rawUrl
        }

        val domain = try {
            Uri.parse(url).host ?: ""
        } catch (_: Exception) {
            ""
        }

        if (domain.isBlank()) {
            openInExternalBrowser(url)
            finish()
            return
        }

        val sourcePackage = resolveSourcePackage(intent)
        val normalizedBlockedUrl = normalizeBlockedUrl(url)

        if (isBlockedUrl(normalizedBlockedUrl)) {
            Toast.makeText(this, "This URL is blocked by ZeroThreat", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        if (!appPreferences.smartModeEnabled) {
            openInExternalBrowser(url)
            finish()
            return
        }

        // Check if onClick is enabled for this source app
        if (sourcePackage != null && !isOnClickEnabledForApp(sourcePackage)) {
            Log.d("CLICK_SCAN", "onClick disabled for $sourcePackage, bypassing scan")
            openInExternalBrowser(url)
            finish()
            return
        }

        val analysisSource = sourcePackage
            ?.takeIf { enabledSourcePackages().contains(it) }
            ?: "click_scan"

        Log.d(
            "CLICK_SCAN",
            "url=$url source=${sourcePackage ?: "none"} analysisSource=$analysisSource"
        )

        showScanningDialog(domain)

        thread(name = "zt-click-scan") {
            try {
                val dnsResult = runBlocking {
                    dnsChecker.checkUrlExists(url)
                }

                if (!dnsResult.exists) {
                    Log.w("CLICK_SCAN", "Domain does not exist (DNS failed): $url - ${dnsResult.message}")
                    logDnsFailureScan(url, domain, analysisSource, dnsResult)
                    runOnUiThread {
                        if (isFinishing || isDestroyed) return@runOnUiThread
                        hideScanningDialog()
                        showDnsFailureDialog(url, domain, dnsResult, analysisSource)
                    }
                    return@thread
                }

                val report = PhishingDetector.analyzeDetailed(this, url, source = analysisSource)

                runBlocking(Dispatchers.IO) {
                    try {
                        urlRepository.logScan(
                            url = url,
                            domain = domain.lowercase(),
                            result = report.result,
                            source = analysisSource,
                            phishingScore = report.score,
                            analysisNote = report.description
                        )
                    } catch (e: Exception) {
                        Log.e("CLICK_SCAN", "Failed to save click scan result", e)
                    }
                }

                runOnUiThread {
                    if (isFinishing || isDestroyed) return@runOnUiThread
                    hideScanningDialog()
                    when (report.result) {
                        PhishingResult.SAFE -> showSafeBrowserChoiceDialog(url, domain, report, analysisSource)
                        PhishingResult.SUSPICIOUS,
                        PhishingResult.PHISHING -> showThreatDialog(url, domain, report, analysisSource)
                    }
                }
            } catch (e: Exception) {
                Log.e("CLICK_SCAN", "Unexpected scan failure", e)
                runBlocking(Dispatchers.IO) {
                    try {
                        urlRepository.logScan(
                            url = url,
                            domain = domain.lowercase(),
                            result = PhishingResult.SUSPICIOUS,
                            source = analysisSource,
                            threatType = "Scan error",
                            phishingScore = 50,
                            analysisNote = e.message ?: "Scan failed unexpectedly"
                        )
                    } catch (_: Exception) {
                    }
                }
                runOnUiThread {
                    if (isFinishing || isDestroyed) return@runOnUiThread
                    hideScanningDialog()
                    Toast.makeText(
                        this,
                        "Unable to complete scan. Please try again.",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
            }
        }
    }

    private fun showScanningDialog(domain: String) {
        hideScanningDialog()

        val content = layoutInflater.inflate(R.layout.dialog_scanning, null)
        content.findViewById<TextView>(R.id.scanningTitle).text =
            "Scanning link for suspicious activity"
        content.findViewById<TextView>(R.id.scanningDomain).text = domain
        content.findViewById<TextView>(R.id.scanningHint).text =
            "Checking DNS, phishing patterns, and threat signals..."

        scanningDialog = MaterialAlertDialogBuilder(this, R.style.Theme_ZeroThreatCore_Dialog)
            .setView(content)
            .setCancelable(false)
            .create()
            .also { it.show() }
    }

    private fun hideScanningDialog() {
        scanningDialog?.dismiss()
        scanningDialog = null
    }

    private fun showSafeBrowserChoiceDialog(
        url: String,
        domain: String,
        report: PhishingDetector.AnalysisReport,
        source: String
    ) {
        val safetyPercentage = 100 - report.score

        val dialogBuilder = MaterialAlertDialogBuilder(this, R.style.Theme_ZeroThreatCore_Dialog)
            .setTitle("Scan Complete: Safe")
            .setMessage(
                "Domain: $domain\n\nVerdict: $safetyPercentage% Safe\nRisk Level: Low\n\nNo suspicious phishing patterns were detected."
            )
            .setPositiveButton("Open Link") { _, _ ->
                showBrowserChoiceDialog(url)
            }
            .setNeutralButton("Exit") { _, _ -> finish() }
            .setOnCancelListener { finish() }

        if (safetyPercentage < 100) {
            dialogBuilder.setNegativeButton("Block") { _, _ ->
                blockUrlAndFinish(url, domain, report, source)
            }
        }

        dialogBuilder.show()
    }

    private fun showDnsFailureDialog(
        url: String,
        domain: String,
        dnsResult: DnsCheckResult,
        source: String
    ) {
        MaterialAlertDialogBuilder(this, R.style.Theme_ZeroThreatCore_Dialog)
            .setTitle("Scan Result: Domain Not Found")
            .setMessage(
                "Domain: $domain\n\n${dnsResult.message}\n\nThis domain could not be resolved. It may be a typo or an unsafe destination."
            )
            .setPositiveButton("Continue Anyway") { _, _ ->
                showBrowserChoiceDialog(url)
            }
            .setNegativeButton("Block") { _, _ ->
                blockUrlDueToNonExistence(url, domain, dnsResult, source)
            }
            .setNeutralButton("Exit") { _, _ -> finish() }
            .setOnCancelListener { finish() }
            .show()
    }

    private fun logDnsFailureScan(
        url: String,
        domain: String,
        source: String,
        dnsResult: DnsCheckResult
    ) {
        runBlocking(Dispatchers.IO) {
            try {
                urlRepository.logScan(
                    url = url,
                    domain = domain.lowercase(),
                    result = PhishingResult.SUSPICIOUS,
                    source = source,
                    threatType = "Non-existent domain",
                    phishingScore = dnsResult.score,
                    analysisNote = dnsResult.message
                )
            } catch (e: Exception) {
                Log.e("CLICK_SCAN", "Failed to save DNS-failed click scan", e)
            }
        }
    }

    private fun blockUrlDueToNonExistence(
        url: String,
        domain: String,
        dnsResult: DnsCheckResult,
        source: String
    ) {
        val normalizedUrl = normalizeBlockedUrl(url)
        runBlocking(Dispatchers.IO) {
            urlRepository.blockUrl(
                url = normalizedUrl,
                domain = domain.lowercase(),
                result = PhishingResult.SUSPICIOUS,
                source = source,
                phishingScore = dnsResult.score,
                analysisNote = dnsResult.message
            )
        }
        Toast.makeText(this, "Domain blocked - ${dnsResult.message}", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun showBrowserChoiceDialog(url: String) {
        val availableBrowsers = getAvailableBrowsers(url)
        if (availableBrowsers.isEmpty()) {
            openInExternalBrowser(url)
            finish()
            return
        }

        MaterialAlertDialogBuilder(this, R.style.Theme_ZeroThreatCore_Dialog)
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

    private fun showThreatDialog(
        url: String,
        domain: String,
        report: PhishingDetector.AnalysisReport,
        source: String
    ) {
        val status = when (report.result) {
            PhishingResult.SUSPICIOUS -> "Suspicious"
            PhishingResult.PHISHING -> "Phishing Detected"
            PhishingResult.SAFE -> "Safe"
        }

        MaterialAlertDialogBuilder(this, R.style.Theme_ZeroThreatCore_Dialog)
            .setTitle("Threat Detected")
            .setMessage(
                "Domain: $domain\n\nVerdict: ${report.score}% $status\n\nReason:\n${report.description}"
            )
            .setPositiveButton("Continue Anyway") { _, _ ->
                showBrowserChoiceDialog(url)
            }
            .setNegativeButton("Block") { _, _ ->
                blockUrlAndFinish(url, domain, report, source)
            }
            .setNeutralButton("Exit") { _, _ -> finish() }
            .setOnCancelListener { finish() }
            .show()
    }

    private fun blockUrlAndFinish(
        url: String,
        domain: String,
        report: PhishingDetector.AnalysisReport,
        source: String
    ) {
        val normalizedUrl = normalizeBlockedUrl(url)
        runBlocking(Dispatchers.IO) {
            urlRepository.blockUrl(
                url = normalizedUrl,
                domain = domain.lowercase(),
                result = report.result,
                source = source,
                phishingScore = report.score,
                analysisNote = report.description
            )
        }
        Toast.makeText(this, "Link blocked", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun isBlockedUrl(url: String): Boolean {
        return runBlocking(Dispatchers.IO) {
            urlRepository.isUrlBlocked(url)
        }
    }

    private fun normalizeBlockedUrl(rawUrl: String): String {
        val raw = rawUrl.trim()
        if (raw.isBlank()) return raw

        return try {
            val withScheme = if (
                raw.startsWith("http://", ignoreCase = true) ||
                raw.startsWith("https://", ignoreCase = true)
            ) {
                raw
            } else {
                "https://$raw"
            }

            val uri = URI(withScheme)
            val host = uri.host?.lowercase()?.removePrefix("www.") ?: return raw.lowercase()
            val path = (uri.rawPath ?: "").ifBlank { "/" }
            val query = uri.rawQuery?.let { "?$it" } ?: ""
            val normalized = "https://$host$path$query"
            if (path == "/" && query.isEmpty()) "https://$host" else normalized
        } catch (_: Exception) {
            raw.lowercase()
        }
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
                val label = resolveInfo.loadLabel(packageManager)
                    ?.toString()
                    ?.takeIf { it.isNotBlank() }
                    ?: activityInfo.packageName
                BrowserOption(
                    packageName = activityInfo.packageName,
                    activityName = activityInfo.name,
                    label = label
                )
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
        try {
            startActivity(explicitIntent)
            return
        } catch (_: Exception) {
            // no-op
        }

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
            .filter { it != packageName }
            .distinct()

        val preferredPackage = appPreferences.preferredBrowserPackage
            ?.takeIf { it in availableBrowserPackages }
        val systemDefaultPackage = resolveExternalDefaultBrowserPackage(this)
            ?.takeIf { it in availableBrowserPackages }
        val rememberedDefaultPackage = appPreferences.lastKnownDefaultBrowserPackage
            ?.takeIf { it in availableBrowserPackages }
        val targetPackage = preferredPackage
            ?: systemDefaultPackage
            ?: rememberedDefaultPackage
            ?: availableBrowserPackages.firstOrNull()

        if (targetPackage != null) {
            val launchIntent = Intent(viewIntent).apply {
                `package` = targetPackage
            }
            try {
                startActivity(launchIntent)
                return
            } catch (_: Exception) {
                // no-op
            }
        }

        try {
            val chooserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addCategory(Intent.CATEGORY_BROWSABLE)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            startActivity(
                Intent.createChooser(chooserIntent, "Open link with").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        } catch (_: Exception) {
            // no-op
        }
    }

    private fun enabledSourcePackages(): Set<String> {
        val packages = mutableSetOf<String>()
        if (appPreferences.monitorWhatsapp) {
            packages.add("com.whatsapp")
            packages.add("com.whatsapp.w4b")
        }
        if (appPreferences.monitorInstagram) packages.add("com.instagram.android")
        if (appPreferences.monitorGmail) packages.add("com.google.android.gm")
        if (appPreferences.monitorTelegram) {
            packages.add("org.telegram.messenger")
            packages.add("org.telegram.messenger.web")
        }
        if (appPreferences.monitorMessages) {
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
        if (!referrerName.isNullOrBlank()) {
            parseReferrerPackage(Uri.parse(referrerName))?.let { return it }
        }

        parseReferrerPackage(referrer)?.let { return it }
        return null
    }

    private fun parseReferrerPackage(uri: Uri?): String? {
        if (uri == null) return null
        return when (uri.scheme) {
            "android-app" -> uri.host
            "package" -> uri.schemeSpecificPart
            else -> null
        }
    }

    private fun isOnClickEnabledForApp(packageName: String): Boolean {
        return when (packageName) {
            "com.whatsapp" -> appPreferences.whatsappOnClickEnabled
            "com.instagram.android" -> appPreferences.instagramOnClickEnabled
            "com.google.android.gm" -> appPreferences.gmailOnClickEnabled
            "org.telegram.messenger" -> appPreferences.telegramOnClickEnabled
            "com.google.android.apps.messaging", "com.android.mms" -> appPreferences.messagesOnClickEnabled
            else -> true // Default to enabled for other apps
        }
    }
}
