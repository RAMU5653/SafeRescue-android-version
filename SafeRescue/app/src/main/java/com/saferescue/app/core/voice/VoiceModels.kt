package com.saferescue.app.core.voice

enum class VoiceStatus { INACTIVE, PERMISSION_REQUIRED, STARTING, LISTENING, UNAVAILABLE, ERROR, STOPPED }

data class VoiceSignalMetrics(
    val timestampMillis: Long,
    val durationMillis: Long,
    val rmsDb: Float,
    val peakAmplitude: Int,
    val bytesCaptured: Long
)

data class VoiceSnapshot(
    val status: VoiceStatus = VoiceStatus.INACTIVE,
    val metrics: VoiceSignalMetrics? = null,
    val temporaryPath: String? = null,
    val message: String? = null
)
