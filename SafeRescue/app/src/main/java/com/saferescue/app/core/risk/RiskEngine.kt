package com.saferescue.app.core.risk

import com.saferescue.app.core.location.LocationStatus
import kotlin.math.roundToInt

/**
 * Phase 9 local, deterministic risk engine.
 *
 * The score is intentionally conservative and transparent. It is not scientifically
 * validated and must never block, cancel, or downgrade a manual SOS.
 */
class RiskEngine {
    fun evaluate(input: RiskInput, nowMillis: Long = System.currentTimeMillis()): RiskSnapshot {
        if (!input.emergencyActive) return RiskSnapshot(updatedAtMillis = nowMillis)

        val factors = buildList {
            add(RiskFactor("manual_sos", "Manual SOS", 40, "A user-triggered emergency is active."))

            when (input.locationStatus) {
                LocationStatus.UNAVAILABLE, LocationStatus.ERROR ->
                    add(RiskFactor("location_unavailable", "Location unavailable", 5, "GPS/location signal is currently unavailable."))
                LocationStatus.PERMISSION_REQUIRED ->
                    add(RiskFactor("location_permission", "Location permission", 3, "Location permission is not available."))
                LocationStatus.ACTIVE -> {
                    val accuracy = input.accuracyMeters
                    if (accuracy != null && accuracy.isFinite() && accuracy > 100f) {
                        add(RiskFactor("location_accuracy", "Low location accuracy", 3, "Latest location accuracy is above 100 m."))
                    }
                }
                LocationStatus.INACTIVE -> Unit
            }

            input.voiceMetrics?.let { voice ->
                // Signal loudness is only an acoustic signal. It is deliberately not called distress.
                val loudnessPoints = when {
                    voice.rmsDb >= -12f -> 12
                    voice.rmsDb >= -20f -> 8
                    voice.rmsDb >= -35f -> 4
                    else -> 0
                }
                if (loudnessPoints > 0) {
                    add(RiskFactor("voice_signal", "Elevated voice signal", loudnessPoints, "Recent microphone signal is above the local heuristic threshold."))
                }
                val peakPoints = when {
                    voice.peakAmplitude >= 30000 -> 3
                    voice.peakAmplitude >= 22000 -> 2
                    voice.peakAmplitude >= 12000 -> 1
                    else -> 0
                }
                if (peakPoints > 0) {
                    add(RiskFactor("voice_peak", "Voice peak signal", peakPoints, "Recent microphone peak amplitude is elevated."))
                }
            }
        }

        val score = factors.sumOf { it.points }.coerceIn(0, 100)
        return RiskSnapshot(
            score = score,
            severity = severityFor(score),
            factors = factors,
            updatedAtMillis = nowMillis
        )
    }

    fun severityFor(score: Int): RiskSeverity = when (score.coerceIn(0, 100)) {
        in 0..24 -> RiskSeverity.LOW
        in 25..49 -> RiskSeverity.MEDIUM
        in 50..74 -> RiskSeverity.HIGH
        else -> RiskSeverity.CRITICAL
    }
}
