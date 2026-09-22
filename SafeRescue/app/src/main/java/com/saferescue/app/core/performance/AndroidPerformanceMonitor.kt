package com.saferescue.app.core.performance

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import android.os.SystemClock

/**
 * Reads system power/thermal state without requiring special permissions.
 *
 * The snapshot is briefly cached because the voice pipeline can ask for the
 * current policy many times per second. This reduces repeated framework calls
 * while keeping emergency performance decisions responsive.
 */
class AndroidPerformanceMonitor(context: Context) {
    private val appContext = context.applicationContext

    @Volatile private var cachedSnapshot: PerformanceSnapshot? = null
    @Volatile private var cachedAtElapsedMillis = 0L

    fun snapshot(): PerformanceSnapshot {
        val now = SystemClock.elapsedRealtime()
        val cached = cachedSnapshot
        if (cached != null && now - cachedAtElapsedMillis < SNAPSHOT_CACHE_MILLIS) {
            return cached
        }

        val fresh = readSnapshot()
        cachedSnapshot = fresh
        cachedAtElapsedMillis = now
        return fresh
    }

    fun policy(): PerformancePolicy = PerformancePolicy.from(snapshot())

    /** Forces the next policy read to query Android again. */
    fun invalidateCache() {
        cachedSnapshot = null
        cachedAtElapsedMillis = 0L
    }

    private fun readSnapshot(): PerformanceSnapshot {
        val batteryIntent = appContext.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
        val percent = if (level >= 0 && scale > 0) {
            ((level * 100f) / scale).toInt().coerceIn(0, 100)
        } else {
            100
        }
        val chargingState = batteryIntent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val charging = chargingState == BatteryManager.BATTERY_STATUS_CHARGING ||
            chargingState == BatteryManager.BATTERY_STATUS_FULL
        val powerManager = appContext.getSystemService(PowerManager::class.java)
        val powerSave = powerManager?.isPowerSaveMode == true
        val thermal = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            powerManager?.currentThermalStatus ?: PowerManager.THERMAL_STATUS_NONE
        } else {
            PowerManager.THERMAL_STATUS_NONE
        }
        return PerformanceSnapshot(percent, charging, powerSave, thermal)
    }

    private companion object {
        const val SNAPSHOT_CACHE_MILLIS = 2_000L
    }
}
