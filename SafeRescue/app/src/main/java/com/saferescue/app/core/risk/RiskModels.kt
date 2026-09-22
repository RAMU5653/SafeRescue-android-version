package com.saferescue.app.core.risk

import com.saferescue.app.core.location.LocationStatus
import com.saferescue.app.core.voice.VoiceSignalMetrics

/** Risk is an auditable heuristic signal, never proof of danger or a replacement for manual SOS. */
data class RiskInput(
    val emergencyActive: Boolean,
    val locationStatus: LocationStatus,
    val accuracyMeters: Float?,
    val voiceMetrics: VoiceSignalMetrics?
)

data class RiskFactor(
    val id: String,
    val label: String,
    val points: Int,
    val explanation: String
)

enum class RiskSeverity { LOW, MEDIUM, HIGH, CRITICAL }

data class RiskSnapshot(
    val score: Int = 0,
    val severity: RiskSeverity = RiskSeverity.LOW,
    val factors: List<RiskFactor> = emptyList(),
    val updatedAtMillis: Long = 0L,
    val isHeuristic: Boolean = true
)
