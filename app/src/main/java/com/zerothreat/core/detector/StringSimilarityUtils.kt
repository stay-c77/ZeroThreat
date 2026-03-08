package com.zerothreat.core.detector

import kotlin.math.max
import kotlin.math.min

object StringSimilarityUtils {

    /**
     * Calculates the Levenshtein distance between two strings.
     * Represents the minimum number of single-character edits (insertions, deletions or substitutions)
     * required to change one word into the other.
     */
    fun levenshtein(s1: String, s2: String): Int {
        val len1 = s1.length
        val len2 = s2.length
        if (len1 == 0) return len2
        if (len2 == 0) return len1

        // Use two rows to save memory (O(min(len1, len2)))
        var cost = IntArray(len2 + 1) { it }
        var newCost = IntArray(len2 + 1)

        for (i in 1..len1) {
            newCost[0] = i
            for (j in 1..len2) {
                val match = if (s1[i - 1] == s2[j - 1]) 0 else 1
                val costReplace = cost[j - 1] + match
                val costInsert = cost[j] + 1
                val costDelete = newCost[j - 1] + 1
                newCost[j] = min(min(costInsert, costDelete), costReplace)
            }
            val temp = cost
            cost = newCost
            newCost = temp
        }
        return cost[len2]
    }

    /**
     * Calculates the Jaro-Winkler similarity between two strings.
     * Result is between 0.0 (no similarity) and 1.0 (exact match).
     * Heavily weights matching prefixes.
     */
    fun jaroWinkler(s1: String, s2: String): Double {
        val jaro = jaro(s1, s2)
        val prefixLength = commonPrefixLength(s1, s2)
        // Standard Jaro-Winkler constant is 0.1, max prefix length is 4
        return jaro + (0.1 * min(prefixLength, 4) * (1.0 - jaro))
    }

    private fun jaro(s1: String, s2: String): Double {
        if (s1 == s2) return 1.0
        val len1 = s1.length
        val len2 = s2.length
        if (len1 == 0 || len2 == 0) return 0.0

        val matchDistance = max(len1, len2) / 2 - 1
        val s1Matches = BooleanArray(len1)
        val s2Matches = BooleanArray(len2)

        var matches = 0
        var transpositions = 0

        for (i in 0 until len1) {
            val start = max(0, i - matchDistance)
            val end = min(len2 - 1, i + matchDistance)
            for (j in start..end) {
                if (s2Matches[j]) continue
                if (s1[i] != s2[j]) continue
                s1Matches[i] = true
                s2Matches[j] = true
                matches++
                break
            }
        }

        if (matches == 0) return 0.0

        var k = 0
        for (i in 0 until len1) {
            if (!s1Matches[i]) continue
            while (!s2Matches[k]) k++
            if (s1[i] != s2[k]) transpositions++
            k++
        }

        val m = matches.toDouble()
        return (m / len1 + m / len2 + (m - transpositions / 2.0) / m) / 3.0
    }

    private fun commonPrefixLength(s1: String, s2: String): Int {
        val n = min(s1.length, s2.length)
        for (i in 0 until n) {
            if (s1[i] != s2[i]) return i
        }
        return n
    }

    /**
     * Removes consecutive duplicate characters.
     * "nettttflic" -> "netflic"
     * "gooogle" -> "gogle"
     * Useful for normalizing domains for comparison.
     */
    fun deduplicateChars(input: String): String {
        if (input.isEmpty()) return input
        val sb = StringBuilder()
        sb.append(input[0])
        for (i in 1 until input.length) {
            if (input[i] != input[i - 1]) {
                sb.append(input[i])
            }
        }
        return sb.toString()
    }
}
