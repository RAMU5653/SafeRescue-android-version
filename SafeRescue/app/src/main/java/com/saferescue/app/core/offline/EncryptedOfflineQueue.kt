package com.saferescue.app.core.offline

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.security.MessageDigest
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec
import javax.crypto.SecretKey
import kotlin.math.min

/**
 * Phase 12: small durable queue backed by one AES-GCM encrypted record per item.
 * Files live under app-private filesDir/offline_queue. No queue payload is stored plaintext.
 */
class EncryptedOfflineQueue(context: Context) : OfflineQueueRepository {
    private val appContext = context.applicationContext
    private val directory = File(appContext.filesDir, "offline_queue").apply { mkdirs() }
    private val keyAlias = "SafeRescue.OfflineQueue.AES256.v1"
    private val maxItems = 100
    private val maxPayloadBytes = 256 * 1024
    private val lock = Any()
    @Volatile private var lastError: String? = null

    override fun enqueue(item: OfflineQueueItem): Boolean = synchronized(lock) {
        if (item.id.isBlank() || item.incidentId.isBlank() || item.idempotencyKey.isBlank()) return false
        val payloadBytes = item.payload.toByteArray(StandardCharsets.UTF_8)
        if (payloadBytes.size > maxPayloadBytes) return false
        if (files().size >= maxItems && files().none { it.nameWithoutExtension == item.id }) return false
        runCatching { write(item) }.onFailure { lastError = "queue write failed" }.isSuccess
    }

    override fun ready(nowMillis: Long): List<OfflineQueueItem> = synchronized(lock) {
        files().mapNotNull { read(it) }
            .filter { it.nextAttemptAtMillis <= nowMillis }
            .sortedWith(compareBy<OfflineQueueItem> { it.nextAttemptAtMillis }.thenBy { it.createdAtMillis })
    }

    override fun markRetryableFailure(id: String, error: String?, nowMillis: Long): Boolean = synchronized(lock) {
        val file = fileFor(id) ?: return false
        val current = read(file) ?: return false
        val retry = current.retryCount + 1
        val capped = min(retry, 12)
        val delay = (1L shl min(capped, 8)) * 1000L
        val updated = current.copy(retryCount = capped, nextAttemptAtMillis = nowMillis + delay)
        lastError = error?.take(160)
        runCatching { write(updated); true }.getOrDefault(false)
    }

    override fun markFatalFailure(id: String, error: String?): Boolean = synchronized(lock) {
        lastError = error?.take(160)
        remove(id)
    }

    override fun remove(id: String): Boolean = synchronized(lock) {
        val file = fileFor(id) ?: return false
        runCatching { file.delete() }.getOrDefault(false)
    }

    override fun snapshot(nowMillis: Long): QueueSnapshot = synchronized(lock) {
        val items = files().mapNotNull { read(it) }
        if (items.isEmpty()) return@synchronized QueueSnapshot(lastError = lastError)
        val ready = items.count { it.nextAttemptAtMillis <= nowMillis }
        QueueSnapshot(
            status = if (ready > 0) QueueStatus.READY else QueueStatus.RETRY_WAIT,
            pendingCount = items.size,
            oldestCreatedAtMillis = items.minOf { it.createdAtMillis },
            lastError = lastError
        )
    }

    override fun clear() = synchronized(lock) { files().forEach { it.delete() } }

    private fun files(): List<File> = directory.listFiles { f -> f.isFile && f.extension == "q" }?.toList().orEmpty()
    private fun fileFor(id: String): File? = files().firstOrNull { it.nameWithoutExtension == id }

    private fun write(item: OfflineQueueItem) {
        val target = File(directory, "${item.id}.q")
        val temp = File(directory, ".${item.id}.tmp")
        val plain = JSONObject().apply {
            put("id", item.id)
            put("incidentId", item.incidentId)
            put("idempotencyKey", item.idempotencyKey)
            put("payload", item.payload)
            put("createdAtMillis", item.createdAtMillis)
            put("retryCount", item.retryCount)
            put("nextAttemptAtMillis", item.nextAttemptAtMillis)
            put("evidencePaths", JSONArray(item.evidencePaths))
        }.toString().toByteArray(StandardCharsets.UTF_8)
        val encrypted = encrypt(plain)
        FileOutputStream(temp).use { it.write(encrypted); it.fd.sync() }
        if (!temp.renameTo(target)) {
            temp.delete()
            throw IllegalStateException("queue atomic rename failed")
        }
    }

    private fun read(file: File): OfflineQueueItem? = runCatching {
        val bytes = FileInputStream(file).use { it.readBytes() }
        val json = JSONObject(String(decrypt(bytes), StandardCharsets.UTF_8))
        val pathsJson = json.optJSONArray("evidencePaths") ?: JSONArray()
        val paths = buildList { for (i in 0 until pathsJson.length()) add(pathsJson.getString(i)) }
        OfflineQueueItem(
            id = json.getString("id"), incidentId = json.getString("incidentId"),
            idempotencyKey = json.getString("idempotencyKey"), payload = json.getString("payload"),
            evidencePaths = paths, createdAtMillis = json.getLong("createdAtMillis"),
            retryCount = json.optInt("retryCount", 0), nextAttemptAtMillis = json.optLong("nextAttemptAtMillis", 0L)
        )
    }.getOrElse { lastError = "queue read failed"; null }

    private fun key(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(keyAlias, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(KeyGenParameterSpec.Builder(keyAlias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
            .setKeySize(256).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE).setUserAuthenticationRequired(false).build())
        return generator.generateKey()
    }

    private fun encrypt(plain: ByteArray): ByteArray {
        val iv = ByteArray(12).also { java.security.SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key(), GCMParameterSpec(128, iv))
        return iv + cipher.doFinal(plain)
    }

    private fun decrypt(blob: ByteArray): ByteArray {
        require(blob.size > 12) { "invalid queue record" }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, blob.copyOfRange(0, 12)))
        return cipher.doFinal(blob.copyOfRange(12, blob.size))
    }
}
