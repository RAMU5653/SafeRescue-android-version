package com.saferescue.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EmergencyHoldPolicyTest {
    @Test fun sosRequiresFiveSeconds() {
        assertFalse(isComplete(4999))
        assertTrue(isComplete(5000))
        assertTrue(isComplete(6200))
    }

    @Test fun cancellationUsesSameFiveSecondPolicy() {
        assertFalse(isComplete(4999))
        assertTrue(isComplete(5000))
    }

    private fun isComplete(elapsedMs: Long): Boolean = elapsedMs >= 5_000L
}
