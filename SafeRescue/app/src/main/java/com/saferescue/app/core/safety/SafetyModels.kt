package com.saferescue.app.core.safety

data class SafePlace(
    val name: String,
    val category: SafePlaceCategory,
    val latitude: Double,
    val longitude: Double,
    val distanceMeters: Double,
    val source: String
)

enum class SafePlaceCategory(val label: String) {
    POLICE("Police"),
    HOSPITAL("Hospital"),
    FIRE_STATION("Fire station"),
    PUBLIC_PLACE("Mapped public place")
}

data class WeatherSnapshot(
    val temperatureC: Double,
    val apparentTemperatureC: Double,
    val precipitationMm: Double,
    val weatherCode: Int,
    val windSpeedKmh: Double,
    val observedAt: String,
    val source: String = "Open-Meteo"
)

data class SafetyGuidanceSnapshot(
    val places: List<SafePlace> = emptyList(),
    val weather: WeatherSnapshot? = null,
    val loading: Boolean = false,
    val error: String? = null,
    val loadedAtMillis: Long? = null
)
