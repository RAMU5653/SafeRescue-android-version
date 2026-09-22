package com.saferescue.app.core.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority

class AndroidLocationRepository(context: Context) : LocationRepository {
    private val appContext = context.applicationContext
    private val fused: FusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(appContext)
    private var callback: LocationCallback? = null

    @SuppressLint("MissingPermission")
    override fun start(onPoint: (LocationPoint) -> Unit, onStatus: (LocationStatus, String?) -> Unit, updateIntervalMillis: Long): Boolean {
        if (!hasLocationPermission()) {
            onStatus(LocationStatus.PERMISSION_REQUIRED, "Location permission is required for emergency location tracking.")
            return false
        }
        val locationManager = appContext.getSystemService(LocationManager::class.java)
        val enabled = locationManager?.isProviderEnabled(LocationManager.GPS_PROVIDER) == true ||
            locationManager?.isProviderEnabled(LocationManager.NETWORK_PROVIDER) == true
        if (!enabled) {
            onStatus(LocationStatus.UNAVAILABLE, "Location providers are turned off. Emergency continues without GPS.")
            return false
        }

        stop()
        val safeInterval = updateIntervalMillis.coerceIn(5_000L, 60_000L)
        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, safeInterval)
            .setMinUpdateIntervalMillis(safeInterval)
            .setWaitForAccurateLocation(false)
            .build()
        val cb = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.locations.forEach { location ->
                    onPoint(LocationPoint(location.latitude, location.longitude, location.time, location.accuracy))
                }
                onStatus(LocationStatus.ACTIVE, null)
            }
        }
        callback = cb
        return try {
            fused.requestLocationUpdates(request, cb, appContext.mainLooper)
            onStatus(LocationStatus.ACTIVE, "Emergency location monitoring active.")
            true
        } catch (_: SecurityException) {
            callback = null
            onStatus(LocationStatus.PERMISSION_REQUIRED, "Location permission was not available.")
            false
        } catch (_: Exception) {
            callback = null
            onStatus(LocationStatus.ERROR, "Location service is temporarily unavailable. Emergency continues.")
            false
        }
    }

    override fun stop() {
        callback?.let { fused.removeLocationUpdates(it) }
        callback = null
    }

    private fun hasLocationPermission(): Boolean =
        ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
}
