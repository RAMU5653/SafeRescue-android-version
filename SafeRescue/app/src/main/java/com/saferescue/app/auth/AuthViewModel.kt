package com.saferescue.app.auth

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.saferescue.app.security.SecureSessionStore
import com.saferescue.app.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed interface AuthScreenState {
    data object Login : AuthScreenState
    data class RegisterOtp(val challengeId: String, val debugOtp: String?, val expiresAt: Long) : AuthScreenState
    data class ResetOtp(val challengeId: String, val debugOtp: String?, val expiresAt: Long) : AuthScreenState
    data class SignedIn(val user: AuthUser) : AuthScreenState
}

data class AuthUiState(
    val screen: AuthScreenState = AuthScreenState.Login,
    val loading: Boolean = false,
    val message: String? = null,
    val error: String? = null
)

class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: AuthRepository = if (BuildConfig.DEBUG) DebugAuthRepository(application) else ProductionAuthRepository()
    private val sessions = SecureSessionStore(application)
    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    init {
        if (sessions.isSignedIn()) {
            val username = sessions.username().orEmpty()
            _state.value = AuthUiState(screen = AuthScreenState.SignedIn(AuthUser(username, "SafeRescue User", "", "")))
        }
    }

    fun login(username: String, password: String) {
        if (username.isBlank() || password.isBlank()) {
            _state.value = _state.value.copy(error = "Enter both username and password.")
            return
        }
        _state.value = _state.value.copy(loading = true, error = null, message = null)
        val result = repository.login(username, password.toCharArray())
        _state.value = when (result) {
            is AuthResult.Success -> {
                sessions.save(result.user.username)
                AuthUiState(screen = AuthScreenState.SignedIn(result.user), message = "Signed in securely.")
            }
            is AuthResult.Failure -> AuthUiState(error = result.message)
        }
    }

    fun startRegistration(input: RegistrationInput) {
        _state.value = _state.value.copy(loading = true, error = null, message = null)
        _state.value = when (val result = repository.requestRegistrationOtp(input)) {
            is OtpResult.Sent -> AuthUiState(screen = AuthScreenState.RegisterOtp(result.challenge.challengeId, result.debugCode, result.challenge.expiresAtEpochMs), message = "OTP challenge created.")
            is OtpResult.Failure -> AuthUiState(error = result.message)
            is OtpResult.Verified -> _state.value
        }
    }

    fun verifyRegistrationOtp(challengeId: String, code: String) {
        _state.value = _state.value.copy(loading = true, error = null)
        _state.value = when (val result = repository.verifyRegistrationOtp(challengeId, code)) {
            is OtpResult.Verified -> {
                sessions.save(result.user.username)
                AuthUiState(screen = AuthScreenState.SignedIn(result.user), message = "Account created and signed in.")
            }
            is OtpResult.Failure -> _state.value.copy(loading = false, error = result.message)
            is OtpResult.Sent -> _state.value
        }
    }

    fun startPasswordReset(username: String) {
        _state.value = _state.value.copy(loading = true, error = null, message = null)
        _state.value = when (val result = repository.requestPasswordReset(username)) {
            is OtpResult.Sent -> AuthUiState(screen = AuthScreenState.ResetOtp(result.challenge.challengeId, result.debugCode, result.challenge.expiresAtEpochMs), message = "Reset OTP challenge created.")
            is OtpResult.Failure -> AuthUiState(error = result.message)
            is OtpResult.Verified -> _state.value
        }
    }

    fun completePasswordReset(challengeId: String, code: String, password: String) {
        _state.value = _state.value.copy(loading = true, error = null)
        _state.value = when (val result = repository.verifyPasswordReset(challengeId, code, password.toCharArray())) {
            is AuthResult.Success -> AuthUiState(message = "Password updated. Please sign in.")
            is AuthResult.Failure -> _state.value.copy(loading = false, error = result.message)
        }
    }

    fun backToLogin() {
        _state.value = AuthUiState()
    }

    fun logout() {
        sessions.clear()
        _state.value = AuthUiState()
    }
}
