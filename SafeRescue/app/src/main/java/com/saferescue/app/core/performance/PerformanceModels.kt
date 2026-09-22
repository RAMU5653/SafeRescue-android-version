package com.saferescue.app.core.performance

enum class PerformanceMode {
    NORMAL,
    POWER_SAVER,
    CRITICAL_BATTERY,
    THERMAL_CONSTRAINED
}

data class PerformanceSnapshot(
    val batteryPercent: Int,
    val isCharging: Boolean,
    val powerSaveMode: Boolean,
    val thermalStatus: Int
)

data class PerformancePolicy(
    val mode: PerformanceMode,
    val voiceMetricIntervalMillis: Long,
    val riskRecalculationIntervalMillis: Long
) {
    companion object {
        fun from(snapshot: PerformanceSnapshot): PerformancePolicy {
            val thermalConstrained = snapshot.thermalStatus >= android.os.PowerManager.THERMAL_STATUS_MODERATE
            return when {
                snapshot.batteryPercent <= 15 && !snapshot.isCharging ->
                    PerformancePolicy(PerformanceMode.CRITICAL_BATTERY, 2_000L, 2_000L)
                thermalConstrained ->
                    PerformancePolicy(PerformanceMode.THERMAL_CONSTRAINED, 1_500L, 1_500L)
                snapshot.powerSaveMode || (snapshot.batteryPercent <= 30 && !snapshot.isCharging) ->
                    PerformancePolicy(PerformanceMode.POWER_SAVER, 1_000L, 1_000L)
                else ->
                    PerformancePolicy(PerformanceMode.NORMAL, 500L, 500L)
            }
        }
    }
}
