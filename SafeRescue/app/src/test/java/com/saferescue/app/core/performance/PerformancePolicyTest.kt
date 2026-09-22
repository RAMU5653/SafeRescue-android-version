package com.saferescue.app.core.performance

import android.os.PowerManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PerformancePolicyTest {
    @Test fun normalModeKeepsFrequentOptionalProcessing() {
        val policy = PerformancePolicy.from(PerformanceSnapshot(80, true, false, PowerManager.THERMAL_STATUS_NONE))
        assertEquals(PerformanceMode.NORMAL, policy.mode)
        assertTrue(policy.voiceMetricIntervalMillis <= 500L)
        assertTrue(policy.riskRecalculationIntervalMillis <= 500L)
    }

    @Test fun powerSaverThrottlesOptionalWorkNotEmergencyCapture() {
        val policy = PerformancePolicy.from(PerformanceSnapshot(25, false, true, PowerManager.THERMAL_STATUS_NONE))
        assertEquals(PerformanceMode.POWER_SAVER, policy.mode)
        assertEquals(1_000L, policy.voiceMetricIntervalMillis)
        assertEquals(1_000L, policy.riskRecalculationIntervalMillis)
    }

    @Test fun criticalBatteryUsesSlowestOptionalCadence() {
        val policy = PerformancePolicy.from(PerformanceSnapshot(10, false, false, PowerManager.THERMAL_STATUS_NONE))
        assertEquals(PerformanceMode.CRITICAL_BATTERY, policy.mode)
        assertEquals(2_000L, policy.voiceMetricIntervalMillis)
        assertEquals(2_000L, policy.riskRecalculationIntervalMillis)
    }

    @Test fun thermalConstraintTakesPriorityOverNormalMode() {
        val policy = PerformancePolicy.from(PerformanceSnapshot(90, true, false, PowerManager.THERMAL_STATUS_MODERATE))
        assertEquals(PerformanceMode.THERMAL_CONSTRAINED, policy.mode)
    }

    @Test fun everyPolicyHasPositiveCadences() {
        val snapshots = listOf(
            PerformanceSnapshot(100, true, false, PowerManager.THERMAL_STATUS_NONE),
            PerformanceSnapshot(30, false, true, PowerManager.THERMAL_STATUS_NONE),
            PerformanceSnapshot(15, false, false, PowerManager.THERMAL_STATUS_NONE),
            PerformanceSnapshot(90, true, false, PowerManager.THERMAL_STATUS_SEVERE)
        )
        snapshots.forEach { snapshot ->
            val policy = PerformancePolicy.from(snapshot)
            assertTrue(policy.voiceMetricIntervalMillis > 0L)
            assertTrue(policy.riskRecalculationIntervalMillis > 0L)
        }
    }
}
