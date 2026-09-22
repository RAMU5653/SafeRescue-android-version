package com.saferescue.app.auth

data class RegistrationInput(
    val name: String,
    val username: String,
    val phone: String,
    val email: String,
    val password: CharArray
)

data class AuthUser(
    val username: String,
    val name: String,
    val phone: String,
    val email: String
)

data class PendingOtp(
    val challengeId: String,
    val expiresAtEpochMs: Long
)

enum class AuthError {
    INVALID_CREDENTIALS,
    ACCOUNT_EXISTS,
    INVALID_INPUT,
    OTP_EXPIRED,
    OTP_INVALID,
    OTP_RATE_LIMITED,
    SERVICE_UNAVAILABLE,
    NOT_FOUND
}

sealed interface AuthResult {
    data class Success(val user: AuthUser) : AuthResult
    data class Failure(val error: AuthError, val message: String) : AuthResult
}

sealed interface OtpResult {
    data class Sent(val challenge: PendingOtp, val debugCode: String? = null) : OtpResult
    data class Verified(val user: AuthUser) : OtpResult
    data class Failure(val error: AuthError, val message: String) : OtpResult
}
