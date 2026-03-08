// notification listener
package com.zerothreat.core.notifications


import android.content.pm.PackageManager
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import androidx.core.content.ContextCompat
import com.zerothreat.core.data.AppPreferences
import com.zerothreat.core.data.db.AppDatabase
import com.zerothreat.core.data.repository.UrlRepository
import com.zerothreat.core.detector.DnsChecker
import com.zerothreat.core.detector.PhishingDetector
import com.zerothreat.core.detector.PhishingResult
import java.util.regex.Pattern
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import android.annotation.SuppressLint

class NotificationListener : NotificationListenerService() {
    private lateinit var appPreferences: AppPreferences
    private val dnsChecker = DnsChecker()
    private val urlRepository by lazy { UrlRepository(AppDatabase.getDatabase(this).urlDao()) }

    // ORIGINAL BROAD PATTERN: Allows detected without http/www
    // Updated: Added (?<!@) to skip email addresses (no match if preceded by @)
    private val urlPattern = Pattern.compile(
        "(?<!@)((https?://)?(www\\.)?[\\w-]+(\\.[\\w-]+)+(/[\\w./?%&=-]*)?)",
        Pattern.CASE_INSENSITIVE
    )

    // Cache to prevent duplicate processing within short bursts
    private val processedLinks = java.util.concurrent.ConcurrentHashMap<String, Long>()

    override fun onCreate() {
        super.onCreate()
        appPreferences = AppPreferences(this)
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (!appPreferences.notificationMonitoring) return

        val allowedPackages = getEnabledSourcePackages()
        // Ignore notifications from own app or non-target apps
        if (sbn.packageName == packageName || !allowedPackages.contains(sbn.packageName)) {
            return
        }

        val extras = sbn.notification.extras
        val pkg = sbn.packageName
        val appName = try {
            val pm = packageManager
            pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
        } catch (e: PackageManager.NameNotFoundException) {
            pkg // Fallback to package name
        }


        // DEBUG: print all extras keys + values
        for (key in extras.keySet()) {
            val value = extras.get(key)
            Log.d("NOTIF_DEBUG", "pkg=$pkg | $key = $value")
        }

        val allTexts = mutableListOf<String>()
        extractText(extras, allTexts)


        if (allTexts.isEmpty()) return

        val combinedText = allTexts.joinToString(" ")

        val seenLinks = mutableSetOf<String>()

        val matcher = urlPattern.matcher(combinedText)
        while (matcher.find()) {
            val start = matcher.start()
            val end = matcher.end()
            if (isPartOfEmailToken(combinedText, start, end)) {
                continue
            }

            val matchedUrl = matcher.group()
            val normalizedUrl = normalizeUrlCandidate(matchedUrl) ?: continue
            val domain = PhishingDetector.normalizeInput(normalizedUrl)
            if (domain.isBlank() || PhishingDetector.isSystemDomain(domain)) continue

            // 🔒 Prevent duplicate logs for the same link in one notification.
            if (!seenLinks.add(normalizedUrl)) continue

            // 🕒 DEDUPLICATION CACHE (5 Seconds)
            // Fixes issue where Gmail updates notification multiple times
            val lastTime = processedLinks.getOrDefault(normalizedUrl, 0L)
            if (System.currentTimeMillis() - lastTime < 5000) {
                Log.d("NOTIF_TRACE", "Skipping duplicate analysis for: $normalizedUrl")
                continue
            }
            processedLinks[normalizedUrl] = System.currentTimeMillis()

            Log.d("NOTIF_TRACE", "Analyzing link: $normalizedUrl from $appName")

            // DNS Check FIRST - Check if URL domain exists
            val dnsResult = runBlocking(Dispatchers.IO) {
                dnsChecker.checkUrlExists(normalizedUrl)
            }
            Log.d("NOTIF_TRACE", "DNS Check: ${dnsResult.message}")

            // If DNS check failed and URL doesn't exist, mark as suspicious
            if (!dnsResult.exists) {
                Log.w("NOTIF_TRACE", "Domain does not exist (DNS failed): $normalizedUrl")
                val msg = "SUSPICIOUS LINK (Non-existent domain): $domain [${dnsResult.score}%]"
                Log.w("NOTIF", msg)

                runBlocking(Dispatchers.IO) {
                    try {
                        urlRepository.logScan(
                            url = normalizedUrl,
                            domain = domain,
                            result = PhishingResult.SUSPICIOUS,
                            source = pkg,
                            threatType = "Non-existent domain",
                            phishingScore = dnsResult.score,
                            analysisNote = dnsResult.message
                        )
                    } catch (e: Exception) {
                        Log.e("NOTIF_TRACE", "Failed to save DNS-failed notification scan", e)
                    }
                }

                val intent = android.content.Intent("com.zerothreat.core.LOG_EVENT")
                intent.putExtra("msg", "[NOTIF] $msg")
                intent.setPackage(packageName)
                sendBroadcast(intent)

                // Show threat notification for non-existent domains
                showThreatNotification(domain, PhishingResult.SUSPICIOUS, "Non-existent domain detected")
                continue
            }

            // Continue with phishing detection
            val report = PhishingDetector.analyzeDetailed(this, normalizedUrl, source = pkg)
            val result = report.result
            Log.d("NOTIF_TRACE", "Analysis Result for $normalizedUrl: $result (${report.score}%)")

            // Save scan result to database
            runBlocking(Dispatchers.IO) {
                try {
                    urlRepository.logScan(
                        url = normalizedUrl,
                        domain = domain,
                        result = result,
                        source = pkg,
                        phishingScore = report.score,
                        analysisNote = report.description
                    )
                } catch (e: Exception) {
                    Log.e("NOTIF_TRACE", "Failed to save notification scan", e)
                }
            }

            val msg = when (result) {
                PhishingResult.SAFE -> "SAFE LINK: $domain [${report.score}%]"
                PhishingResult.SUSPICIOUS -> "SUSPICIOUS LINK: $domain [${report.score}%]"
                PhishingResult.PHISHING -> "PHISHING LINK: $domain [${report.score}%]"
            }

            // Log to system log
            when (result) {
                PhishingResult.SAFE -> Log.d("NOTIF", msg)
                PhishingResult.SUSPICIOUS -> Log.w("NOTIF", msg)
                PhishingResult.PHISHING -> Log.e("NOTIF", msg)
            }

            // Send to UI
            val intent = android.content.Intent("com.zerothreat.core.LOG_EVENT")
            intent.putExtra("msg", "[NOTIF] $msg")
            intent.setPackage(packageName) // Restrict to own app
            sendBroadcast(intent)

            // ---- SHOW NOTIFICATION ALERT ----
            // Check if notification is enabled for this app
            if (isNotificationEnabledForApp(pkg)) {
                showEnhancedThreatNotification(normalizedUrl, domain, result, report.score, report.description)
            }
        }
        
        // Clean old cache entries occasionally
        if (processedLinks.size > 100) processedLinks.clear()

    }

    private fun isNotificationEnabledForApp(packageName: String): Boolean {
        return when (packageName) {
            "com.whatsapp" -> appPreferences.whatsappNotificationEnabled
            "com.instagram.android" -> appPreferences.instagramNotificationEnabled
            "com.google.android.gm" -> appPreferences.gmailNotificationEnabled
            "org.telegram.messenger" -> appPreferences.telegramNotificationEnabled
            "com.google.android.apps.messaging", "com.android.mms" -> appPreferences.messagesNotificationEnabled
            else -> true // Default to enabled for other apps
        }
    }

    @SuppressLint("MissingPermission")
    private fun showEnhancedThreatNotification(
        url: String,
        domain: String,
        result: PhishingResult,
        score: Int,
        description: String?
    ) {
        val channelId = "zerothreat_alerts"
        val notificationManager = getSystemService(android.app.NotificationManager::class.java)

        // Create Channel if needed
        val channel = android.app.NotificationChannel(
            channelId,
            "Threat Alerts",
            android.app.NotificationManager.IMPORTANCE_HIGH
        )
        notificationManager.createNotificationChannel(channel)

        val notificationId = url.hashCode()

        // Different notifications based on threat level
        when (result) {
            PhishingResult.SAFE -> {
                // Safe URL - Show "Continue" and "Show in Chat"
                val title = "✅ Safe Link Detected"
                val safetyScore = 100 - score // Convert risk score to safety score
                val body = "$domain is safe (${safetyScore}% safe). You can continue."

                // Create Continue Action - Opens the URL in browser
                val continueIntent = android.content.Intent(this, NotificationActionReceiver::class.java).apply {
                    action = NotificationActionReceiver.ACTION_CONTINUE_URL
                    putExtra(NotificationActionReceiver.EXTRA_URL, url)
                    putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
                }
                val continuePendingIntent = android.app.PendingIntent.getBroadcast(
                    this, notificationId, continueIntent,
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                )

                // Create Show in Chat Action - Just dismisses notification
                val showInChatIntent = android.content.Intent(this, NotificationActionReceiver::class.java).apply {
                    action = NotificationActionReceiver.ACTION_SHOW_IN_CHAT
                    putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
                }
                val showInChatPendingIntent = android.app.PendingIntent.getBroadcast(
                    this, notificationId + 1, showInChatIntent,
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                )

                val notification = android.app.Notification.Builder(this, channelId)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle(title)
                    .setContentText(body)
                    .setAutoCancel(true)
                    .addAction(
                        android.app.Notification.Action.Builder(
                            android.graphics.drawable.Icon.createWithResource(this, android.R.drawable.ic_menu_view),
                            "Continue",
                            continuePendingIntent
                        ).build()
                    )
                    .addAction(
                        android.app.Notification.Action.Builder(
                            android.graphics.drawable.Icon.createWithResource(this, android.R.drawable.ic_menu_info_details),
                            "Show in Chat",
                            showInChatPendingIntent
                        ).build()
                    )
                    .build()

                postNotificationIfGranted(notificationManager, notificationId, notification)
            }

            PhishingResult.SUSPICIOUS, PhishingResult.PHISHING -> {
                // Suspicious/Phishing URL - Show "Show Link", "Block", "Continue"
                val title = if (result == PhishingResult.PHISHING) "⚠️ Phishing Detected" else "⚠️ Suspicious Link"
                val body = "$domain detected as ${result.name.lowercase()} ($score% unsafe). ${description ?: ""}"

                // Create Show Link Action - Shows the URL without opening
                val showLinkIntent = android.content.Intent(this, NotificationActionReceiver::class.java).apply {
                    action = NotificationActionReceiver.ACTION_SHOW_LINK
                    putExtra(NotificationActionReceiver.EXTRA_URL, url)
                    putExtra(NotificationActionReceiver.EXTRA_DOMAIN, domain)
                    putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
                }
                val showLinkPendingIntent = android.app.PendingIntent.getBroadcast(
                    this, notificationId, showLinkIntent,
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                )

                // Create Block Action - Blocks the URL
                val blockIntent = android.content.Intent(this, NotificationActionReceiver::class.java).apply {
                    action = NotificationActionReceiver.ACTION_BLOCK_URL
                    putExtra(NotificationActionReceiver.EXTRA_URL, url)
                    putExtra(NotificationActionReceiver.EXTRA_DOMAIN, domain)
                    putExtra(NotificationActionReceiver.EXTRA_RESULT, result.name)
                    putExtra(NotificationActionReceiver.EXTRA_SCORE, score)
                    putExtra(NotificationActionReceiver.EXTRA_DESCRIPTION, description)
                    putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
                }
                val blockPendingIntent = android.app.PendingIntent.getBroadcast(
                    this, notificationId + 1, blockIntent,
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                )

                // Create Continue Action - Opens URL in browser
                val continueIntent = android.content.Intent(this, NotificationActionReceiver::class.java).apply {
                    action = NotificationActionReceiver.ACTION_CONTINUE_URL
                    putExtra(NotificationActionReceiver.EXTRA_URL, url)
                    putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, notificationId)
                }
                val continuePendingIntent = android.app.PendingIntent.getBroadcast(
                    this, notificationId + 2, continueIntent,
                    android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
                )

                val notification = android.app.Notification.Builder(this, channelId)
                    .setSmallIcon(android.R.drawable.stat_sys_warning)
                    .setContentTitle(title)
                    .setContentText(body)
                    .setStyle(android.app.Notification.BigTextStyle().bigText(body))
                    .setAutoCancel(true)
                    .addAction(
                        android.app.Notification.Action.Builder(
                            android.graphics.drawable.Icon.createWithResource(this, android.R.drawable.ic_menu_info_details),
                            "Show Link",
                            showLinkPendingIntent
                        ).build()
                    )
                    .addAction(
                        android.app.Notification.Action.Builder(
                            android.graphics.drawable.Icon.createWithResource(this, android.R.drawable.ic_delete),
                            "Block",
                            blockPendingIntent
                        ).build()
                    )
                    .addAction(
                        android.app.Notification.Action.Builder(
                            android.graphics.drawable.Icon.createWithResource(this, android.R.drawable.ic_menu_view),
                            "Continue",
                            continuePendingIntent
                        ).build()
                    )
                    .build()

                postNotificationIfGranted(notificationManager, notificationId, notification)
            }
        }

        // Check permission for Android 13+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                Log.w("NotificationListener", "POST_NOTIFICATIONS permission not granted")
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun showThreatNotification(
        domain: String,
        result: PhishingResult,
        customReason: String? = null
    ) {
        val channelId = "zerothreat_alerts"
        val notificationManager = getSystemService(android.app.NotificationManager::class.java)

        // Create Channel if needed
        val channel = android.app.NotificationChannel(
            channelId,
            "Threat Alerts",
            android.app.NotificationManager.IMPORTANCE_HIGH
        )
        notificationManager.createNotificationChannel(channel)

        val title = if (result == PhishingResult.PHISHING) "⚠️ Phishing Detected" else "⚠️ Suspicious Link"
        val body = customReason
            ?: "Caution! The link '$domain' found in a notification might be unsafe. Click ALLOW to unblock if you trust it."

        // Create ALLOW Action
        val allowIntent = android.content.Intent(this, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_ALLOW_DOMAIN
            putExtra(NotificationActionReceiver.EXTRA_DOMAIN, domain)
            putExtra(NotificationActionReceiver.EXTRA_NOTIFICATION_ID, domain.hashCode())
        }
        
        val allowPendingIntent = android.app.PendingIntent.getBroadcast(
            this, 
            domain.hashCode(), 
            allowIntent, 
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )

        val allowAction = android.app.Notification.Action.Builder(
            android.graphics.drawable.Icon.createWithResource(this, android.R.drawable.ic_menu_add), 
            "ALLOW", 
            allowPendingIntent
        ).build()

        val notification = android.app.Notification.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .addAction(allowAction)
            .build()

        // Check permission for Android 13+
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    android.Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                postNotificationIfGranted(notificationManager, domain.hashCode(), notification)
            } else {
                Log.w("NotificationListener", "POST_NOTIFICATIONS permission not granted")
            }
        } else {
            postNotificationIfGranted(notificationManager, domain.hashCode(), notification)
        }
    }

    @SuppressLint("MissingPermission")
    private fun postNotificationIfGranted(
        notificationManager: android.app.NotificationManager,
        id: Int,
        notification: android.app.Notification
    ) {
        notificationManager.notify(id, notification)
    }

    private fun extractText(bundle: Bundle?, texts: MutableList<String>) {
        if (bundle == null) return
        
        for (key in bundle.keySet()) {
            val value = bundle.get(key)
            // Log.d("NOTIF_EXTRACT", "Key: $key, Value: $value") // Uncomment for deep debug
            
            when (value) {
                is CharSequence -> {
                    if (value.isNotBlank()) texts.add(value.toString())
                }
                is Bundle -> extractText(value, texts)
                is Array<*> -> {
                    for (item in value) {
                        if (item is Bundle) {
                            extractText(item, texts)
                        } else if (item is CharSequence) {
                            texts.add(item.toString())
                        }
                    }
                }
            }
        }
    }


    private fun normalizeUrlCandidate(url: String): String? {
        return try {
            val fixedUrl = if (!url.startsWith("http")) {
                "https://$url"
            } else url

            val uri = java.net.URI(fixedUrl)
            val host = uri.host?.lowercase() ?: return null
            val path = uri.rawPath ?: ""
            val query = uri.rawQuery?.let { "?$it" } ?: ""
            "https://${host.removePrefix("www.")}$path$query"
        } catch (_: Exception) {
            null
        }
    }

    private fun isPartOfEmailToken(text: String, start: Int, end: Int): Boolean {
        var left = start
        var right = end

        while (left > 0 && isEmailTokenChar(text[left - 1])) {
            left--
        }
        while (right < text.length && isEmailTokenChar(text[right])) {
            right++
        }

        val token = text.substring(left, right)
        return token.contains('@')
    }

    private fun isEmailTokenChar(c: Char): Boolean {
        return c.isLetterOrDigit() || c == '.' || c == '@' || c == '_' || c == '-' || c == '+'
    }

    private fun getEnabledSourcePackages(): Set<String> {
        val packages = mutableSetOf<String>()

        if (appPreferences.monitorWhatsapp) {
            packages.add("com.whatsapp")
            packages.add("com.whatsapp.w4b")
        }
        if (appPreferences.monitorInstagram) {
            packages.add("com.instagram.android")
        }
        if (appPreferences.monitorGmail) {
            packages.add("com.google.android.gm")
        }
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

}
