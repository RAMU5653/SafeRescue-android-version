package com.saferescue.app.core.timeline

import java.util.ArrayDeque

interface TimelineRepository {
    fun append(event: TimelineEvent)
    fun snapshot(): List<TimelineEvent>
    fun clear()
}

/** Process-local timeline for Phase 11. Durable incident persistence is intentionally deferred. */
class InMemoryTimelineRepository : TimelineRepository {
    private val events = ArrayDeque<TimelineEvent>()
    private val lock = Any()

    override fun append(event: TimelineEvent) {
        synchronized(lock) {
            events.addLast(event)
            while (events.size > MAX_EVENTS) events.removeFirst()
        }
    }

    override fun snapshot(): List<TimelineEvent> = synchronized(lock) { events.toList() }
    override fun clear() = synchronized(lock) { events.clear() }

    companion object { private const val MAX_EVENTS = 500 }
}
