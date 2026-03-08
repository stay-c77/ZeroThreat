package com.zerothreat.core

import android.app.role.RoleManager
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.setContent
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.zerothreat.core.browser.rememberExternalDefaultBrowser
import com.zerothreat.core.data.AppPreferences
import com.zerothreat.core.ui.ZeroThreatApp

class MainActivity : AppCompatActivity() {
    companion object {
        private const val TAG = "MainActivity"
        private const val BROWSER_ROLE_REQUEST_CODE = 1001
    }

    private lateinit var appPreferences: AppPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appPreferences = AppPreferences(this)
        rememberExternalDefaultBrowser(this, appPreferences)

        setContent {
            ZeroThreatApp()
        }

        maybeShowFirstLaunchLinkPrompt()
    }

    override fun onResume() {
        super.onResume()
        // Check if browser role is now held after returning from settings
        if (isBrowserRoleHeld()) {
            Log.d(TAG, "✅ Browser role is now active")
        } else {
            // If permission not granted, show prompt again
            Log.w(TAG, "⚠️ Browser role not active - user needs to grant permission")
        }
    }

    private fun maybeShowFirstLaunchLinkPrompt() {
        // Always check if permission is granted, not just on first launch
        if (!appPreferences.linkHandlerPromptEnabled) return

        // If browser role already held, no need to show dialog
        if (isBrowserRoleHeld()) {
            // Mark first launch as complete if permission is granted
            if (appPreferences.isFirstLaunch) {
                appPreferences.isFirstLaunch = false
            }
            return
        }

        // Show dialog if this is first launch OR if permission not granted
        val shouldShowDialog = appPreferences.isFirstLaunch || !isBrowserRoleHeld()

        if (!shouldShowDialog) return

        // Only mark first launch as complete after showing dialog
        if (appPreferences.isFirstLaunch) {
            appPreferences.isFirstLaunch = false
        }

        try {
            MaterialAlertDialogBuilder(this, R.style.Theme_ZeroThreatCore_Dialog)
                .setTitle("Enable Link Protection")
                .setMessage(
                    "ZeroThreat needs to be set as your default browser to scan links BEFORE they open.\n\n" +
                    "How it works:\n" +
                    "1. You click a link in WhatsApp/Instagram/Gmail/SMS\n" +
                    "2. ZeroThreat scans it for phishing\n" +
                    "3. Shows safety percentage and options\n" +
                    "4. Opens in your preferred browser if you choose Continue\n\n" +
                    "This is required for link protection to work."
                )
                .setPositiveButton("Enable Now") { _, _ ->
                    requestBrowserRoleOrSettings()
                }
                .setNegativeButton("Later") { _, _ ->
                    Toast.makeText(
                        this,
                        "Link protection is disabled. Enable it in Settings to scan links.",
                        Toast.LENGTH_LONG
                    ).show()
                }
                .setCancelable(false)
                .show()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to show permission dialog", e)
            // If dialog fails, try direct permission request
            requestBrowserRoleOrSettings()
        }
    }

    private fun isBrowserRoleHeld(): Boolean {
        return try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
            val roleManager = getSystemService(RoleManager::class.java) ?: return false
            roleManager.isRoleAvailable(RoleManager.ROLE_BROWSER) &&
                roleManager.isRoleHeld(RoleManager.ROLE_BROWSER)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking browser role", e)
            false
        }
    }

    fun requestBrowserRoleOrSettings() {
        rememberExternalDefaultBrowser(this, appPreferences)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roleManager = getSystemService(RoleManager::class.java)
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_BROWSER)) {
                if (roleManager.isRoleHeld(RoleManager.ROLE_BROWSER)) {
                    Toast.makeText(this, "✅ ZeroThreat is already set as your browser", Toast.LENGTH_SHORT).show()
                    return
                }
                try {
                    val intent = roleManager.createRequestRoleIntent(RoleManager.ROLE_BROWSER)
                    startActivityForResult(intent, BROWSER_ROLE_REQUEST_CODE)
                    return
                } catch (e: Exception) {
                    Log.w(TAG, "Browser role request failed, opening settings fallback", e)
                }
            }
        }

        // Fallback for older Android versions or if role request fails
        showBrowserSetupInstructions()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == BROWSER_ROLE_REQUEST_CODE) {
            if (isBrowserRoleHeld()) {
                Toast.makeText(
                    this,
                    "✅ Link protection enabled! ZeroThreat will now scan links before they open.",
                    Toast.LENGTH_LONG
                ).show()
            } else {
                Toast.makeText(
                    this,
                    "Link protection not enabled. You can try again in Settings.",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private fun showBrowserSetupInstructions() {
        MaterialAlertDialogBuilder(this, R.style.Theme_ZeroThreatCore_Dialog)
            .setTitle("Manual Setup Required")
            .setMessage(
                "To enable link protection:\n\n" +
                "1. Tap 'Open Settings' below\n" +
                "2. Find 'Browser app' or 'Default apps'\n" +
                "3. Select 'ZeroThreat Browser'\n\n" +
                "This allows ZeroThreat to scan links before they open."
            )
            .setPositiveButton("Open Settings") { _, _ ->
                if (!openLinkHandlerSettings()) {
                    Toast.makeText(
                        this,
                        "Please go to: Settings > Apps > Default apps > Browser app",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun openLinkHandlerSettings(): Boolean {
        val intents = listOf(
            Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS),
            Intent(Settings.ACTION_APP_OPEN_BY_DEFAULT_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
            },
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
            },
            Intent(Settings.ACTION_SETTINGS)
        ).map { intent ->
            intent.apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        }

        for (candidate in intents) {
            if (candidate.resolveActivity(packageManager) == null) continue
            try {
                startActivity(candidate)
                return true
            } catch (e: Exception) {
                Log.w(TAG, "Failed opening settings action: ${candidate.action}", e)
            }
        }

        return false
    }
}
