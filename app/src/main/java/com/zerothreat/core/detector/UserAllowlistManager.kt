package com.zerothreat.core.detector

import android.content.Context
import android.content.SharedPreferences
import java.util.concurrent.ConcurrentHashMap

/**
 * Manages domains that the user has explicitly allowed after a detection.
 * These domains bypass Phishing/Suspicious checks.
 */
object UserAllowlistManager {
    private const val PREF_NAME = "zerothreat_user_allowlist"
    private const val KEY_DOMAINS = "allowed_domains"
    
    private val allowedDomains = ConcurrentHashMap.newKeySet<String>()
    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        if (prefs != null) return
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        loadFromPrefs()
    }

    private fun loadFromPrefs() {
        val saved = prefs?.getStringSet(KEY_DOMAINS, emptySet()) ?: emptySet()
        allowedDomains.addAll(saved)
    }

    fun allow(domain: String) {
        val clean = domain.lowercase().trim()
        if (allowedDomains.add(clean)) {
            persist()
        }
    }

    fun isAllowed(domain: String): Boolean {
        return allowedDomains.contains(domain.lowercase().trim())
    }

    private fun persist() {
        prefs?.edit()?.putStringSet(KEY_DOMAINS, allowedDomains.toMutableSet())?.apply()
    }
    
    fun clear() {
        allowedDomains.clear()
        persist()
    }
}
