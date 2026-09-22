package com.saferescue.app.core.timeline

data class TimelineEvent(
    val id: String,
    val timestampMillis: Long,
    val type: TimelineEventType,
    val title: String,
    val detail: String,
    val riskScore: Int? = null
)

enum class TimelineEventType {
    SOS_STARTED,
    STATE_CHANGED,
    LOCATION_UPDATE,
    VOICE_SIGNAL,
    RISK_UPDATED,
    CANCELLATION_STARTED,
    CANCELLATION_COMPLETED,
    EMERGENCY_CONFIRMED,
    SYSTEM
}
