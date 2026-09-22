package com.saferescue.app.security

import android.content.Context
import android.util.Base64
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Stores only a short-lived debug/staging session token and username, encrypted with Android Keystore. */
class SecureSessionStore(context: Context) {
    private val prefs = context.getSharedPreferences("saferescue_session", Context.MODE_PRIVATE)

    fun save(username: String) {
        val token = ByteArray(32).also { SecureRandom().nextBytes(it) }
        prefs.edit()
            .putString(KEY_USERNAME, encrypt(username.toByteArray(StandardCharsets.UTF_8)))
            .putString(KEY_TOKEN, encrypt(token))
            .apply()
        token.fill(0)
    }

    fun saveProfile(displayName: String, phone: String) {
        prefs.edit()
            .putString(KEY_DISPLAY_NAME, encrypt(displayName.toByteArray(StandardCharsets.UTF_8)))
            .putString(KEY_PHONE, encrypt(phone.toByteArray(StandardCharsets.UTF_8)))
            .apply()
    }

    fun username(): String? = prefs.getString(KEY_USERNAME, null)?.let { decrypt(it)?.toString(StandardCharsets.UTF_8) }

    fun displayName(): String? = prefs.getString(KEY_DISPLAY_NAME, null)?.let { decrypt(it)?.toString(StandardCharsets.UTF_8) } ?: username()

    fun phone(): String? = prefs.getString(KEY_PHONE, null)?.let { decrypt(it)?.toString(StandardCharsets.UTF_8) }

    fun tokenBytes(): ByteArray? = prefs.getString(KEY_TOKEN, null)?.let { decrypt(it) }

    fun isSignedIn(): Boolean = prefs.contains(KEY_TOKEN) && username() != null

    fun clear() {
        prefs.edit().clear().apply()
    }

    private fun encrypt(plain: ByteArray): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        val combined = cipher.iv + cipher.doFinal(plain)
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    private fun decrypt(value: String): ByteArray? = runCatching {
        val combined = Base64.decode(value, Base64.NO_WRAP)
        require(combined.size > 12)
        val iv = combined.copyOfRange(0, 12)
        val ciphertext = combined.copyOfRange(12, combined.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
        cipher.doFinal(ciphertext)
    }.getOrNull()

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val existing = store.getKey(KEY_ALIAS, null)
        if (existing is SecretKey) return existing

        val generator = KeyGenerator.getInstance("AES", "AndroidKeyStore")
        generator.init(android.security.keystore.KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or
                android.security.keystore.KeyProperties.PURPOSE_DECRYPT
        ).setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
            .setRandomizedEncryptionRequired(true)
            .build())
        return generator.generateKey()
    }

    companion object {
        private const val KEY_ALIAS = "saferescue.session.v1"
        private const val KEY_USERNAME = "username"
        private const val KEY_DISPLAY_NAME = "display_name"
        private const val KEY_PHONE = "user_phone"
        private const val KEY_TOKEN = "token"
    }
}
