package com.saferescue.app.core.contacts

/** A user-configured trusted contact. Phone numbers are stored encrypted at rest. */
data class TrustedContact(
    val id: String,
    val name: String,
    val phone: String,
    val verified: Boolean,
    val createdAtMillis: Long,
    val updatedAtMillis: Long
)

sealed interface ContactSaveResult {
    data class Success(val contact: TrustedContact) : ContactSaveResult
    data class Failure(val message: String) : ContactSaveResult
}
