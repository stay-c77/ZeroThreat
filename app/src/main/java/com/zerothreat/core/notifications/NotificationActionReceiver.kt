package com.zerothreat.core.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import com.zerothreat.core.detector.UserAllowlistManager
import com.zerothreat.core.detector.PhishingResult
import com.zerothreat.core.data.db.AppDatabase
import com.zerothreat.core.data.repository.UrlRepository
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class NotificationActionReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_ALLOW_DOMAIN = "com.zerothreat.core.ACTION_ALLOW_DOMAIN"
        const val ACTION_SHOW_LINK = "com.zerothreat.core.ACTION_SHOW_LINK"
        const val ACTION_BLOCK_URL = "com.zerothreat.core.ACTION_BLOCK_URL"
        const val ACTION_CONTINUE_URL = "com.zerothreat.core.ACTION_CONTINUE_URL"
        const val ACTION_SHOW_IN_CHAT = "com.zerothreat.core.ACTION_SHOW_IN_CHAT"

        const val EXTRA_DOMAIN = "domain"
        const val EXTRA_URL = "url"
        const val EXTRA_RESULT = "result"
        const val EXTRA_SCORE = "score"
        const val EXTRA_DESCRIPTION = "description"
        const val EXTRA_NOTIFICATION_ID = "notification_id"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val notificationId = intent.getIntExtra(EXTRA_NOTIFICATION_ID, -1)
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager

        when (intent.action) {
            ACTION_ALLOW_DOMAIN -> {
                val domain = intent.getStringExtra(EXTRA_DOMAIN)
                if (!domain.isNullOrBlank()) {
                    Log.d("NotifReceiver", "User allowed domain: $domain")
                    UserAllowlistManager.init(context)
                    UserAllowlistManager.allow(domain)
                    Toast.makeText(context, "$domain allowed. You can now access it.", Toast.LENGTH_LONG).show()
                    notificationManager.cancel(notificationId)
                }
            }

            ACTION_SHOW_LINK -> {
                val url = intent.getStringExtra(EXTRA_URL)
                val domain = intent.getStringExtra(EXTRA_DOMAIN)
                if (!url.isNullOrBlank()) {
                    // Show the URL without opening it
                    Toast.makeText(context, "Link: $url", Toast.LENGTH_LONG).show()
                    Log.d("NotifReceiver", "Showing link: $url")
                }
                // Don't cancel notification - user might want to take other actions
            }

            ACTION_BLOCK_URL -> {
                val url = intent.getStringExtra(EXTRA_URL)
                val domain = intent.getStringExtra(EXTRA_DOMAIN)
                val resultStr = intent.getStringExtra(EXTRA_RESULT)
                val score = intent.getIntExtra(EXTRA_SCORE, 0)
                val description = intent.getStringExtra(EXTRA_DESCRIPTION)

                if (!url.isNullOrBlank() && !domain.isNullOrBlank()) {
                    val result = try {
                        PhishingResult.valueOf(resultStr ?: "SUSPICIOUS")
                    } catch (e: Exception) {
                        PhishingResult.SUSPICIOUS
                    }

                    // Block URL in database
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val db = AppDatabase.getDatabase(context)
                            val repository = UrlRepository(db.urlDao())
                            repository.blockUrl(
                                url = url,
                                domain = domain,
                                result = result,
                                source = "notification",
                                phishingScore = score,
                                analysisNote = description
                            )
                            Log.d("NotifReceiver", "URL blocked: $url")
                        } catch (e: Exception) {
                            Log.e("NotifReceiver", "Failed to block URL", e)
                        }
                    }

                    Toast.makeText(context, "URL blocked: $domain", Toast.LENGTH_SHORT).show()
                    notificationManager.cancel(notificationId)
                }
            }

            ACTION_CONTINUE_URL -> {
                val url = intent.getStringExtra(EXTRA_URL)
                if (!url.isNullOrBlank()) {
                    // Open URL in browser
                    try {
                        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(browserIntent)
                        Log.d("NotifReceiver", "Opening URL: $url")
                    } catch (e: Exception) {
                        Toast.makeText(context, "Failed to open URL", Toast.LENGTH_SHORT).show()
                        Log.e("NotifReceiver", "Failed to open URL", e)
                    }
                    notificationManager.cancel(notificationId)
                }
            }

            ACTION_SHOW_IN_CHAT -> {
                // Just dismiss the notification - user can see link in chat
                Toast.makeText(context, "Link is safe, you can view it in the chat", Toast.LENGTH_SHORT).show()
                notificationManager.cancel(notificationId)
            }
        }
    }
}
