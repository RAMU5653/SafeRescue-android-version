package com.saferescue.app.core.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.saferescue.app.LockScreenSosActivity
import com.saferescue.app.R

/** Local notifications. Lock-screen SOS is an OS-supported notification surface; it never unlocks the device. */
class SafeRescueNotificationManager(private val context: Context) {
    fun canPostNotifications(): Boolean =
        android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    /**
     * Posts an ongoing emergency-access notification. When the user has enabled
     * lock-screen notifications, Android may show it on the secure lock screen.
     */
    fun showLockScreenEmergencyAccess(): Boolean {
        if (!canPostNotifications()) return false
        createChannel(LOCK_SCREEN_CHANNEL_ID)
        val openSos = PendingIntent.getActivity(
            context,
            LOCK_SCREEN_REQUEST_CODE,
            Intent(context, LockScreenSosActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notification = NotificationCompat.Builder(context, LOCK_SCREEN_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_saferescue)
            .setContentTitle("SafeRescue Emergency Access")
            .setContentText("SOS is available from the lock screen")
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .addAction(R.drawable.ic_stat_saferescue, "SOS", openSos)
            .build()
        context.getSystemService(NotificationManager::class.java).notify(LOCK_SCREEN_ID, notification)
        return true
    }

    fun hideLockScreenEmergencyAccess() {
        context.getSystemService(NotificationManager::class.java).cancel(LOCK_SCREEN_ID)
    }

    fun showTestNotification(contactName: String): Boolean {
        if (!canPostNotifications()) return false
        createChannel(ALERT_CHANNEL_ID)
        val notification = NotificationCompat.Builder(context, ALERT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_saferescue)
            .setContentTitle("SafeRescue test alert")
            .setContentText("Test alert prepared for $contactName. No external message was sent.")
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        context.getSystemService(NotificationManager::class.java).notify(TEST_ID, notification)
        return true
    }

    fun showEmergencyContactPlan(verifiedCount: Int): Boolean {
        if (!canPostNotifications()) return false
        createChannel(ALERT_CHANNEL_ID)
        val text = if (verifiedCount == 0) {
            "Emergency confirmed. No verified trusted contacts are configured."
        } else {
            "Emergency confirmed. Contact notification is pending the configured secure delivery backend."
        }
        val notification = NotificationCompat.Builder(context, ALERT_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_saferescue)
            .setContentTitle("SafeRescue emergency status")
            .setContentText(text)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .build()
        context.getSystemService(NotificationManager::class.java).notify(EMERGENCY_ID, notification)
        return true
    }

    private fun createChannel(id: String) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = NotificationChannel(id, if (id == LOCK_SCREEN_CHANNEL_ID) "Emergency lock-screen access" else "SafeRescue alerts", NotificationManager.IMPORTANCE_HIGH).apply {
                description = if (id == LOCK_SCREEN_CHANNEL_ID) {
                    "Emergency SOS access that can appear on the phone lock screen"
                } else {
                    "Safety status and test notifications from SafeRescue"
                }
                setShowBadge(false)
            }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    companion object {
        private const val ALERT_CHANNEL_ID = "saferescue_alerts"
        private const val LOCK_SCREEN_CHANNEL_ID = "saferescue_lock_sos"
        private const val TEST_ID = 7101
        private const val EMERGENCY_ID = 7102
        private const val LOCK_SCREEN_ID = 7103
        private const val LOCK_SCREEN_REQUEST_CODE = 7104
    }
}
