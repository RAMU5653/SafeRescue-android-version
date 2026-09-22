package com.saferescue.app.core.offline

interface OfflineQueueRepository {
    fun enqueue(item: OfflineQueueItem): Boolean
    fun ready(nowMillis: Long = System.currentTimeMillis()): List<OfflineQueueItem>
    fun markRetryableFailure(id: String, error: String?, nowMillis: Long = System.currentTimeMillis()): Boolean
    fun markFatalFailure(id: String, error: String?): Boolean
    fun remove(id: String): Boolean
    fun snapshot(nowMillis: Long = System.currentTimeMillis()): QueueSnapshot
    fun clear()
}
