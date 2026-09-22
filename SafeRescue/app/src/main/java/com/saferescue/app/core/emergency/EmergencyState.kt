package com.saferescue.app.core.emergency

/** Durable emergency lifecycle states. UI must observe this state rather than own emergency booleans. */
enum class EmergencyState {
    IDLE,
    SOS_HOLDING,
    EMERGENCY_ACTIVE,
    MONITORING,
    CANCELLATION_PENDING,
    CANCELLED,
    CONFIRMED,
    EVIDENCE_PRESERVATION,
    UPLOADING,
    QUEUED_OFFLINE,
    SYNCING,
    COMPLETED,
    FAILED_RECOVERABLE,
    FAILED_FATAL
}
