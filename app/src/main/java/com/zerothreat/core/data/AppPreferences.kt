package com.zerothreat.core.data

import android.content.Context
import android.content.SharedPreferences

class AppPreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("zerothreat_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_FIRST_LAUNCH = "is_first_launch"
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
        private const val KEY_PERMISSIONS_GRANTED = "permissions_granted"
        private const val KEY_MODE_SELECTED = "mode_selected"
        private const val KEY_SELECTED_MODE = "selected_mode"
        private const val KEY_SMART_MODE_ENABLED = "smart_mode_enabled"
        private const val KEY_LINK_HANDLER_PROMPT_ENABLED = "link_handler_prompt_enabled"
        private const val KEY_NOTIFICATION_MONITORING = "notification_monitoring"
        private const val KEY_LINK_MONITORING = "link_monitoring"
        private const val KEY_BLOCK_MODE_ENABLED = "block_mode_enabled"
        private const val KEY_MONITOR_WHATSAPP = "monitor_whatsapp"
        private const val KEY_MONITOR_INSTAGRAM = "monitor_instagram"
        private const val KEY_MONITOR_GMAIL = "monitor_gmail"
        private const val KEY_MONITOR_TELEGRAM = "monitor_telegram"
        private const val KEY_MONITOR_MESSAGES = "monitor_messages"
        private const val KEY_PREFERRED_BROWSER_PACKAGE = "preferred_browser_package"
        private const val KEY_LAST_KNOWN_DEFAULT_BROWSER_PACKAGE = "last_known_default_browser_package"

        // Individual app notification and onClick controls
        private const val KEY_WHATSAPP_NOTIFICATION_ENABLED = "whatsapp_notification_enabled"
        private const val KEY_WHATSAPP_ONCLICK_ENABLED = "whatsapp_onclick_enabled"
        private const val KEY_INSTAGRAM_NOTIFICATION_ENABLED = "instagram_notification_enabled"
        private const val KEY_INSTAGRAM_ONCLICK_ENABLED = "instagram_onclick_enabled"
        private const val KEY_GMAIL_NOTIFICATION_ENABLED = "gmail_notification_enabled"
        private const val KEY_GMAIL_ONCLICK_ENABLED = "gmail_onclick_enabled"
        private const val KEY_TELEGRAM_NOTIFICATION_ENABLED = "telegram_notification_enabled"
        private const val KEY_TELEGRAM_ONCLICK_ENABLED = "telegram_onclick_enabled"
        private const val KEY_MESSAGES_NOTIFICATION_ENABLED = "messages_notification_enabled"
        private const val KEY_MESSAGES_ONCLICK_ENABLED = "messages_onclick_enabled"
        private const val KEY_PROFILE_NAME = "profile_name"
        private const val KEY_PROFILE_ROLE = "profile_role"
    }

    var isFirstLaunch: Boolean
        get() = prefs.getBoolean(KEY_FIRST_LAUNCH, true)
        set(value) = prefs.edit().putBoolean(KEY_FIRST_LAUNCH, value).apply()

    var onboardingCompleted: Boolean
        get() = prefs.getBoolean(KEY_ONBOARDING_COMPLETED, false)
        set(value) = prefs.edit().putBoolean(KEY_ONBOARDING_COMPLETED, value).apply()

    var permissionsGranted: Boolean
        get() = prefs.getBoolean(KEY_PERMISSIONS_GRANTED, false)
        set(value) = prefs.edit().putBoolean(KEY_PERMISSIONS_GRANTED, value).apply()

    var modeSelected: Boolean
        get() = prefs.getBoolean(KEY_MODE_SELECTED, false)
        set(value) = prefs.edit().putBoolean(KEY_MODE_SELECTED, value).apply()

    var selectedMode: String
        get() = prefs.getString(KEY_SELECTED_MODE, "MANUAL") ?: "MANUAL"
        set(value) = prefs.edit().putString(KEY_SELECTED_MODE, value).apply()

    var smartModeEnabled: Boolean
        get() = prefs.getBoolean(KEY_SMART_MODE_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_SMART_MODE_ENABLED, value).apply()

    var linkHandlerPromptEnabled: Boolean
        get() = prefs.getBoolean(KEY_LINK_HANDLER_PROMPT_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_LINK_HANDLER_PROMPT_ENABLED, value).apply()

    var notificationMonitoring: Boolean
        get() = prefs.getBoolean(KEY_NOTIFICATION_MONITORING, false)
        set(value) = prefs.edit().putBoolean(KEY_NOTIFICATION_MONITORING, value).apply()

    var linkMonitoring: Boolean
        get() = prefs.getBoolean(KEY_LINK_MONITORING, false)
        set(value) = prefs.edit().putBoolean(KEY_LINK_MONITORING, value).apply()

    var blockModeEnabled: Boolean
        get() = prefs.getBoolean(KEY_BLOCK_MODE_ENABLED, false)
        set(value) = prefs.edit().putBoolean(KEY_BLOCK_MODE_ENABLED, value).apply()

    var monitorWhatsapp: Boolean
        get() = prefs.getBoolean(KEY_MONITOR_WHATSAPP, true)
        set(value) = prefs.edit().putBoolean(KEY_MONITOR_WHATSAPP, value).apply()

    var monitorInstagram: Boolean
        get() = prefs.getBoolean(KEY_MONITOR_INSTAGRAM, true)
        set(value) = prefs.edit().putBoolean(KEY_MONITOR_INSTAGRAM, value).apply()

    var monitorGmail: Boolean
        get() = prefs.getBoolean(KEY_MONITOR_GMAIL, true)
        set(value) = prefs.edit().putBoolean(KEY_MONITOR_GMAIL, value).apply()

    var monitorTelegram: Boolean
        get() = prefs.getBoolean(KEY_MONITOR_TELEGRAM, true)
        set(value) = prefs.edit().putBoolean(KEY_MONITOR_TELEGRAM, value).apply()

    var monitorMessages: Boolean
        get() = prefs.getBoolean(KEY_MONITOR_MESSAGES, true)
        set(value) = prefs.edit().putBoolean(KEY_MONITOR_MESSAGES, value).apply()

    var preferredBrowserPackage: String?
        get() = prefs.getString(KEY_PREFERRED_BROWSER_PACKAGE, null)
        set(value) = prefs.edit().putString(KEY_PREFERRED_BROWSER_PACKAGE, value).apply()

    var lastKnownDefaultBrowserPackage: String?
        get() = prefs.getString(KEY_LAST_KNOWN_DEFAULT_BROWSER_PACKAGE, null)
        set(value) = prefs.edit().putString(KEY_LAST_KNOWN_DEFAULT_BROWSER_PACKAGE, value).apply()

    // Individual app notification and onClick controls (default = true for both)
    var whatsappNotificationEnabled: Boolean
        get() = prefs.getBoolean(KEY_WHATSAPP_NOTIFICATION_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_WHATSAPP_NOTIFICATION_ENABLED, value).apply()

    var whatsappOnClickEnabled: Boolean
        get() = prefs.getBoolean(KEY_WHATSAPP_ONCLICK_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_WHATSAPP_ONCLICK_ENABLED, value).apply()

    var instagramNotificationEnabled: Boolean
        get() = prefs.getBoolean(KEY_INSTAGRAM_NOTIFICATION_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_INSTAGRAM_NOTIFICATION_ENABLED, value).apply()

    var instagramOnClickEnabled: Boolean
        get() = prefs.getBoolean(KEY_INSTAGRAM_ONCLICK_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_INSTAGRAM_ONCLICK_ENABLED, value).apply()

    var gmailNotificationEnabled: Boolean
        get() = prefs.getBoolean(KEY_GMAIL_NOTIFICATION_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_GMAIL_NOTIFICATION_ENABLED, value).apply()

    var gmailOnClickEnabled: Boolean
        get() = prefs.getBoolean(KEY_GMAIL_ONCLICK_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_GMAIL_ONCLICK_ENABLED, value).apply()

    var telegramNotificationEnabled: Boolean
        get() = prefs.getBoolean(KEY_TELEGRAM_NOTIFICATION_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_TELEGRAM_NOTIFICATION_ENABLED, value).apply()

    var telegramOnClickEnabled: Boolean
        get() = prefs.getBoolean(KEY_TELEGRAM_ONCLICK_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_TELEGRAM_ONCLICK_ENABLED, value).apply()

    var messagesNotificationEnabled: Boolean
        get() = prefs.getBoolean(KEY_MESSAGES_NOTIFICATION_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_MESSAGES_NOTIFICATION_ENABLED, value).apply()

    var messagesOnClickEnabled: Boolean
        get() = prefs.getBoolean(KEY_MESSAGES_ONCLICK_ENABLED, true)
        set(value) = prefs.edit().putBoolean(KEY_MESSAGES_ONCLICK_ENABLED, value).apply()

    var profileName: String
        get() = prefs.getString(KEY_PROFILE_NAME, "ZeroThreat User") ?: "ZeroThreat User"
        set(value) = prefs.edit().putString(KEY_PROFILE_NAME, value).apply()

    var profileRole: String
        get() = prefs.getString(KEY_PROFILE_ROLE, "Security Analyst") ?: "Security Analyst"
        set(value) = prefs.edit().putString(KEY_PROFILE_ROLE, value).apply()

    fun clearAll() {
        prefs.edit().clear().apply()
    }

    fun resetFirstLaunch() {
        isFirstLaunch = true
    }
}
