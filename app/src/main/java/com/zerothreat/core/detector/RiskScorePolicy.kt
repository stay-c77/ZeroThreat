package com.zerothreat.core.detector

import kotlin.math.exp
import kotlin.math.roundToInt

/**
 * Central scoring policy so all entry points use the same calibrated percentages.
 */
object RiskScorePolicy {
    const val INVALID_URL = 18
    const val DNS_DOMAIN_NOT_FOUND = 22
    const val DNS_LOOKUP_ERROR = 30

    /**
     * Converts a raw additive heuristic score into a smoother 0-100 risk percentage.
     */
    fun normalizeHeuristicScore(
        rawScore: Int,
        phishingSignalCount: Int,
        hasCriticalSignal: Boolean
    ): Int {
        val boundedRaw = rawScore.coerceIn(0, 180)
        var calibrated = (100.0 / (1.0 + exp(-0.055 * (boundedRaw - 52)))).roundToInt()

        calibrated = when {
            hasCriticalSignal -> maxOf(calibrated, 85)
            phishingSignalCount >= 3 -> maxOf(calibrated, 70)
            phishingSignalCount <= 1 -> (calibrated * 0.82).roundToInt()
            else -> calibrated
        }

        return calibrated.coerceIn(0, 100)
    }
}

