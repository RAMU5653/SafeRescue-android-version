package com.saferescue.app.core.safety

import com.saferescue.app.core.location.LocationPoint
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Phase 16 real-data adapters.
 *
 * Open-Meteo supplies weather data; OpenStreetMap Overpass supplies mapped nearby amenities.
 * These are guidance sources, not emergency dispatch or safety certification services.
 */
class RemoteSafetyDataRepository : SafetyDataRepository {
    override suspend fun nearbySafePlaces(location: LocationPoint, radiusMeters: Int): List<SafePlace> =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            require(radiusMeters in 250..5000) { "Unsupported search radius" }
            val query = """
                [out:json][timeout:15];
                (
                  nwr[amenity=police](around:$radiusMeters,${location.latitude},${location.longitude});
                  nwr[amenity=hospital](around:$radiusMeters,${location.latitude},${location.longitude});
                  nwr[amenity=fire_station](around:$radiusMeters,${location.latitude},${location.longitude});
                  nwr[amenity=community_centre](around:$radiusMeters,${location.latitude},${location.longitude});
                  nwr[amenity=library](around:$radiusMeters,${location.latitude},${location.longitude});
                );
                out center tags;
            """.trimIndent()
            val url = "https://overpass-api.de/api/interpreter?data=" + URLEncoder.encode(query, "UTF-8")
            val root = getJson(url)
            val elements = root.optJSONArray("elements") ?: return@withContext emptyList()
            val result = mutableListOf<SafePlace>()
            for (i in 0 until elements.length()) {
                val item = elements.optJSONObject(i) ?: continue
                val tags = item.optJSONObject("tags") ?: JSONObject()
                val amenity = tags.optString("amenity")
                val category = when (amenity) {
                    "police" -> SafePlaceCategory.POLICE
                    "hospital" -> SafePlaceCategory.HOSPITAL
                    "fire_station" -> SafePlaceCategory.FIRE_STATION
                    "community_centre", "library" -> SafePlaceCategory.PUBLIC_PLACE
                    else -> continue
                }
                val center = item.optJSONObject("center")
                val lat = if (item.has("lat")) item.optDouble("lat", Double.NaN) else center?.optDouble("lat", Double.NaN) ?: Double.NaN
                val lon = if (item.has("lon")) item.optDouble("lon", Double.NaN) else center?.optDouble("lon", Double.NaN) ?: Double.NaN
                if (!lat.isFinite() || !lon.isFinite()) continue
                result += SafePlace(
                    name = tags.optString("name").ifBlank { category.label },
                    category = category,
                    latitude = lat,
                    longitude = lon,
                    distanceMeters = haversineMeters(location.latitude, location.longitude, lat, lon),
                    source = "OpenStreetMap"
                )
            }
            result.distinctBy { "${it.category}:${it.name.lowercase(Locale.US)}:${it.latitude}:${it.longitude}" }
                .sortedWith(compareBy<SafePlace> { it.distanceMeters }.thenBy { it.category.ordinal })
                .take(10)
        }

    override suspend fun currentWeather(location: LocationPoint): WeatherSnapshot =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            val url = buildString {
                append("https://api.open-meteo.com/v1/forecast?")
                append("latitude=").append(location.latitude)
                append("&longitude=").append(location.longitude)
                append("&current=temperature_2m,apparent_temperature,precipitation,weather_code,wind_speed_10m")
                append("&timezone=auto")
            }
            val current = getJson(url).optJSONObject("current")
                ?: error("Weather response did not contain current conditions")
            WeatherSnapshot(
                temperatureC = current.optDouble("temperature_2m", Double.NaN),
                apparentTemperatureC = current.optDouble("apparent_temperature", Double.NaN),
                precipitationMm = current.optDouble("precipitation", Double.NaN),
                weatherCode = current.optInt("weather_code", -1),
                windSpeedKmh = current.optDouble("wind_speed_10m", Double.NaN),
                observedAt = current.optString("time").ifBlank { "unknown" }
            )
        }

    private fun getJson(urlString: String): JSONObject {
        val connection = (URL(urlString).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 8000
            readTimeout = 12000
            setRequestProperty("Accept", "application/json")
            setRequestProperty("User-Agent", "SafeRescue/0.10 Phase16")
            instanceFollowRedirects = false
        }
        try {
            val code = connection.responseCode
            if (code !in 200..299) error("Remote safety data unavailable (HTTP $code)")
            val body = BufferedReader(InputStreamReader(connection.inputStream, Charsets.UTF_8)).use { it.readText() }
            require(body.length <= 1_500_000) { "Remote response exceeded safety limit" }
            return JSONObject(body)
        } finally {
            connection.disconnect()
        }
    }

    private fun haversineMeters(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earth = 6_371_000.0
        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)
        val a = sin(dLat / 2).pow(2.0) + cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) * sin(dLon / 2).pow(2.0)
        return 2 * earth * atan2(sqrt(a), sqrt(1 - a))
    }
}
