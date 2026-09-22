package com.saferescue.app

import com.saferescue.app.core.location.LocationStatus
import com.saferescue.app.core.risk.RiskEngine
import com.saferescue.app.core.risk.RiskInput
import com.saferescue.app.core.risk.RiskSeverity
import com.saferescue.app.core.voice.VoiceSignalMetrics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RiskEngineTest {
    private val engine = RiskEngine()

    @Test fun inactiveEmergencyReturnsZero() {
        val result = engine.evaluate(RiskInput(false, LocationStatus.INACTIVE, null, null), 1L)
        assertEquals(0, result.score)
        assertEquals(RiskSeverity.LOW, result.severity)
    }

    @Test fun manualSosIsAlwaysRepresented() {
        val result = engine.evaluate(RiskInput(true, LocationStatus.ACTIVE, 10f, null), 2L)
        assertEquals(40, result.score)
        assertEquals(RiskSeverity.MEDIUM, result.severity)
        assertTrue(result.factors.any { it.id == "manual_sos" })
    }

    @Test fun signalsRaiseScoreButStayBounded() {
        val voice = VoiceSignalMetrics(3L, 1000L, -8f, 32000, 64000L)
        val result = engine.evaluate(RiskInput(true, LocationStatus.ERROR, 250f, voice), 4L)
        assertTrue(result.score in 0..100)
        assertEquals(RiskSeverity.HIGH, result.severity)
    }

    @Test fun severityBoundariesMatchSpecification() {
        assertEquals(RiskSeverity.LOW, engine.severityFor(24))
        assertEquals(RiskSeverity.MEDIUM, engine.severityFor(25))
        assertEquals(RiskSeverity.HIGH, engine.severityFor(50))
        assertEquals(RiskSeverity.CRITICAL, engine.severityFor(75))
        assertEquals(RiskSeverity.CRITICAL, engine.severityFor(100))
    }
}
