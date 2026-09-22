package com.saferescue.app.core.backend

data class BackendUploadResult(
    val outcome: UploadOutcome,
    val httpCode: Int? = null,
    val message: String? = null
)

enum class UploadOutcome { SUCCESS, RETRYABLE, FATAL }
