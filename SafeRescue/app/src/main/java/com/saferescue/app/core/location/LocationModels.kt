package com.saferescue.app.core.location

data class LocationPoint(
    val latitude: Double,
    val longitude: Double,
    val timestampMillis: Long,
    val accuracyMeters: Float
)

enum class LocationStatus {
    INACTIVE,
    PERMISSION_REQUIRED,
    ACTIVE,
    UNAVAILABLE,
    ERROR
}

data class LocationSnapshot(
    val status: LocationStatus = LocationStatus.INACTIVE,
    val latestPoint: LocationPoint? = null,
    val message: String? = null
)
