package com.saferescue.app.core.location

interface LocationRepository {
    fun start(onPoint: (LocationPoint) -> Unit, onStatus: (LocationStatus, String?) -> Unit, updateIntervalMillis: Long = 10_000L): Boolean
    fun stop()
}
