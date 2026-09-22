package com.saferescue.app.security

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Password hashing helper for the local debug/staging auth adapter.
 * Production password hashing MUST happen on the backend.
 */
object PasswordHasher {
    private const val ITERATIONS = 120_000
    private const val KEY_LENGTH_BITS = 256
    private const val SALT_BYTES = 16

    data class HashResult(val salt: ByteArray, val hash: ByteArray)

    fun hash(password: CharArray): HashResult {
        require(password.isNotEmpty()) { "Password cannot be empty" }
        val salt = ByteArray(SALT_BYTES).also { SecureRandom().nextBytes(it) }
        return HashResult(salt, derive(password, salt))
    }

    fun verify(password: CharArray, salt: ByteArray, expectedHash: ByteArray): Boolean {
        val actual = derive(password, salt)
        return MessageDigest.isEqual(actual, expectedHash)
    }

    private fun derive(password: CharArray, salt: ByteArray): ByteArray {
        val working = password.copyOf()
        val spec = PBEKeySpec(working, salt, ITERATIONS, KEY_LENGTH_BITS)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
            working.fill('\u0000')
        }
    }
}
