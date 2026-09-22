package com.saferescue.app.core.timeline

/** Process-wide bridge used only to render the current emergency timeline in Phase 11. */
object TimelineStore : TimelineRepository by InMemoryTimelineRepository()
