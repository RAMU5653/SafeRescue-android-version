package com.saferescue.app.core.voice

interface VoiceRepository {
    fun start(onMetrics: (VoiceSignalMetrics) -> Unit, onStatus: (VoiceStatus, String?) -> Unit): Boolean
    fun stop()
}
