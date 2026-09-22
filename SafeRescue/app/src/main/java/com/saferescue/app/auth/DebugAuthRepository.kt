package com.saferescue.app.auth

import android.content.Context
import android.util.Base64
import com.saferescue.app.security.PasswordHasher
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Debug-only local adapter. It exists because Phase 2 has no backend/SMS provider yet.
 * It deliberately does not claim to be production authentication.
 */
class DebugAuthRepository(context: Context) : AuthRepository {
    private data class StoredUser(
        val user: AuthUser,
        val salt: ByteArray,
        val passwordHash: ByteArray
    )

    private data class Challenge(
        val username: String,
        val input: RegistrationInput?,
        val codeHash: ByteArray,
        val expiresAt: Long,
        var attempts: Int
    )

    private val users = ConcurrentHashMap<String, StoredUser>()
    private val challenges = ConcurrentHashMap<String, Challenge>()
    private val prefs = context.getSharedPreferences("saferescue_debug_auth", Context.MODE_PRIVATE)

    init {
        val username = "admin"
        val salt = Base64.decode(prefs.getString("admin_salt", "") ?: "", Base64.NO_WRAP)
        val hash = Base64.decode(prefs.getString("admin_hash", "") ?: "", Base64.NO_WRAP)
        if (salt.isNotEmpty() && hash.isNotEmpty()) {
            users[username] = StoredUser(AuthUser(username, "SafeRescue Test User", "", ""), salt, hash)
        } else {
            // Debug fixture only. The plaintext password is immediately converted to a password hash.
            val result = PasswordHasher.hash("admin".toCharArray())
            users[username] = StoredUser(AuthUser(username, "SafeRescue Test User", "", ""), result.salt, result.hash)
            prefs.edit()
                .putString("admin_salt", Base64.encodeToString(result.salt, Base64.NO_WRAP))
                .putString("admin_hash", Base64.encodeToString(result.hash, Base64.NO_WRAP))
                .apply()
        }
    }

    override fun login(username: String, password: CharArray): AuthResult {
        val key = username.trim().lowercase()
        val stored = users[key]
        val ok = stored != null && PasswordHasher.verify(password, stored.salt, stored.passwordHash)
        return if (ok) AuthResult.Success(stored!!.user)
        else AuthResult.Failure(AuthError.INVALID_CREDENTIALS, "Invalid username or password.")
    }

    override fun requestRegistrationOtp(input: RegistrationInput): OtpResult {
        val username = input.username.trim().lowercase()
        if (username.isBlank() || input.name.isBlank() || input.phone.length < 8 || !input.email.contains('@') || input.password.size < 8) {
            input.password.fill('\u0000')
            return OtpResult.Failure(AuthError.INVALID_INPUT, "Enter valid name, username, phone, email and an 8+ character password.")
        }
        if (users.containsKey(username)) {
            input.password.fill('\u0000')
            return OtpResult.Failure(AuthError.ACCOUNT_EXISTS, "That username is already registered.")
        }

        val codeString = (100000 + SecureRandom().nextInt(900000)).toString()
        val codeHash = sha256(codeString)
        val challengeId = UUID.randomUUID().toString()
        val expires = System.currentTimeMillis() + 5 * 60_000L
        challenges[challengeId] = Challenge(username, input.copy(password = input.password.copyOf()), codeHash, expires, 0)
        input.password.fill('\u0000')

        // Debug-only OTP is returned to the UI for local testing. A real SMS gateway replaces this adapter.
        return OtpResult.Sent(PendingOtp(challengeId, expires), debugCode = codeString)
    }

    override fun verifyRegistrationOtp(challengeId: String, code: String): OtpResult {
        val challenge = challenges[challengeId] ?: return OtpResult.Failure(AuthError.NOT_FOUND, "OTP challenge not found.")
        if (System.currentTimeMillis() > challenge.expiresAt) {
            challenges.remove(challengeId)
            challenge.input?.password?.fill('\u0000')
            return OtpResult.Failure(AuthError.OTP_EXPIRED, "OTP expired. Request a new code.")
        }
        if (++challenge.attempts > 5) {
            challenge.input?.password?.fill('\u0000')
            challenges.remove(challengeId)
            return OtpResult.Failure(AuthError.OTP_RATE_LIMITED, "Too many attempts. Request a new OTP.")
        }
        if (!MessageDigest.isEqual(sha256(code.trim()), challenge.codeHash)) {
            return OtpResult.Failure(AuthError.OTP_INVALID, "Invalid OTP.")
        }

        val input = challenge.input ?: return OtpResult.Failure(AuthError.INVALID_INPUT, "Registration data unavailable.")
        val hash = PasswordHasher.hash(input.password)
        input.password.fill('\u0000')
        val user = AuthUser(challenge.username, input.name.trim(), input.phone.trim(), input.email.trim())
        users[challenge.username] = StoredUser(user, hash.salt, hash.hash)
        challenges.remove(challengeId)
        return OtpResult.Verified(user)
    }

    override fun requestPasswordReset(username: String): OtpResult {
        val key = username.trim().lowercase()
        if (!users.containsKey(key)) return OtpResult.Failure(AuthError.NOT_FOUND, "Account not found.")
        val code = (100000 + SecureRandom().nextInt(900000)).toString()
        val challengeId = UUID.randomUUID().toString()
        challenges[challengeId] = Challenge(key, null, sha256(code), System.currentTimeMillis() + 5 * 60_000L, 0)
        return OtpResult.Sent(PendingOtp(challengeId, System.currentTimeMillis() + 5 * 60_000L), debugCode = code)
    }

    override fun verifyPasswordReset(challengeId: String, code: String, newPassword: CharArray): AuthResult {
        val challenge = challenges[challengeId] ?: return AuthResult.Failure(AuthError.NOT_FOUND, "Reset challenge not found.")
        if (System.currentTimeMillis() > challenge.expiresAt) {
            newPassword.fill('\u0000')
            challenges.remove(challengeId)
            return AuthResult.Failure(AuthError.OTP_EXPIRED, "OTP expired. Request a new code.")
        }
        if (++challenge.attempts > 5) {
            newPassword.fill('\u0000')
            challenges.remove(challengeId)
            return AuthResult.Failure(AuthError.OTP_RATE_LIMITED, "Too many attempts. Request a new OTP.")
        }
        if (!MessageDigest.isEqual(sha256(code.trim()), challenge.codeHash)) {
            newPassword.fill('\u0000')
            return AuthResult.Failure(AuthError.OTP_INVALID, "Invalid OTP.")
        }
        if (newPassword.size < 8) {
            newPassword.fill('\u0000')
            return AuthResult.Failure(AuthError.INVALID_INPUT, "Password must contain at least 8 characters.")
        }
        val current = users[challenge.username] ?: run {
            newPassword.fill('\u0000')
            return AuthResult.Failure(AuthError.NOT_FOUND, "Account not found.")
        }
        val hash = PasswordHasher.hash(newPassword)
        newPassword.fill('\u0000')
        users[challenge.username] = current.copy(salt = hash.salt, passwordHash = hash.hash)
        challenges.remove(challengeId)
        return AuthResult.Success(current.user)
    }

    private fun sha256(value: String): ByteArray = MessageDigest.getInstance("SHA-256").digest(value.toByteArray())
}
