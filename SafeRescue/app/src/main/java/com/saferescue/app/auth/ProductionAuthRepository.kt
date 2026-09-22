package com.saferescue.app.auth

/**
 * Release boundary for the real backend/SMS implementation.
 * Phase 2 intentionally does not ship a fake production authentication service.
 */
class ProductionAuthRepository : AuthRepository {
    override fun login(username: String, password: CharArray): AuthResult {
        password.fill('\u0000')
        return AuthResult.Failure(AuthError.SERVICE_UNAVAILABLE, "Authentication service is not configured in this build.")
    }

    override fun requestRegistrationOtp(input: RegistrationInput): OtpResult {
        input.password.fill('\u0000')
        return OtpResult.Failure(AuthError.SERVICE_UNAVAILABLE, "Registration/OTP service is not configured in this build.")
    }

    override fun verifyRegistrationOtp(challengeId: String, code: String): OtpResult =
        OtpResult.Failure(AuthError.SERVICE_UNAVAILABLE, "OTP service is not configured in this build.")

    override fun requestPasswordReset(username: String): OtpResult =
        OtpResult.Failure(AuthError.SERVICE_UNAVAILABLE, "Password reset service is not configured in this build.")

    override fun verifyPasswordReset(challengeId: String, code: String, newPassword: CharArray): AuthResult {
        newPassword.fill('\u0000')
        return AuthResult.Failure(AuthError.SERVICE_UNAVAILABLE, "Password reset service is not configured in this build.")
    }
}
