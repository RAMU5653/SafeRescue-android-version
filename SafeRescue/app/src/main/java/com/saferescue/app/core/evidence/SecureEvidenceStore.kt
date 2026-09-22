package com.saferescue.app.core.evidence

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.OutputStream
import java.security.KeyStore
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.CipherOutputStream
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import kotlin.random.Random

/**
 * Phase 10: app-private, AES-256-GCM evidence storage.
 * The encrypted file format is: 12-byte IV followed by AES-GCM ciphertext+tag.
 */
class SecureEvidenceStore(context: Context) {
    private val appContext = context.applicationContext
    private val keyAlias = "SafeRescue.Evidence.AES256.v1"

    private fun key(): SecretKey {
        val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        (ks.getKey(keyAlias, null) as? SecretKey)?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(
                keyAlias,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            )
                .setKeySize(256)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setUserAuthenticationRequired(false)
                .build()
        )
        return generator.generateKey()
    }

    /** Creates an encrypted stream; callers must close it before hashing the file. */
    fun createEncryptedOutput(target: File): OutputStream {
        target.parentFile?.mkdirs()
        val iv = ByteArray(12).also { Random.nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.ENCRYPT_MODE, key(), GCMParameterSpec(128, iv))
        }
        val raw = BufferedOutputStream(FileOutputStream(target))
        raw.write(iv)
        raw.flush()
        return CipherOutputStream(raw, cipher)
    }

    /** Encrypts a completed plaintext file, then callers can delete the source. */
    fun encryptFile(source: File, target: File): EvidenceFile {
        require(source.exists()) { "Source evidence does not exist" }
        createEncryptedOutput(target).use { encrypted ->
            BufferedInputStream(FileInputStream(source)).use { input -> input.copyTo(encrypted) }
        }
        val hash = sha256(target)
        return EvidenceFile(target.absolutePath, hash, target.length())
    }

    fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        BufferedInputStream(FileInputStream(file)).use { input ->
            val buffer = ByteArray(8192)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                digest.update(buffer, 0, count)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    /** Best-effort secure deletion for temporary plaintext/cache material. */
    fun secureDelete(file: File): Boolean {
        if (!file.exists()) return true
        return runCatching {
            if (file.isFile) {
                FileOutputStream(file, false).use { output ->
                    val size = file.length().coerceAtMost(8L * 1024L * 1024L).toInt()
                    if (size > 0) output.write(ByteArray(size) { 0 })
                    output.flush()
                }
            }
            file.delete()
        }.getOrDefault(false)
    }

    fun pendingDirectory(): File = File(appContext.filesDir, "evidence_secure").apply { mkdirs() }

    fun encryptedEvidenceFiles(): List<File> =
        pendingDirectory().listFiles { file -> file.isFile && file.name.endsWith(".enc") }?.sortedBy { it.name }.orEmpty()

    data class EvidenceFile(val path: String, val sha256: String, val sizeBytes: Long)
}
