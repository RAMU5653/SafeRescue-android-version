package com.saferescue.app

import com.saferescue.app.core.voice.VoiceSignalMetrics
import com.saferescue.app.core.voice.VoiceSnapshot
import com.saferescue.app.core.voice.VoiceStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class VoiceModelTest {
    @Test fun defaultSnapshotIsInactive() {
        assertEquals(VoiceStatus.INACTIVE, VoiceSnapshot().status)
    }

    @Test fun metricsPreserveMeasuredValues() {
        val metrics = VoiceSignalMetrics(100L, 500L, -12.5f, 1200, 32000L)
        assertEquals(500L, metrics.durationMillis)
        assertEquals(-12.5f, metrics.rmsDb, 0.001f)
        assertEquals(1200, metrics.peakAmplitude)
    }
}
