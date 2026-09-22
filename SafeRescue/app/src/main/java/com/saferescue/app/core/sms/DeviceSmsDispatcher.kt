package com.saferescue.app.core.sms

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.telephony.SmsManager
import androidx.core.content.ContextCompat
import com.saferescue.app.core.contacts.EncryptedTrustedContactsStore
import com.saferescue.app.core.contacts.TrustedContact

sealed interface SmsDispatchResult {
    data class Success(val contactCount: Int) : SmsDispatchResult
    data class PermissionDenied(val message: String) : SmsDispatchResult
    data class Failure(val message: String) : SmsDispatchResult
}

/**
 * Dispatches emergency SMS alerts directly through the physical Android device's
 * active SIM card and cellular network to all configured trusted contacts.
 */
class DeviceSmsDispatcher(private val context: Context) {

    private val contactsStore by lazy { EncryptedTrustedContactsStore(context) }

    fun hasSmsPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.SEND_SMS
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Sends an emergency SMS to all configured trusted contacts with victim identity,
     * GPS coordinates, on-device AI incident summary, and secured evidence details.
     */
    fun dispatchEmergencySms(
        latitude: Double?,
        longitude: Double?,
        victimName: String? = null,
        victimPhone: String? = null,
        accuracyMeters: Float? = null,
        aiSummary: String? = null,
        evidenceCount: Int = 0,
        evidenceDigests: List<String> = emptyList(),
        isTimerExpired: Boolean = false
    ): SmsDispatchResult {
        if (!hasSmsPermission()) {
            return SmsDispatchResult.PermissionDenied(
                "SEND_SMS permission not granted. Please grant SMS permissions in App Settings."
            )
        }

        val contacts = contactsStore.list()
        if (contacts.isEmpty()) {
            return SmsDispatchResult.Failure("No trusted contacts configured.")
        }

        val smsManager: SmsManager = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            context.getSystemService(SmsManager::class.java)
        } else {
            @Suppress("DEPRECATION")
            SmsManager.getDefault()
        }

        val header = if (isTimerExpired) {
            "🚨 SafeRescue CRITICAL SOS ALERT (2-Min Safety Timer Expired)"
        } else {
            "🚨 SafeRescue EMERGENCY ALERT"
        }

        val name = victimName?.takeIf { it.isNotBlank() } ?: "Venkata Ram"
        val phoneSuffix = victimPhone?.takeIf { it.isNotBlank() }?.let { " (Tel: $it)" } ?: ""
        val victimSection = "👤 Victim: $name$phoneSuffix"

        val gpsSection = if (latitude != null && longitude != null) {
            val accSuffix = accuracyMeters?.let { " [±${it.toInt()}m]" } ?: ""
            "📍 Live GPS: https://maps.google.com/?q=$latitude,$longitude ($latitude, $longitude)$accSuffix"
        } else {
            "📍 Live GPS: Coordinates currently acquiring/unavailable."
        }

        val aiSection = if (!aiSummary.isNullOrBlank()) {
            "🤖 AI Summary: $aiSummary"
        } else if (isTimerExpired) {
            "🤖 AI Summary: SOS triggered via continuous hold. 2-minute safety countdown expired without cancellation. High-priority assistance required."
        } else {
            "🤖 AI Summary: Manual emergency hold triggered. Protection mode engaged."
        }

        val evidenceSection = if (evidenceCount > 0) {
            val hashesPreview = if (evidenceDigests.isNotEmpty()) {
                " [SHA256: " + evidenceDigests.take(2).joinToString(", ") { it.take(8) + "..." } + "]"
            } else ""
            "📸 Evidences: $evidenceCount secured photo(s) sealed$hashesPreview."
        } else {
            "📸 Evidences: 0 photos captured."
        }

        val messageText = "$header\n$victimSection\n$gpsSection\n$aiSection\n$evidenceSection\nImmediate assistance requested!"

        var successCount = 0
        var failureReason: String? = null

        for (contact in contacts) {
            try {
                val parts = smsManager.divideMessage(messageText)
                smsManager.sendMultipartTextMessage(
                    contact.phone,
                    null,
                    parts,
                    null,
                    null
                )
                successCount++
            } catch (e: Exception) {
                failureReason = e.message ?: "Unknown SMS sending failure"
            }
        }

        return if (successCount > 0) {
            SmsDispatchResult.Success(successCount)
        } else {
            SmsDispatchResult.Failure(failureReason ?: "Failed to send SMS via SIM card.")
        }
    }
}
