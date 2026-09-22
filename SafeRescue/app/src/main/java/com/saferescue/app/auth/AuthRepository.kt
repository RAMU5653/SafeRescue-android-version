package com.saferescue.app.auth

interface AuthRepository {
    fun login(username: String, password: CharArray): AuthResult
    fun requestRegistrationOtp(input: RegistrationInput): OtpResult
    fun verifyRegistrationOtp(challengeId: String, code: String): OtpResult
    fun requestPasswordReset(username: String): OtpResult
    fun verifyPasswordReset(challengeId: String, code: String, newPassword: CharArray): AuthResult
}
