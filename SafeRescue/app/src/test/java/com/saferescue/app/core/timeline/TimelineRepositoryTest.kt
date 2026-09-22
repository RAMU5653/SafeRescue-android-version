package com.saferescue.app.core.timeline

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TimelineRepositoryTest {
    @Test fun appendPreservesChronologicalInsertionOrder() {
        val repo = InMemoryTimelineRepository()
        repo.append(TimelineEvent("1", 1000L, TimelineEventType.SOS_STARTED, "SOS", "Started"))
        repo.append(TimelineEvent("2", 2000L, TimelineEventType.LOCATION_UPDATE, "GPS", "Fix"))
        assertEquals(listOf("1", "2"), repo.snapshot().map { it.id })
    }

    @Test fun repositoryBoundsMemory() {
        val repo = InMemoryTimelineRepository()
        repeat(501) { i -> repo.append(TimelineEvent(i.toString(), i.toLong(), TimelineEventType.SYSTEM, "Event", "Test")) }
        assertEquals(500, repo.snapshot().size)
        assertTrue(repo.snapshot().first().id == "1")
    }

    @Test fun clearRemovesSensitiveTimelineEntries() {
        val repo = InMemoryTimelineRepository()
        repo.append(TimelineEvent("1", 1000L, TimelineEventType.LOCATION_UPDATE, "GPS", "Fix"))
        repo.clear()
        assertTrue(repo.snapshot().isEmpty())
    }
}
