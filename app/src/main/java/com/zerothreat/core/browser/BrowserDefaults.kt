package com.zerothreat.core.browser

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import com.zerothreat.core.data.AppPreferences

private const val BROWSER_CHECK_URL = "https://example.com"

fun resolveExternalDefaultBrowserPackage(context: Context): String? {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(BROWSER_CHECK_URL)).apply {
        addCategory(Intent.CATEGORY_BROWSABLE)
    }
    val resolved = context.packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        ?: return null
    val packageName = resolved.activityInfo?.packageName ?: return null
    if (packageName == context.packageName || packageName == "android") {
        return null
    }
    return packageName
}

fun rememberExternalDefaultBrowser(context: Context, appPreferences: AppPreferences) {
    resolveExternalDefaultBrowserPackage(context)?.let {
        appPreferences.lastKnownDefaultBrowserPackage = it
    }
}
