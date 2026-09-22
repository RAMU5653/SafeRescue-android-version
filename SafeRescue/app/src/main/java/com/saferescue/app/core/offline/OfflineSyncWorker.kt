package com.saferescue.app.core.offline

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.saferescue.app.BuildConfig
import com.saferescue.app.core.backend.SecureBackendClient
import com.saferescue.app.security.SecureSessionStore
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit

/** Phase 13: drains the encrypted queue through authenticated HTTPS. */
class OfflineSyncWorker(appContext: Context, params: WorkerParameters) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val queue = EncryptedOfflineQueue(applicationContext)
        val ready = queue.ready()
        if (ready.isEmpty()) return Result.success()

        val token = SecureSessionStore(applicationContext).tokenBytes()
            ?: return Result.retry()
        val client = SecureBackendClient(
            baseUrl = BuildConfig.BACKEND_BASE_URL,
            bearerToken = token.toString(StandardCharsets.UTF_8)
        )
        token.fill(0)

        var hadRetryableFailure = false
        ready.forEach { item ->
            val incident = client.uploadIncident(item.payload, item.idempotencyKey)
            when (incident.outcome) {
                com.saferescue.app.core.backend.UploadOutcome.SUCCESS -> {
                    var evidenceFailed = false
                    item.evidencePaths.forEachIndexed { index, path ->
                        val evidence = java.io.File(path)
                        val key = "${item.idempotencyKey}:evidence:$index"
                        when (client.uploadEvidence(item.incidentId, evidence, key).outcome) {
                            com.saferescue.app.core.backend.UploadOutcome.SUCCESS -> Unit
                            com.saferescue.app.core.backend.UploadOutcome.RETRYABLE -> {
                                evidenceFailed = true
                                hadRetryableFailure = true
                            }
                            com.saferescue.app.core.backend.UploadOutcome.FATAL -> evidenceFailed = true
                        }
                    }
                    if (!evidenceFailed) queue.remove(item.id)
                    else queue.markRetryableFailure(item.id, "Evidence upload pending")
                }
                com.saferescue.app.core.backend.UploadOutcome.RETRYABLE -> {
                    hadRetryableFailure = true
                    queue.markRetryableFailure(item.id, incident.message)
                }
                com.saferescue.app.core.backend.UploadOutcome.FATAL -> queue.markFatalFailure(item.id, incident.message)
            }
        }

        return if (hadRetryableFailure) Result.retry() else Result.success()
    }

    companion object {
        const val UNIQUE_WORK_NAME = "SafeRescue.OfflineEvidenceSync"

        fun schedule(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = OneTimeWorkRequestBuilder<OfflineSyncWorker>()
                .setConstraints(constraints)
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 10, TimeUnit.SECONDS)
                .build()
            WorkManager.getInstance(context).enqueueUniqueWork(
                UNIQUE_WORK_NAME,
                androidx.work.ExistingWorkPolicy.KEEP,
                request
            )
        }
    }
}
