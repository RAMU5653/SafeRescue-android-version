package com.saferescue.app.core.contacts

import android.content.Context
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.security.SecureRandom
import java.util.UUID
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Phase 14 local contact store. Contact names and phone numbers never enter plain SharedPreferences;
 * the complete JSON document is encrypted with an Android Keystore AES-GCM key.
 */
class EncryptedTrustedContactsStore(context: Context) : TrustedContactsRepository {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    @Synchronized
    override fun getAll(): List<TrustedContact> = read().sortedBy { it.createdAtMillis }

    @Synchronized
    override fun save(name: String, phone: String, existingId: String?): ContactSaveResult {
        val cleanName = name.trim().replace(Regex("\\s+"), " ")
        val cleanPhone = phone.trim()
        if (cleanName.length !in 2..80) return ContactSaveResult.Failure("Enter a contact name (2–80 characters).")
        if (!isPlausiblePhone(cleanPhone)) return ContactSaveResult.Failure("Enter a valid phone number.")

        val current = read().toMutableList()
        val now = System.currentTimeMillis()
        val duplicate = current.any { it.id != existingId && normalizePhone(it.phone) == normalizePhone(cleanPhone) }
        if (duplicate) return ContactSaveResult.Failure("That phone number is already a trusted contact.")

        val contact = if (existingId == null) {
            if (current.size >= MAX_CONTACTS) return ContactSaveResult.Failure("You can configure up to 3 trusted contacts.")
            TrustedContact(UUID.randomUUID().toString(), cleanName, cleanPhone, false, now, now)
        } else {
            val index = current.indexOfFirst { it.id == existingId }
            if (index < 0) return ContactSaveResult.Failure("Trusted contact was not found.")
            current[index].copy(name = cleanName, phone = cleanPhone, verified = false, updatedAtMillis = now)
        }

        if (existingId == null) current += contact else current[current.indexOfFirst { it.id == existingId }] = contact
        write(current)
        return ContactSaveResult.Success(contact)
    }

    @Synchronized
    override fun remove(id: String): Boolean {
        val current = read().toMutableList()
        val changed = current.removeAll { it.id == id }
        if (changed) write(current)
        return changed
    }

    @Synchronized
    override fun markVerified(id: String, verified: Boolean): Boolean {
        val current = read().toMutableList()
        val index = current.indexOfFirst { it.id == id }
        if (index < 0) return false
        current[index] = current[index].copy(verified = verified, updatedAtMillis = System.currentTimeMillis())
        write(current)
        return true
    }

    private fun read(): List<TrustedContact> = runCatching {
        val raw = prefs.getString(KEY_CONTACTS, null) ?: return emptyList()
        val json = JSONArray(decrypt(raw))
        buildList {
            for (i in 0 until json.length()) {
                val item = json.getJSONObject(i)
                add(
                    TrustedContact(
                        id = item.getString("id"),
                        name = item.getString("name"),
                        phone = item.getString("phone"),
                        verified = item.optBoolean("verified", false),
                        createdAtMillis = item.optLong("createdAtMillis", 0L),
                        updatedAtMillis = item.optLong("updatedAtMillis", 0L)
                    )
                )
            }
        }
    }.getOrElse { emptyList() }

    private fun write(items: List<TrustedContact>) {
        val json = JSONArray()
        items.take(MAX_CONTACTS).forEach { contact ->
            json.put(JSONObject().apply {
                put("id", contact.id)
                put("name", contact.name)
                put("phone", contact.phone)
                put("verified", contact.verified)
                put("createdAtMillis", contact.createdAtMillis)
                put("updatedAtMillis", contact.updatedAtMillis)
            })
        }
        prefs.edit().putString(KEY_CONTACTS, encrypt(json.toString())).apply()
    }

    private fun isPlausiblePhone(value: String): Boolean {
        val digits = value.count { it.isDigit() }
        return value.length <= 25 && digits in 7..15 && value.all { it.isDigit() || it in "+ -()." }
    }

    private fun normalizePhone(value: String): String = value.filter(Char::isDigit).takeLast(15)

    private fun encrypt(plain: String): String {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        return Base64.encodeToString(cipher.iv + cipher.doFinal(plain.toByteArray(StandardCharsets.UTF_8)), Base64.NO_WRAP)
    }

    private fun decrypt(value: String): String {
        val combined = Base64.decode(value, Base64.NO_WRAP)
        require(combined.size > 12)
        val iv = combined.copyOfRange(0, 12)
        val cipherText = combined.copyOfRange(12, combined.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
        return cipher.doFinal(cipherText).toString(StandardCharsets.UTF_8)
    }

    private fun key(): SecretKey {
        val store = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val existing = store.getKey(KEY_ALIAS, null)
        if (existing is SecretKey) return existing
        val generator = KeyGenerator.getInstance("AES", "AndroidKeyStore")
        generator.init(
            android.security.keystore.KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                android.security.keystore.KeyProperties.PURPOSE_ENCRYPT or android.security.keystore.KeyProperties.PURPOSE_DECRYPT
            ).setBlockModes(android.security.keystore.KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(android.security.keystore.KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }

    companion object {
        const val MAX_CONTACTS = 3
        private const val PREFS_NAME = "saferescue_trusted_contacts"
        private const val KEY_CONTACTS = "encrypted_contacts_v1"
        private const val KEY_ALIAS = "saferescue.contacts.v1"
    }
}
