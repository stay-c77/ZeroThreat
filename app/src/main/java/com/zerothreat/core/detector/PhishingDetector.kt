package com.zerothreat.core.detector

import android.content.Context
import android.util.Log
import com.zerothreat.core.data.db.AppDatabase
import java.net.IDN
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlin.math.max

object PhishingDetector {
    private const val MAX_REDIRECT_DEPTH = 2

    data class AnalysisReport(
        val result: PhishingResult,
        val score: Int,
        val description: String,
        val exactBlacklistMatch: Boolean = false
    )

    private data class ParsedUrl(
        val original: String,
        val scheme: String?,
        val host: String,
        val path: String,
        val query: String,
        val userInfo: String?,
        val isIpv4Literal: Boolean,
        val isIpv6Literal: Boolean,
        val canonicalBlacklistKey: String,
        val redirectTargets: List<String>
    )

    private data class BasicLexicalFeatures(
        val urlLength: Int,
        val dotCount: Int,
        val specialCharCount: Int,
        val specialCharDensity: Double, val usesNonHttps: Boolean,
        val hasScriptEndpoint: Boolean,
        val hasRandomAlphaNumSegment: Boolean
    )

    private val allowlist = setOf(
        "google.com", "youtube.com", "gmail.com", "googleapis.com", "gvt2.com", "android.com", "gstatic.com",
        "facebook.com", "fb.com", "instagram.com", "whatsapp.com",
        "amazon.com", "paypal.com", "apple.com", "icloud.com",
        "microsoft.com", "outlook.com", "netflix.com"
    )

    private val topBrands = setOf(
        "google", "youtube", "gmail", "facebook", "fb", "instagram", "whatsapp",
        "amazon", "paypal", "apple", "icloud", "microsoft", "outlook", "netflix",
        "twitter", "x.com", "linkedin", "dropbox", "wellsfargo"
    )

    private val suspiciousTlds = setOf(
        "tk", "ml", "ga", "cf", "gq", "xyz", "buzz", "shop", "icu", "top", "exam"
    )

    private val ignoredPrefixes = listOf(
        "android.", "androidx.", "com.android.", "com.google.", "java.", "kotlin.", "datastore.", "service."
    )

    private val keywords = listOf(
        "login", "verify", "secure", "account", "update", "bank", "wallet", "signin",
        "password", "auth", "otp", "confirm", "billing", "card", "payment"
    )

    private val redirectKeys = setOf(
        "url", "u", "target", "dest", "destination", "next", "continue", "to",
        "redirect", "redirect_url", "redirecturi", "redirect_uri",
        "return", "returnto", "return_url", "redir", "out", "link"
    )

    private val shortenerDomains = setOf(
        "bit.ly", "t.co", "tinyurl.com", "goo.gl", "ow.ly", "is.gd",
        "cutt.ly", "rebrand.ly", "tiny.cc", "shorturl.at", "buff.ly"
    )

    private val confusableChars = setOf(
        // Cyrillic
        'а', 'е', 'о', 'р', 'х', 'с', 'у', 'і', 'ј', 'ӏ', 'ԁ',
        // Greek
        'α', 'β', 'γ', 'δ', 'ε', 'ι', 'κ', 'ν', 'ο', 'ρ', 'τ', 'υ', 'χ'
    )

    fun analyze(context: Context, domaininput: String, source: String = "Auto-Scan"): PhishingResult {
        return analyzeDetailed(context, domaininput, source).result
    }

    fun analyzeDetailed(context: Context, domaininput: String, source: String = "Auto-Scan"): AnalysisReport {
        return analyzeDetailedInternal(
            context = context,
            domaininput = domaininput,
            source = source,
            depth = 0,
            visited = mutableSetOf()
        )
    }

    private fun analyzeDetailedInternal(
        context: Context,
        domaininput: String,
        source: String,
        depth: Int,
        visited: MutableSet<String>
    ): AnalysisReport {
        val parsed = parseInput(domaininput)
        val cleanDomain = parsed.host
        val shouldPersist = depth == 0

        val nodeKey = parsed.canonicalBlacklistKey
            .ifBlank { cleanDomain }
            .ifBlank { domaininput.trim().lowercase() }
        if (nodeKey.isNotBlank() && !visited.add(nodeKey)) {
            return AnalysisReport(
                result = PhishingResult.SUSPICIOUS,
                score = 35,
                description = "35% suspicious - Redirect loop detected"
            )
        }

        if (parsed.scheme != null && parsed.scheme !in setOf("http", "https")) {
            val nonWebReport = when (parsed.scheme) {
                "javascript", "data", "file" -> AnalysisReport(
                    result = PhishingResult.PHISHING,
                    score = 100,
                    description = "100% unsafe - Unsupported unsafe link scheme"
                )
                else -> AnalysisReport(
                    result = PhishingResult.SUSPICIOUS,
                    score = 40,
                    description = "40% suspicious - Non-web link scheme"
                )
            }
            if (shouldPersist) {
                logToDatabase(context, domaininput, cleanDomain, nonWebReport, source)
            }
            return nonWebReport
        }

        if (cleanDomain.isBlank()) {
            val report = AnalysisReport(
                result = PhishingResult.SUSPICIOUS,
                score = RiskScorePolicy.INVALID_URL,
                description = "${RiskScorePolicy.INVALID_URL}% suspicious - Invalid or incomplete link"
            )
            if (shouldPersist) {
                logToDatabase(context, domaininput, cleanDomain, report, source)
            }
            return report
        }

        if (isPdfResource(parsed)) {
            return AnalysisReport(
                result = PhishingResult.SAFE,
                score = 0,
                description = "Safe link - PDF scan skipped"
            )
        }

        BlacklistManager.load(context)
        val blacklistMatch = listOf(
            domaininput,
            cleanDomain,
            parsed.canonicalBlacklistKey
        ).any { it.isNotBlank() && BlacklistManager.isExactBlacklisted(it) }

        if (blacklistMatch) {
            val report = AnalysisReport(
                result = PhishingResult.PHISHING,
                score = 100,
                description = "Known unsafe link",
                exactBlacklistMatch = true
            )
            if (shouldPersist) {
                logToDatabase(context, domaininput, cleanDomain, report, source)
            }
            return report
        }

        val redirectChainOverride = evaluateRedirectChain(
            context = context,
            redirectTargets = parsed.redirectTargets,
            source = source,
            depth = depth,
            visited = visited
        )
        if (redirectChainOverride != null) {
            if (shouldPersist) {
                logToDatabase(context, domaininput, cleanDomain, redirectChainOverride, source)
            }
            return redirectChainOverride
        }

        UserAllowlistManager.init(context)
        if (UserAllowlistManager.isAllowed(cleanDomain)) {
            val report = AnalysisReport(
                result = PhishingResult.SAFE,
                score = 0,
                description = "Safe link"
            )
            if (shouldPersist) {
                logToDatabase(context, domaininput, cleanDomain, report, source)
            }
            return report
        }

        if (isSystemDomain(cleanDomain)) {
            val report = AnalysisReport(
                result = PhishingResult.SAFE,
                score = 0,
                description = "Ignored internal link"
            )
            return report
        }

        WhitelistManager.load(context)
        if (WhitelistManager.isWhitelisted(cleanDomain) || isAllowlisted(cleanDomain)) {
            val report = AnalysisReport(
                result = PhishingResult.SAFE,
                score = 0,
                description = "Safe link"
            )
            if (shouldPersist) {
                logToDatabase(context, domaininput, cleanDomain, report, source)
            }
            return report
        }

        var score = 0
        val reasons = linkedSetOf<String>()

        val dynamicBrands = WhitelistManager.getTopBrands(500)
        val allBrands = (topBrands + dynamicBrands).distinct()
        val labels = cleanDomain.split(".")
        val signalText = buildSignalText(parsed)
        val tokenCandidates = buildDomainLabelCandidates(labels)

        var brandScore = 0
        for (candidate in tokenCandidates) {
            for (brand in allBrands) {
                val normalizedBrand = brand.substringBefore(".")
                if (normalizedBrand.length < 4) continue

                // Check for exact substring match first
                if (candidate.contains(normalizedBrand) && candidate != normalizedBrand) {
                    brandScore = max(brandScore, 14)
                    reasons.add("Looks similar to a known brand")
                }

                // DIRECT Levenshtein distance (NO deduplication) - catches typosquatting
                val directDist = StringSimilarityUtils.levenshtein(candidate, normalizedBrand)
                if (directDist == 1 && candidate != normalizedBrand) {
                    // Single character difference = suspicious (e.g., gooogle vs google, netflixx vs netflix)
                    brandScore = max(brandScore, 32)
                    reasons.add("Looks like a fake brand spelling (typo/extra character)")
                }

                // Also check deduplicated version for other duplicate patterns
                val dedupedCandidate = StringSimilarityUtils.deduplicateChars(candidate)
                val dedupDist = StringSimilarityUtils.levenshtein(dedupedCandidate, normalizedBrand)
                if (dedupDist <= 1 && candidate != normalizedBrand && dedupedCandidate != candidate) {
                    // If deduplication makes it similar, it's suspicious
                    brandScore = max(brandScore, 28)
                    reasons.add("Looks like a fake brand with duplicate characters")
                }

                // Jaro-Winkler for high similarity
                val jwScore = StringSimilarityUtils.jaroWinkler(candidate, normalizedBrand)
                if (jwScore >= 0.97 && candidate != normalizedBrand) {
                    brandScore = max(brandScore, 36)
                    reasons.add("Looks very similar to a known brand")
                } else if (jwScore >= 0.94 && candidate != normalizedBrand) {
                    brandScore = max(brandScore, 20)
                }
            }
        }
        val cappedBrandScore = brandScore.coerceAtMost(40)
        score += cappedBrandScore

        // ⭐ NEW: Check similarity against whitelist domains for typosquatting
        // This catches attempts to mimic legitimate whitelisted websites
        var whitelistScore = 0
        WhitelistManager.load(context)
        val whitelistedDomains = WhitelistManager.getTopBrands(1000) // Check top 1000 whitelisted brands

        for (candidate in tokenCandidates) {
            for (whitelistedBrand in whitelistedDomains) {
                val normalizedWhitelistBrand = whitelistedBrand.substringBefore(".")
                if (normalizedWhitelistBrand.length < 4) continue

                // DIRECT distance check - catches typosquatting like gooogle vs google
                val directWhitelistDist = StringSimilarityUtils.levenshtein(candidate, normalizedWhitelistBrand)
                if (directWhitelistDist == 1 && candidate != normalizedWhitelistBrand) {
                    whitelistScore = max(whitelistScore, 35)
                    reasons.add("Typo of whitelisted domain '${normalizedWhitelistBrand}'")
                    break // Found a match, no need to check further
                }

                // Jaro-Winkler for very high similarity
                val jwWhitelistScore = StringSimilarityUtils.jaroWinkler(candidate, normalizedWhitelistBrand)
                if (jwWhitelistScore >= 0.98 && candidate != normalizedWhitelistBrand) {
                    whitelistScore = max(whitelistScore, 34)
                    reasons.add("Very similar to whitelisted domain '${normalizedWhitelistBrand}'")
                    break
                }
            }
            if (whitelistScore > 0) break // Found a suspicious match
        }

        // Only add whitelist score if it's higher than brand score (don't penalize twice)
        if (whitelistScore > cappedBrandScore) {
            score += (whitelistScore - cappedBrandScore)
        }

        var homographDetected = false
        val decodedLabels = decodePunycodeLabels(labels)
        if (hasHomographChars(decodedLabels)) {
            homographDetected = true
            score += 100
            reasons.add("Looks like a disguised link")
        }

        var keywordPoints = 0
        val keywordCount = keywords.count { signalText.contains(it) }
        if (keywordCount > 0) keywordPoints = 20
        if (keywordCount >= 2) keywordPoints += 10
        if (keywordCount >= 4) keywordPoints += 10
        score += keywordPoints
        if (keywordPoints > 0) reasons.add("Contains risky words")

        val tld = labels.lastOrNull() ?: ""
        if (suspiciousTlds.contains(tld)) {
            score += 15
            reasons.add("Uses an unusual domain ending")
        }

        if (parsed.isIpv4Literal || parsed.isIpv6Literal) {
            score += 50
            reasons.add("Uses an IP address instead of a name")
        }

        if (!parsed.userInfo.isNullOrBlank()) {
            score += 45
            reasons.add("Contains hidden username info")
        }

        if (isShortenerDomain(cleanDomain)) {
            score += 25
            reasons.add("Uses a shortened link")
        }

        if (parsed.redirectTargets.isNotEmpty()) {
            score += 20
            reasons.add("Contains redirect to another link")

            parsed.redirectTargets.take(3).forEach { target ->
                val targetParsed = parseInput(target)
                val targetDomain = targetParsed.host
                if (targetDomain.isNotBlank() && targetDomain != cleanDomain) {
                    score += 15
                    reasons.add("Redirects to another domain")
                }
                if (
                    BlacklistManager.isExactBlacklisted(target) ||
                    (targetDomain.isNotBlank() && BlacklistManager.isExactBlacklisted(targetDomain))
                ) {
                    score = 100
                    reasons.add("Redirect target is blacklisted")
                }

                val targetSignalText = buildSignalText(targetParsed)
                if (keywords.any { targetSignalText.contains(it) }) {
                    score += 10
                }
            }
        }

        val basicLexicalFeatures = extractBasicLexicalFeatures(domaininput, parsed)
        score += scoreBasicLexicalFeatures(basicLexicalFeatures, reasons)

        val hasCriticalSignal = homographDetected
        val hasStrongBrandSignal = cappedBrandScore >= 24
        val hasKeywordSignal = keywordPoints > 0
        val hasInfraSignal =
            parsed.isIpv4Literal || parsed.isIpv6Literal ||
            !parsed.userInfo.isNullOrBlank() || isShortenerDomain(cleanDomain) ||
            basicLexicalFeatures.usesNonHttps
        val hasRedirectSignal = parsed.redirectTargets.isNotEmpty()
        val phishingSignalCount = listOf(
            hasStrongBrandSignal,
            hasKeywordSignal,
            hasInfraSignal,
            hasRedirectSignal,
            homographDetected
        ).count { it }
        val normalizedScore = RiskScorePolicy.normalizeHeuristicScore(
            rawScore = score,
            phishingSignalCount = phishingSignalCount,
            hasCriticalSignal = hasCriticalSignal
        )

        val finalResult = when {
            normalizedScore >= 80 && phishingSignalCount >= 2 -> PhishingResult.PHISHING
            normalizedScore >= 10 -> PhishingResult.SUSPICIOUS
            else -> PhishingResult.SAFE
        }

        val summary = when (finalResult) {
            PhishingResult.PHISHING -> "$normalizedScore% unsafe"
            PhishingResult.SUSPICIOUS -> "$normalizedScore% suspicious"
            PhishingResult.SAFE -> "$normalizedScore% safe"
        }
        val description = if (reasons.isNotEmpty()) {
            "$summary - ${reasons.first()}"
        } else {
            "$summary"
        }

        val report = AnalysisReport(
            result = finalResult,
            score = normalizedScore,
            description = description
        )

        if (shouldPersist) {
            logToDatabase(context, domaininput, cleanDomain, report, source)
        }

        return report
    }

    fun normalizeInput(input: String): String {
        return parseInput(input.lowercase().trim()).host
    }

    internal fun isSystemDomain(domain: String): Boolean {
        val clean = domain.lowercase()
        return ignoredPrefixes.any { clean.contains(it) } ||
            clean.contains(".app.notification") ||
            clean.contains("android.resource") ||
            clean.endsWith("googleapis.com") ||
            clean.endsWith("gstatic.com") ||
            clean.endsWith("googleusercontent.com") ||
            clean.contains("gvt1.com") ||
            clean.contains("gvt2.com") ||
            clean.endsWith("instagram.com") ||
            clean.endsWith("cdninstagram.com") ||
            clean.endsWith("fbcdn.net") ||
            clean.endsWith("facebook.com") ||
            clean.endsWith("whatsapp.net") ||
            clean.endsWith("whatsapp.com") ||
            clean.endsWith("telegram.org") ||
            clean.endsWith("telegram.me")
    }

    private fun parseInput(input: String): ParsedUrl {
        val trimmed = input.trim()
        val hasScheme = Regex("^[a-zA-Z][a-zA-Z0-9+.-]*:").containsMatchIn(trimmed)
        val withScheme = if (hasScheme) trimmed else "https://$trimmed"

        val uri = try {
            URI(withScheme)
        } catch (_: Exception) {
            null
        }

        val host = uri?.host?.lowercase()?.removePrefix("www.")?.removeSuffix(".").orEmpty()
        val path = uri?.rawPath.orEmpty()
        val query = uri?.rawQuery.orEmpty()
        val scheme = uri?.scheme?.lowercase()
        val userInfo = uri?.userInfo
        val isIpv4 = host.matches(Regex("^[0-9]+\\.[0-9]+\\.[0-9]+\\.[0-9]+$"))
        val isIpv6 = host.contains(":")

        val canonicalPath = if (path.isBlank()) "/" else path
        val canonicalBlacklistKey = when {
            host.isBlank() -> ""
            query.isNotBlank() -> "$host$canonicalPath?$query"
            path.isBlank() || path == "/" -> host
            else -> "$host$path"
        }.removeSuffix("/")

        val redirectTargets = extractRedirectTargets(query)

        return ParsedUrl(
            original = input,
            scheme = scheme,
            host = host,
            path = path,
            query = query,
            userInfo = userInfo,
            isIpv4Literal = isIpv4,
            isIpv6Literal = isIpv6,
            canonicalBlacklistKey = canonicalBlacklistKey,
            redirectTargets = redirectTargets
        )
    }

    private fun buildSignalText(parsed: ParsedUrl): String {
        val decodedPath = decodeUrlComponent(parsed.path)
        val decodedQuery = decodeUrlComponent(parsed.query)
        return "${parsed.host} $decodedPath $decodedQuery".lowercase()
    }

    private fun buildDomainLabelCandidates(labels: List<String>): List<String> {
        return labels
            .flatMap { listOf(it) + it.split("-", "_") }
            .map { it.trim() }
            .filter { it.length >= 3 && ignoredPrefixes.none { prefix -> it.startsWith(prefix) } }
            .distinct()
    }

    private fun evaluateRedirectChain(
        context: Context,
        redirectTargets: List<String>,
        source: String,
        depth: Int,
        visited: MutableSet<String>
    ): AnalysisReport? {
        if (depth >= MAX_REDIRECT_DEPTH || redirectTargets.isEmpty()) return null

        var maxSuspiciousScore = 0
        for (target in redirectTargets.take(3)) {
            val targetParsed = parseInput(target)
            val targetKey = targetParsed.canonicalBlacklistKey
                .ifBlank { targetParsed.host }
                .ifBlank { target.trim().lowercase() }

            if (targetKey.isBlank() || visited.contains(targetKey)) continue

            val report = analyzeDetailedInternal(
                context = context,
                domaininput = target,
                source = source,
                depth = depth + 1,
                visited = visited.toMutableSet()
            )

            when (report.result) {
                PhishingResult.PHISHING -> {
                    return AnalysisReport(
                        result = PhishingResult.PHISHING,
                        score = 100,
                        description = "100% unsafe - Redirect target is unsafe"
                    )
                }
                PhishingResult.SUSPICIOUS -> {
                    maxSuspiciousScore = max(maxSuspiciousScore, report.score)
                }
                PhishingResult.SAFE -> Unit
            }
        }

        if (maxSuspiciousScore > 0) {
            val score = max(45, maxSuspiciousScore)
            return AnalysisReport(
                result = PhishingResult.SUSPICIOUS,
                score = score,
                description = "$score% suspicious - Redirect target looks suspicious"
            )
        }

        return null
    }

    private fun extractRedirectTargets(query: String): List<String> {
        if (query.isBlank()) return emptyList()

        val extracted = mutableListOf<String>()
        val parts = query.split("&")
        for (part in parts) {
            if (part.isBlank()) continue
            val kv = part.split("=", limit = 2)
            val key = decodeUrlComponent(kv[0]).lowercase()
            val value = decodeUrlComponent(kv.getOrElse(1) { "" }).trim()

            if (key in redirectKeys && value.isNotBlank()) {
                extracted.add(value)
                extracted.addAll(extractHttpUrls(value))
            } else if (value.contains("http://") || value.contains("https://") || value.contains("%2f")) {
                extracted.addAll(extractHttpUrls(value))
            }
        }

        extracted.addAll(extractHttpUrls(decodeUrlComponent(query)))
        return extracted.map { it.trim() }.filter { it.isNotBlank() }.distinct().take(5)
    }

    private fun extractHttpUrls(text: String): List<String> {
        val decoded = decodeUrlComponent(text)
        val regex = Regex("""https?://[^\s"'<>]+""", RegexOption.IGNORE_CASE)
        return regex.findAll(decoded).map { match ->
            match.value.trimEnd('.', ',', ';', ')', ']', '>')
        }.toList()
    }

    private fun isPdfResource(parsed: ParsedUrl): Boolean {
        val normalizedPath = decodeUrlComponent(parsed.path)
            .substringBefore('#')
            .lowercase()
            .trim()
        if (normalizedPath.endsWith(".pdf")) return true

        return parsed.redirectTargets.any { target ->
            val targetPath = decodeUrlComponent(parseInput(target).path)
                .substringBefore('#')
                .lowercase()
                .trim()
            targetPath.endsWith(".pdf")
        }
    }

    private fun isLexicalSpecialChar(c: Char): Boolean {
        return when (c) {
            '-', '_', '?', '=', '%', '&', '@', '#', '+', ';', ':', ',', '/', '!' -> true
            else -> false
        }
    }

    private fun extractBasicLexicalFeatures(rawInput: String, parsed: ParsedUrl): BasicLexicalFeatures {
        val lexicalTarget = buildString {
            append(parsed.host)
            append(parsed.path)
            if (parsed.query.isNotBlank()) {
                append('?')
                append(parsed.query)
            }
        }

        var dots = 0
        var specials = 0
        for (ch in lexicalTarget) {
            if (ch == '.') dots++
            if (isLexicalSpecialChar(ch)) specials++
        }

        val length = lexicalTarget.length
        val density = if (length == 0) 0.0 else specials.toDouble() / length

        val normalizedRaw = rawInput.trim().lowercase()
        val hasExplicitScheme = normalizedRaw.contains("://")
        val usesNonHttps = normalizedRaw.startsWith("http://") ||
            (hasExplicitScheme && !normalizedRaw.startsWith("https://"))

        val decodedPath = decodeUrlComponent(parsed.path).lowercase()
        val pathSegments = decodedPath.split('/').filter { it.isNotBlank() }

        val hasScriptEndpoint = decodedPath.endsWith(".php") ||
            decodedPath.endsWith(".asp") ||
            decodedPath.endsWith(".aspx") ||
            decodedPath.endsWith(".jsp") ||
            decodedPath.endsWith(".cgi")

        // Flags tokens like nm0m1d4 that combine letters and digits and are uncommon in normal links.
        val randomAlphaNumRegex = Regex("^(?=.*[a-z])(?=.*\\d)[a-z\\d._-]{7,}$")
        val hasRandomAlphaNumSegment = pathSegments.any { segment ->
            randomAlphaNumRegex.matches(segment)
        }

        return BasicLexicalFeatures(
            urlLength = length,
            dotCount = dots,
            specialCharCount = specials,
            specialCharDensity = density,
            usesNonHttps = usesNonHttps,
            hasScriptEndpoint = hasScriptEndpoint,
            hasRandomAlphaNumSegment = hasRandomAlphaNumSegment
        )
    }

    private fun scoreBasicLexicalFeatures(
        features: BasicLexicalFeatures,
        reasons: MutableSet<String>
    ): Int {
        var points = 0

        when {
            features.urlLength >= 120 -> {
                points += 16
                reasons.add("URL is unusually long")
            }
            features.urlLength >= 90 -> {
                points += 10
                reasons.add("URL length is above normal")
            }
            features.urlLength >= 75 -> {
                points += 5
                reasons.add("URL length is moderately high")
            }
        }

        when {
            features.dotCount >= 6 -> {
                points += 10
                reasons.add("URL has many dot segments")
            }
            features.dotCount >= 4 -> {
                points += 6
                reasons.add("URL has multiple dot segments")
            }
        }

        when {
            features.specialCharCount >= 14 -> {
                points += 12
                reasons.add("URL contains many special characters")
            }
            features.specialCharCount >= 8 -> {
                points += 7
                reasons.add("URL contains several special characters")
            }
        }

        when {
            features.specialCharDensity >= 0.28 -> {
                points += 14
                reasons.add("URL has high special character density")
            }
            features.specialCharDensity >= 0.18 -> {
                points += 8
                reasons.add("URL has moderate special character density")
            }
        }

        if (features.usesNonHttps) {
            points += 12
            reasons.add("Uses non-HTTPS scheme")
        }

        if (features.hasScriptEndpoint) {
            points += 6
            reasons.add("Targets dynamic script endpoint")
        }

        if (features.hasRandomAlphaNumSegment) {
            points += 5
            reasons.add("Contains randomized path token")
        }

        if (features.hasScriptEndpoint && features.hasRandomAlphaNumSegment) {
            points += 4
            reasons.add("Script endpoint + obfuscated path pattern")
        }

        return points
    }

    private fun decodeUrlComponent(value: String): String {
        var decoded = value
        repeat(2) {
            decoded = try {
                URLDecoder.decode(decoded, StandardCharsets.UTF_8.name())
            } catch (_: Exception) {
                decoded
            }
        }
        return decoded
    }

    private fun isShortenerDomain(domain: String): Boolean {
        return shortenerDomains.any { domain == it || domain.endsWith(".$it") }
    }

    private fun logToDatabase(
        context: Context,
        rawInput: String,
        normalizedDomain: String,
        report: AnalysisReport,
        source: String
    ) {
        GlobalScope.launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val repo = com.zerothreat.core.data.repository.UrlRepository(db.urlDao())

                repo.logScan(
                    url = rawInput,
                    domain = normalizedDomain,
                    result = report.result,
                    source = source,
                    threatType = if (report.result == PhishingResult.PHISHING) "Phishing" else null,
                    phishingScore = report.score,
                    analysisNote = report.description
                )
                Log.d("PHISH_DB", "Saved to DB: $normalizedDomain (${report.score}%)")
            } catch (e: Exception) {
                Log.e("PhishingDetector", "Failed to log to DB", e)
            }
        }
    }

    private fun decodePunycodeLabels(labels: List<String>): List<String> {
        return labels.map { label ->
            if (label.startsWith("xn--")) {
                try {
                    IDN.toUnicode(label, 0)
                } catch (_: Exception) {
                    label
                }
            } else {
                label
            }
        }
    }

    private fun hasHomographChars(decodedLabels: List<String>): Boolean {
        return decodedLabels.any { label ->
            val hasAsciiLetter = label.any { it in 'a'..'z' }
            val hasNonAsciiLetter = label.any { it.code > 127 && it.isLetter() }
            label.any { it.isHighSurrogate() || it.isLowSurrogate() || confusableChars.contains(it.lowercaseChar()) } ||
                (hasAsciiLetter && hasNonAsciiLetter)
        }
    }

    private fun isAllowlisted(domain: String): Boolean {
        return allowlist.any { domain == it || domain.endsWith(".$it") }
    }

}
