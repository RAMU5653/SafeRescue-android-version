package com.saferescue.app

import com.saferescue.app.core.emergency.EmergencyEvent
import com.saferescue.app.core.emergency.EmergencyManager
import com.saferescue.app.core.emergency.EmergencyState
import com.saferescue.app.core.emergency.InMemoryEmergencyRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EmergencyManagerTest {
    private fun manager() = EmergencyManager(InMemoryEmergencyRepository())

    @Test fun manualSosTransitionsToActiveWithoutAiGate() {
        val manager = manager()
        assertEquals(EmergencyState.IDLE, manager.state())
        assertEquals(EmergencyState.SOS_HOLDING, manager.dispatch(EmergencyEvent.BeginSosHold).getOrThrow())
        assertEquals(EmergencyState.EMERGENCY_ACTIVE, manager.dispatch(EmergencyEvent.SosHoldCompleted).getOrThrow())
    }

    @Test fun monitoringCanBeCancelledOnlyThroughCancellationState() {
        val manager = manager()
        manager.dispatch(EmergencyEvent.BeginSosHold)
        manager.dispatch(EmergencyEvent.SosHoldCompleted)
        manager.dispatch(EmergencyEvent.StartMonitoring)
        manager.dispatch(EmergencyEvent.BeginCancellation)
        assertEquals(EmergencyState.CANCELLATION_PENDING, manager.state())
        assertEquals(EmergencyState.CANCELLED, manager.dispatch(EmergencyEvent.CancelHoldCompleted).getOrThrow())
    }

    @Test fun cancelledEmergencyCannotUpload() {
        val manager = manager()
        manager.dispatch(EmergencyEvent.BeginSosHold)
        manager.dispatch(EmergencyEvent.SosHoldCompleted)
        manager.dispatch(EmergencyEvent.BeginCancellation)
        manager.dispatch(EmergencyEvent.CancelHoldCompleted)
        assertTrue(manager.dispatch(EmergencyEvent.UploadOnline).isFailure)
        assertEquals(EmergencyState.CANCELLED, manager.state())
    }

    @Test fun confirmedEmergencyCanReachCompletedOnlinePath() {
        val manager = manager()
        manager.dispatch(EmergencyEvent.BeginSosHold)
        manager.dispatch(EmergencyEvent.SosHoldCompleted)
        manager.dispatch(EmergencyEvent.ConfirmEmergency)
        manager.dispatch(EmergencyEvent.BeginEvidencePreservation)
        manager.dispatch(EmergencyEvent.UploadOnline)
        manager.dispatch(EmergencyEvent.BeginSync)
        assertEquals(EmergencyState.COMPLETED, manager.dispatch(EmergencyEvent.SyncCompleted).getOrThrow())
    }

    @Test fun confirmedEmergencyCanQueueOfflineAndLaterSync() {
        val manager = manager()
        manager.dispatch(EmergencyEvent.BeginSosHold)
        manager.dispatch(EmergencyEvent.SosHoldCompleted)
        manager.dispatch(EmergencyEvent.ConfirmEmergency)
        manager.dispatch(EmergencyEvent.BeginEvidencePreservation)
        manager.dispatch(EmergencyEvent.QueueOffline)
        assertEquals(EmergencyState.QUEUED_OFFLINE, manager.state())
        manager.dispatch(EmergencyEvent.BeginSync)
        assertEquals(EmergencyState.COMPLETED, manager.dispatch(EmergencyEvent.SyncCompleted).getOrThrow())
    }

    @Test fun invalidTransitionsDoNotMutateState() {
        val manager = manager()
        assertTrue(manager.dispatch(EmergencyEvent.ConfirmEmergency).isFailure)
        assertEquals(EmergencyState.IDLE, manager.state())
    }
}
