package com.saferescue.app.core.contacts

interface TrustedContactsRepository {
    fun getAll(): List<TrustedContact>
    fun save(name: String, phone: String, existingId: String? = null): ContactSaveResult
    fun remove(id: String): Boolean
    fun markVerified(id: String, verified: Boolean): Boolean
}
