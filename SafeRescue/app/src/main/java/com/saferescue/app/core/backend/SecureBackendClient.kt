package com.saferescue.app.core.backend

import android.net.Uri
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import javax.net.ssl.HttpsURLConnection

/**
 * Phase 13 HTTPS transport. It never embeds credentials or backend secrets.
 * The server contract is intentionally small: create/ack an incident, then upload
 * already-encrypted evidence blobs. The server must enforce Bearer authentication,
 * authorization, idempotency and object ownership.
 */
class SecureBackendClient(
    private val baseUrl: String,
    private val bearerToken: String?,
    private val connectTimeoutMs: Int = 10_000,
    private val readTimeoutMs: Int = 20_000
) {
    fun uploadIncident(incidentJson: String, idempotencyKey: String): BackendUploadResult =
        requestJson("POST", "/v1/incidents", incidentJson, idempotencyKey)

    fun uploadEvidence(incidentId: String, file: File, idempotencyKey: String): BackendUploadResult {
        if (!file.isFile || !file.canRead()) return BackendUploadResult(UploadOutcome.FATAL, message = "Evidence file unavailable")
        val path = "/v1/incidents/${Uri.encode(incidentId)}/evidence/${Uri.encode(file.name)}"
        return requestBytes("PUT", path, file, idempotencyKey)
    }

    private fun requestJson(method: String, path: String, body: String, idempotencyKey: String): BackendUploadResult {
        val bytes = body.toByteArray(Charsets.UTF_8)
        if (bytes.size > 256 * 1024) return BackendUploadResult(UploadOutcome.FATAL, message = "Incident payload too large")
        return request(method, path, "application/json", idempotencyKey) { connection ->
            connection.outputStream.use { it.write(bytes) }
        }
    }

    private fun requestBytes(method: String, path: String, file: File, idempotencyKey: String): BackendUploadResult =
        request(method, path, "application/octet-stream", idempotencyKey) { connection ->
            file.inputStream().use { input ->
                connection.outputStream.use { output ->
                    input.copyTo(output, DEFAULT_BUFFER)
                }
            }
        }

    private fun request(
        method: String,
        path: String,
        contentType: String,
        idempotencyKey: String,
        writeBody: (HttpURLConnection) -> Unit
    ): BackendUploadResult {
        if (baseUrl.isBlank()) return BackendUploadResult(UploadOutcome.RETRYABLE, message = "Backend endpoint not configured")
        if (bearerToken.isNullOrBlank()) return BackendUploadResult(UploadOutcome.RETRYABLE, message = "Authenticated session unavailable")
        val normalized = baseUrl.trimEnd('/')
        val url = runCatching { URL(normalized + path) }.getOrElse {
            return BackendUploadResult(UploadOutcome.FATAL, message = "Invalid backend URL")
        }
        if (url.protocol != "https") return BackendUploadResult(UploadOutcome.FATAL, message = "HTTPS is required")
        if (url.userInfo != null || url.query != null || url.ref != null) {
            return BackendUploadResult(UploadOutcome.FATAL, message = "Backend URL must not contain credentials, query parameters or fragments")
        }
        if (idempotencyKey.length !in 16..128 || !idempotencyKey.all { it.isLetterOrDigit() || it in "._:-" }) {
            return BackendUploadResult(UploadOutcome.FATAL, message = "Invalid idempotency key")
        }

        val connection = runCatching { url.openConnection() as HttpsURLConnection }.getOrElse {
            return BackendUploadResult(UploadOutcome.RETRYABLE, message = "TLS connection unavailable")
        }
        return try {
            connection.requestMethod = method
            connection.connectTimeout = connectTimeoutMs
            connection.readTimeout = readTimeoutMs
            connection.doInput = true
            connection.doOutput = true
            connection.useCaches = false
            connection.setRequestProperty("Authorization", "Bearer $bearerToken")
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("Content-Type", contentType)
            connection.setRequestProperty("Idempotency-Key", idempotencyKey.take(128))
            connection.setRequestProperty("X-SafeRescue-Client", "android-phase13")
            writeBody(connection)
            val code = connection.responseCode
            val message = if (code in 200..299 || code == 409) "HTTP $code" else "HTTP $code"
            when {
                code in 200..299 -> BackendUploadResult(UploadOutcome.SUCCESS, code, message)
                code == 408 || code == 425 || code == 429 || code >= 500 -> BackendUploadResult(UploadOutcome.RETRYABLE, code, message)
                code == 409 -> BackendUploadResult(UploadOutcome.SUCCESS, code, "Idempotent duplicate acknowledged")
                code == 401 || code == 403 -> BackendUploadResult(UploadOutcome.FATAL, code, "Authentication/authorization rejected")
                else -> BackendUploadResult(UploadOutcome.FATAL, code, message)
            }
        } catch (_: java.io.IOException) {
            BackendUploadResult(UploadOutcome.RETRYABLE, message = "Network or TLS I/O failure")
        } finally {
            connection.disconnect()
        }
    }

    companion object { private const val DEFAULT_BUFFER = 16 * 1024 }
}
