// Blacklist updated now
package com.zerothreat.core.detector

import android.content.Context
import java.net.URI
import java.util.concurrent.ConcurrentHashMap

object BlacklistManager {
    private val blacklistedExactEntries = ConcurrentHashMap.newKeySet<String>()
    private var loaded = false

    fun isBlacklisted(input: String): Boolean = isExactBlacklisted(input)

    fun isExactBlacklisted(input: String): Boolean {
        val normalized = normalizeForExactMatch(input)
        return blacklistedExactEntries.contains(normalized)
    }

    fun load(context: Context) {
        if (loaded) return

        // Add explicit test domain for local verification.
        blacklistedExactEntries.add("test-phishing.com")

        // Project policy: use only bundled blacklist/whitelist assets.
        loadCsv(context, "blacklists/phishtank.csv")
        loaded = true
    }

    private fun normalizeForExactMatch(value: String): String {
        val raw = value.trim().lowercase()
        if (raw.isEmpty()) return raw

        return try {
            val withScheme = if (raw.startsWith("http://") || raw.startsWith("https://")) raw else "http://$raw"
            val uri = URI(withScheme)
            val host = uri.host?.removePrefix("www.") ?: raw.removePrefix("www.")
            val path = uri.rawPath ?: ""
            val query = uri.rawQuery

            when {
                query != null -> {
                    val canonicalPath = if (path.isBlank()) "/" else path
                    "$host$canonicalPath?$query".removeSuffix("/")
                }
                path.isBlank() || path == "/" -> host
                else -> "$host$path".removeSuffix("/")
            }
        } catch (e: Exception) {
            raw.removePrefix("http://")
                .removePrefix("https://")
                .removePrefix("www.")
                .removeSuffix("/")
        }
    }

    private fun loadCsv(context: Context, filePath: String) {
        try {
            context.assets.open(filePath).use { input ->
                input.bufferedReader().useLines { lines ->
                    lines.forEach { line ->
                        if (line.startsWith("#") || line.isBlank()) return@forEach

                        val parts = parseCsvLine(line)
                        if (parts.size < 2) return@forEach
                        if (parts[0].equals("phish_id", ignoreCase = true)) return@forEach

                        val normalized = normalizeForExactMatch(parts[1])
                        if (normalized.isNotBlank()) {
                            blacklistedExactEntries.add(normalized)
                        }
                    }
                }
            }
        } catch (_: Exception) {
            // Ignore missing files or parse errors.
        }
    }

    private fun parseCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()
        var inQuotes = false
        var i = 0

        while (i < line.length) {
            val c = line[i]
            when {
                c == '"' && inQuotes && i + 1 < line.length && line[i + 1] == '"' -> {
                    current.append('"')
                    i++
                }
                c == '"' -> inQuotes = !inQuotes
                c == ',' && !inQuotes -> {
                    result.add(current.toString())
                    current.setLength(0)
                }
                else -> current.append(c)
            }
            i++
        }
        result.add(current.toString())
        return result
    }
}
