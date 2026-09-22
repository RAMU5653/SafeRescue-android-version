package com.saferescue.app.core.safety

import com.saferescue.app.core.location.LocationPoint

interface SafetyDataRepository {
    suspend fun nearbySafePlaces(location: LocationPoint, radiusMeters: Int = 3000): List<SafePlace>
    suspend fun currentWeather(location: LocationPoint): WeatherSnapshot
}
