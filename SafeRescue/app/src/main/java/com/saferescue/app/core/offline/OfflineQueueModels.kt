package com.saferescue.app.core.offline

data class OfflineQueueItem(
    val id: String,
    val incidentId: String,
    val idempotencyKey: String,
    val payload: String,
    val evidencePaths: List<String> = emptyList(),
    val createdAtMillis: Long,
    val retryCount: Int = 0,
    val nextAttemptAtMillis: Long = createdAtMillis
)

enum class QueueStatus { EMPTY, QUEUED, RETRY_WAIT, READY, FAILED_RETRYABLE, FAILED_FATAL }

data class QueueSnapshot(
    val status: QueueStatus = QueueStatus.EMPTY,
    val pendingCount: Int = 0,
    val oldestCreatedAtMillis: Long? = null,
    val lastError: String? = null
)
