package com.saferescue.app

import com.saferescue.app.core.offline.OfflineQueueItem
import org.junit.Assert.assertEquals
import org.junit.Test

class OfflineQueueModelTest {
    @Test fun itemKeepsStableIdempotencyKey() {
        val item = OfflineQueueItem("id", "incident", "stable-key", "{}", createdAtMillis = 1L)
        assertEquals("stable-key", item.idempotencyKey)
    }
}
