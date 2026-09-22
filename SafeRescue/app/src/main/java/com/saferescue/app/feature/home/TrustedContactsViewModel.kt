package com.saferescue.app.feature.home

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.saferescue.app.core.contacts.ContactSaveResult
import com.saferescue.app.core.contacts.EncryptedTrustedContactsStore
import com.saferescue.app.core.contacts.TrustedContact
import com.saferescue.app.core.notification.SafeRescueNotificationManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class TrustedContactsViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = EncryptedTrustedContactsStore(application)
    private val notifications = SafeRescueNotificationManager(application)
    private val _state = MutableStateFlow(UiState(contacts = repository.getAll(), notificationsAllowed = notifications.canPostNotifications()))
    val state: StateFlow<UiState> = _state.asStateFlow()

    fun refreshNotificationPermissionState() {
        _state.value = _state.value.copy(notificationsAllowed = notifications.canPostNotifications())
    }

    fun save(name: String, phone: String, existingId: String? = null) {
        when (val result = repository.save(name, phone, existingId)) {
            is ContactSaveResult.Success -> setMessage("Trusted contact saved. Verify the number before relying on it for emergencies.")
            is ContactSaveResult.Failure -> setMessage(result.message, error = true)
        }
        reload()
    }

    fun remove(id: String) {
        if (repository.remove(id)) setMessage("Trusted contact removed.")
        reload()
    }

    fun toggleVerified(contact: TrustedContact) {
        repository.markVerified(contact.id, !contact.verified)
        setMessage(if (contact.verified) "Contact marked unverified." else "Contact marked verified for this device.")
        reload()
    }

    fun testNotification(contact: TrustedContact): Boolean {
        val ok = notifications.showTestNotification(contact.name)
        setMessage(
            if (ok) "Local test notification posted. It did not send an external message."
            else "Allow SafeRescue notifications first.",
            error = !ok
        )
        refreshNotificationPermissionState()
        return ok
    }

    private fun reload() {
        _state.value = _state.value.copy(contacts = repository.getAll())
    }

    private fun setMessage(message: String, error: Boolean = false) {
        _state.value = _state.value.copy(message = if (!error) message else null, error = if (error) message else null)
    }

    data class UiState(
        val contacts: List<TrustedContact> = emptyList(),
        val notificationsAllowed: Boolean = false,
        val message: String? = null,
        val error: String? = null
    )
}
