package com.saferescue.app.core.emergency

/** Persistence boundary. Phase 4 uses an in-memory implementation; Room arrives in the database phase. */
interface EmergencyRepository {
    fun readState(): EmergencyState
    fun writeState(state: EmergencyState)
}

class InMemoryEmergencyRepository(initialState: EmergencyState = EmergencyState.IDLE) : EmergencyRepository {
    private var state = initialState

    @Synchronized
    override fun readState(): EmergencyState = state

    @Synchronized
    override fun writeState(state: EmergencyState) {
        this.state = state
    }
}
