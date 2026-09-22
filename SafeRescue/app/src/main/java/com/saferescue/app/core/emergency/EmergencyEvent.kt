package com.saferescue.app.core.emergency

/** Events accepted by EmergencyManager. Sensor/network events are intentionally independent. */
sealed interface EmergencyEvent {
    data object BeginSosHold : EmergencyEvent
    data object SosHoldCompleted : EmergencyEvent
    data object StartMonitoring : EmergencyEvent
    data object BeginCancellation : EmergencyEvent
    data object CancelHoldCompleted : EmergencyEvent
    data object CancelHoldAborted : EmergencyEvent
    data object ConfirmEmergency : EmergencyEvent
    data object BeginEvidencePreservation : EmergencyEvent
    data object UploadOnline : EmergencyEvent
    data object QueueOffline : EmergencyEvent
    data object BeginSync : EmergencyEvent
    data object SyncCompleted : EmergencyEvent
    data object RecoverableFailure : EmergencyEvent
    data object FatalFailure : EmergencyEvent
    data object Reset : EmergencyEvent
}
