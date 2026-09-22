package com.saferescue.app

import com.saferescue.app.core.location.LocationPoint
import com.saferescue.app.core.location.LocationStatus
import org.junit.Assert.assertEquals
import org.junit.Test

class LocationModelTest {
    @Test
    fun locationPoint_preservesCoreFields() {
        val point = LocationPoint(17.3850, 78.4867, 1_700_000_000_000L, 8.5f)
        assertEquals(17.3850, point.latitude, 0.0)
        assertEquals(78.4867, point.longitude, 0.0)
        assertEquals(1_700_000_000_000L, point.timestampMillis)
        assertEquals(8.5f, point.accuracyMeters, 0.0f)
    }

    @Test
    fun locationStatus_hasExplicitFailureStates() {
        assertEquals(LocationStatus.PERMISSION_REQUIRED, LocationStatus.valueOf("PERMISSION_REQUIRED"))
        assertEquals(LocationStatus.UNAVAILABLE, LocationStatus.valueOf("UNAVAILABLE"))
    }
}
