package com.saferescue.app.core.emergency

/**
 * Single owner of emergency lifecycle transitions.
 *
 * Manual SOS is never evaluated against AI/sensor confidence. Those signals will be
 * consumers of the emergency state in later phases, not gates for activation.
 */
class EmergencyManager(private val repository: EmergencyRepository) {

    @Synchronized
    fun state(): EmergencyState = repository.readState()

    @Synchronized
    fun dispatch(event: EmergencyEvent): Result<EmergencyState> {
        val current = repository.readState()
        val next = transition(current, event)
            ?: return Result.failure(IllegalStateException("Invalid emergency transition: $current + $event"))
        repository.writeState(next)
        return Result.success(next)
    }

    private fun transition(state: EmergencyState, event: EmergencyEvent): EmergencyState? = when (state) {
        EmergencyState.IDLE -> when (event) {
            EmergencyEvent.BeginSosHold -> EmergencyState.SOS_HOLDING
            else -> null
        }

        EmergencyState.SOS_HOLDING -> when (event) {
            EmergencyEvent.SosHoldCompleted -> EmergencyState.EMERGENCY_ACTIVE
            EmergencyEvent.Reset -> EmergencyState.IDLE
            else -> null
        }

        EmergencyState.EMERGENCY_ACTIVE -> when (event) {
            EmergencyEvent.StartMonitoring -> EmergencyState.MONITORING
            EmergencyEvent.BeginCancellation -> EmergencyState.CANCELLATION_PENDING
            EmergencyEvent.ConfirmEmergency -> EmergencyState.CONFIRMED
            EmergencyEvent.FatalFailure -> EmergencyState.FAILED_FATAL
            else -> null
        }

        EmergencyState.MONITORING -> when (event) {
            EmergencyEvent.BeginCancellation -> EmergencyState.CANCELLATION_PENDING
            EmergencyEvent.ConfirmEmergency -> EmergencyState.CONFIRMED
            EmergencyEvent.FatalFailure -> EmergencyState.FAILED_FATAL
            else -> null
        }

        EmergencyState.CANCELLATION_PENDING -> when (event) {
            EmergencyEvent.CancelHoldCompleted -> EmergencyState.CANCELLED
            EmergencyEvent.CancelHoldAborted -> EmergencyState.MONITORING
            EmergencyEvent.FatalFailure -> EmergencyState.FAILED_FATAL
            else -> null
        }

        EmergencyState.CANCELLED -> when (event) {
            EmergencyEvent.Reset -> EmergencyState.IDLE
            else -> null
        }

        EmergencyState.CONFIRMED -> when (event) {
            EmergencyEvent.BeginEvidencePreservation -> EmergencyState.EVIDENCE_PRESERVATION
            EmergencyEvent.FatalFailure -> EmergencyState.FAILED_FATAL
            else -> null
        }

        EmergencyState.EVIDENCE_PRESERVATION -> when (event) {
            EmergencyEvent.UploadOnline -> EmergencyState.UPLOADING
            EmergencyEvent.QueueOffline -> EmergencyState.QUEUED_OFFLINE
            EmergencyEvent.RecoverableFailure -> EmergencyState.FAILED_RECOVERABLE
            EmergencyEvent.FatalFailure -> EmergencyState.FAILED_FATAL
            else -> null
        }

        EmergencyState.UPLOADING -> when (event) {
            EmergencyEvent.BeginSync -> EmergencyState.SYNCING
            EmergencyEvent.SyncCompleted -> EmergencyState.COMPLETED
            EmergencyEvent.QueueOffline -> EmergencyState.QUEUED_OFFLINE
            EmergencyEvent.RecoverableFailure -> EmergencyState.FAILED_RECOVERABLE
            EmergencyEvent.FatalFailure -> EmergencyState.FAILED_FATAL
            else -> null
        }

        EmergencyState.QUEUED_OFFLINE -> when (event) {
            EmergencyEvent.BeginSync -> EmergencyState.SYNCING
            EmergencyEvent.RecoverableFailure -> EmergencyState.FAILED_RECOVERABLE
            EmergencyEvent.FatalFailure -> EmergencyState.FAILED_FATAL
            else -> null
        }

        EmergencyState.SYNCING -> when (event) {
            EmergencyEvent.SyncCompleted -> EmergencyState.COMPLETED
            EmergencyEvent.RecoverableFailure -> EmergencyState.FAILED_RECOVERABLE
            EmergencyEvent.QueueOffline -> EmergencyState.QUEUED_OFFLINE
            EmergencyEvent.FatalFailure -> EmergencyState.FAILED_FATAL
            else -> null
        }

        EmergencyState.COMPLETED -> when (event) {
            EmergencyEvent.Reset -> EmergencyState.IDLE
            else -> null
        }

        EmergencyState.FAILED_RECOVERABLE -> when (event) {
            EmergencyEvent.BeginSync -> EmergencyState.SYNCING
            EmergencyEvent.QueueOffline -> EmergencyState.QUEUED_OFFLINE
            EmergencyEvent.Reset -> EmergencyState.IDLE
            EmergencyEvent.FatalFailure -> EmergencyState.FAILED_FATAL
            else -> null
        }

        EmergencyState.FAILED_FATAL -> when (event) {
            EmergencyEvent.Reset -> EmergencyState.IDLE
            else -> null
        }
    }
}
