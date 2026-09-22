package com.saferescue.app.core.emergency

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.content.pm.ServiceInfo
import com.saferescue.app.core.location.AndroidLocationRepository
import com.saferescue.app.core.location.LocationPoint
import com.saferescue.app.core.location.LocationStatus
import com.saferescue.app.core.voice.AndroidVoiceRepository
import com.saferescue.app.core.voice.VoiceSignalMetrics
import com.saferescue.app.core.voice.VoiceStatus
import com.saferescue.app.core.risk.RiskEngine
import com.saferescue.app.core.risk.RiskInput
import com.saferescue.app.core.risk.RiskSnapshot
import com.saferescue.app.core.timeline.TimelineEvent
import com.saferescue.app.core.timeline.TimelineEventType
import com.saferescue.app.core.timeline.TimelineStore
import com.saferescue.app.core.performance.AndroidPerformanceMonitor
import com.saferescue.app.core.performance.PerformancePolicy
import com.saferescue.app.core.evidence.SecureEvidenceStore
import com.saferescue.app.core.offline.EncryptedOfflineQueue
import com.saferescue.app.core.offline.OfflineQueueItem
import com.saferescue.app.core.offline.OfflineSyncWorker
import org.json.JSONObject
import java.util.UUID
import androidx.core.app.NotificationCompat
import com.saferescue.app.MainActivity
import com.saferescue.app.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Phase 5 foreground owner for the emergency countdown.
 * Sensor/AI modules are deliberately not involved yet; later phases consume this state.
 */
class EmergencyForegroundService : Service() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var countdownJob: Job? = null
    private var accessibleSosJob: Job? = null
    @Volatile private var locationTracking = false
    private val manager by lazy { EmergencyManager(InMemoryEmergencyRepository()) }
    private val locationRepository by lazy { AndroidLocationRepository(this) }
    private val voiceRepository by lazy { AndroidVoiceRepository(this) }
    private val riskEngine by lazy { RiskEngine() }
    private val evidenceStore by lazy { SecureEvidenceStore(this) }
    private val offlineQueue by lazy { EncryptedOfflineQueue(this) }
    private val performanceMonitor by lazy { AndroidPerformanceMonitor(this) }
    @Volatile private var performancePolicy = PerformancePolicy.from(performanceMonitor.snapshot())
    @Volatile private var lastRiskCalculationMillis = 0L
    @Volatile private var latestLocationStatus = LocationStatus.INACTIVE
    @Volatile private var latestAccuracyMeters: Float? = null
    @Volatile private var latestVoiceMetrics: VoiceSignalMetrics? = null
    @Volatile private var riskSnapshot = RiskSnapshot()
    @Volatile private var lastTimelineRiskScore: Int? = null
    @Volatile private var lastIncidentId: String? = null
    private val smsDispatcher by lazy { com.saferescue.app.core.sms.DeviceSmsDispatcher(this) }
    private val sessionStore by lazy { com.saferescue.app.security.SecureSessionStore(this) }
    @Volatile private var latestLatitude: Double? = null
    @Volatile private var latestLongitude: Double? = null
    @Volatile private var emergencySmsDispatched = false

    override fun onCreate() {
        super.onCreate()
        createChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startEmergency()
            ACTION_ACCESSIBLE_START -> startAccessibleEmergency()
            ACTION_ACCESSIBLE_CANCEL -> cancelAccessibleEmergency()
            ACTION_CANCEL_BEGIN -> beginCancellation()
            ACTION_CANCEL_HOLD_COMPLETE -> cancelEmergency()
            ACTION_CANCEL_HOLD_ABORT -> abortCancellation()
            ACTION_LOCATION_START -> startLocation()
            ACTION_VOICE_START -> startVoice()
            ACTION_VOICE_STOP -> stopVoice()
        }
        return START_NOT_STICKY
    }

    /**
     * TalkBack/keyboard-equivalent entry point. It preserves a five-second
     * safety delay rather than bypassing the physical SOS hold requirement.
     */
    private fun startAccessibleEmergency() {
        if (manager.state() != EmergencyState.IDLE && manager.state() != EmergencyState.CANCELLED && manager.state() != EmergencyState.COMPLETED) return
        accessibleSosJob?.cancel()
        startForegroundSafely()
        broadcastState(5)
        accessibleSosJob = scope.launch {
            for (remaining in 5 downTo 1) {
                if (!isActive) return@launch
                broadcastState(remaining)
                delay(1000)
            }
            if (isActive) startEmergency()
        }
    }

    private fun cancelAccessibleEmergency() {
        accessibleSosJob?.cancel()
        accessibleSosJob = null
        if (manager.state() == EmergencyState.IDLE || manager.state() == EmergencyState.CANCELLED || manager.state() == EmergencyState.COMPLETED) {
            broadcastState(0)
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private fun startEmergency() {
        if (manager.state() == EmergencyState.IDLE) {
            manager.dispatch(EmergencyEvent.BeginSosHold)
            manager.dispatch(EmergencyEvent.SosHoldCompleted)
            appendTimeline(TimelineEventType.SOS_STARTED, "SOS started", "Manual SOS completed; emergency mode activated.")
        }
        if (manager.state() == EmergencyState.EMERGENCY_ACTIVE) {
            manager.dispatch(EmergencyEvent.StartMonitoring)
        }
        startForegroundSafely()
        recalculateRisk(force = true)
        broadcastState(120)
        tryDispatchSms()
        countdownJob?.cancel()
        countdownJob = scope.launch {
            for (remaining in 119 downTo 0) {
                if (!isActive) return@launch
                delay(1000)
                if (!isActive) return@launch
                if (remaining == 0) {
                    confirmEmergency()
                    return@launch
                }
                lastRemaining = remaining
                updateNotification(remaining)
                broadcastState(remaining)
            }
        }
    }

    private fun startVoice() {
        if (manager.state() != EmergencyState.EMERGENCY_ACTIVE && manager.state() != EmergencyState.MONITORING && manager.state() != EmergencyState.CANCELLATION_PENDING) return
        promoteForegroundForActiveSensors()
        voiceRepository.start(::onVoiceMetrics, ::onVoiceStatus)
    }

    private fun stopVoice() {
        voiceRepository.stop()
    }

    private fun promoteForegroundForActiveSensors() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        var type = ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && locationTracking) {
            type = type or ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && voiceRepositoryPermissionGranted()) {
            type = type or ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
        }
        startForeground(NOTIFICATION_ID, notification(lastRemaining), type)
    }


    private fun voiceRepositoryPermissionGranted(): Boolean =
        androidx.core.content.ContextCompat.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED

    private fun onVoiceMetrics(metrics: VoiceSignalMetrics) {
        sendBroadcast(Intent(ACTION_VOICE).apply {
            setPackage(packageName)
            putExtra(EXTRA_VOICE_STATUS, VoiceStatus.LISTENING.name)
            putExtra(EXTRA_VOICE_TIMESTAMP, metrics.timestampMillis)
            putExtra(EXTRA_VOICE_DURATION, metrics.durationMillis)
            putExtra(EXTRA_VOICE_RMS_DB, metrics.rmsDb)
            putExtra(EXTRA_VOICE_PEAK, metrics.peakAmplitude)
            putExtra(EXTRA_VOICE_BYTES, metrics.bytesCaptured)
        })
        latestVoiceMetrics = metrics
        recalculateRisk()
    }

    private fun onVoiceStatus(status: VoiceStatus, message: String?) {
        sendBroadcast(Intent(ACTION_VOICE).apply {
            setPackage(packageName)
            putExtra(EXTRA_VOICE_STATUS, status.name)
            if (message != null) putExtra(EXTRA_VOICE_MESSAGE, message)
        })
        recalculateRisk()
    }

    private fun startLocation() {
        if (manager.state() != EmergencyState.EMERGENCY_ACTIVE && manager.state() != EmergencyState.MONITORING && manager.state() != EmergencyState.CANCELLATION_PENDING) return
        performancePolicy = performanceMonitor.policy()
        locationTracking = locationRepository.start(
            onPoint = ::onLocationPoint,
            onStatus = ::onLocationStatus,
            updateIntervalMillis = 10_000L
        )
        if (locationTracking && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            promoteForegroundForActiveSensors()
        }
    }

    private fun startForegroundSafely() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startForeground(NOTIFICATION_ID, notification(120), ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE)
            } else {
                startForeground(NOTIFICATION_ID, notification(120))
            }
        } else {
            startForeground(NOTIFICATION_ID, notification(120))
        }
    }

    private fun onLocationPoint(point: LocationPoint) {
        sendBroadcast(Intent(ACTION_LOCATION).apply {
            setPackage(packageName)
            putExtra(EXTRA_LATITUDE, point.latitude)
            putExtra(EXTRA_LONGITUDE, point.longitude)
            putExtra(EXTRA_ACCURACY, point.accuracyMeters)
            putExtra(EXTRA_TIMESTAMP, point.timestampMillis)
            putExtra(EXTRA_LOCATION_STATUS, LocationStatus.ACTIVE.name)
        })
        latestLocationStatus = LocationStatus.ACTIVE
        latestAccuracyMeters = point.accuracyMeters
        latestLatitude = point.latitude
        latestLongitude = point.longitude
        appendTimeline(TimelineEventType.LOCATION_UPDATE, "Location updated", "GPS fix received (accuracy ±${point.accuracyMeters.toInt()} m).")
        recalculateRisk()
        if (manager.state() == EmergencyState.EMERGENCY_ACTIVE && !emergencySmsDispatched) {
            tryDispatchSms()
        }
    }

    private fun onLocationStatus(status: LocationStatus, message: String?) {
        sendBroadcast(Intent(ACTION_LOCATION).apply {
            setPackage(packageName)
            putExtra(EXTRA_LOCATION_STATUS, status.name)
            if (message != null) putExtra(EXTRA_LOCATION_MESSAGE, message)
        })
        latestLocationStatus = status
        recalculateRisk()
    }

    private fun beginCancellation() {
        if (manager.state() == EmergencyState.MONITORING || manager.state() == EmergencyState.EMERGENCY_ACTIVE) {
            manager.dispatch(EmergencyEvent.BeginCancellation)
            appendTimeline(TimelineEventType.CANCELLATION_STARTED, "Cancellation started", "User began the 5-second cancellation hold.")
            recalculateRisk(force = true)
            broadcastState(lastRemaining)
        }
    }

    private var lastRemaining: Int = 120

    private fun abortCancellation() {
        if (manager.state() == EmergencyState.CANCELLATION_PENDING) {
            manager.dispatch(EmergencyEvent.CancelHoldAborted)
            broadcastState(lastRemaining)
        }
    }

    private fun cancelEmergency() {
        countdownJob?.cancel()
        accessibleSosJob?.cancel()
        locationRepository.stop()
        locationTracking = false
        stopVoice()
        latestVoiceMetrics = null
        latestLocationStatus = LocationStatus.INACTIVE
        latestAccuracyMeters = null
        latestLatitude = null
        latestLongitude = null
        emergencySmsDispatched = false
        riskSnapshot = RiskSnapshot()
        broadcastRisk()
        if (manager.state() == EmergencyState.MONITORING || manager.state() == EmergencyState.EMERGENCY_ACTIVE) {
            manager.dispatch(EmergencyEvent.BeginCancellation)
        }
        TimelineStore.clear()
        sendBroadcast(Intent(ACTION_TIMELINE_CLEAR).apply { setPackage(packageName) })
        appendTimeline(TimelineEventType.CANCELLATION_COMPLETED, "Emergency cancelled", "Cancellation completed; temporary incident data was cleared.")
        if (manager.state() == EmergencyState.CANCELLATION_PENDING) {
            manager.dispatch(EmergencyEvent.CancelHoldCompleted)
        }
        broadcastState(0)
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun confirmEmergency() {
        countdownJob = null
        locationRepository.stop()
        locationTracking = false
        stopVoice()
        latestVoiceMetrics = null
        latestLocationStatus = LocationStatus.INACTIVE
        latestAccuracyMeters = null
        val current = manager.state()
        if (current == EmergencyState.MONITORING || current == EmergencyState.EMERGENCY_ACTIVE) {
            manager.dispatch(EmergencyEvent.ConfirmEmergency)
        }
        enqueueConfirmedIncident()
        dispatchTimerExpiredSms()
        broadcastState(0)
        updateNotification(0, confirmed = true)
    }

    private fun dispatchTimerExpiredSms() {
        scope.launch(Dispatchers.IO) {
            val evidenceFiles = evidenceStore.encryptedEvidenceFiles()
            val evidenceDigests = evidenceFiles.mapNotNull { file ->
                file.name.substringBeforeLast(".").takeIf { it.length == 64 }
            }
            val storedUser = sessionStore.displayName() ?: "Venkata Ram"
            val storedPhone = sessionStore.phone() ?: "+1 555-0199"

            val voiceSnippet = latestVoiceMetrics?.let { vm ->
                "Voice signal registered ${vm.rmsDb.toInt()} dB."
            } ?: "Sensor distress indicators detected."

            val aiSummary = "SOS triggered via 5s safety hold. On-device acoustic analysis: $voiceSnippet Risk evaluated at ${riskSnapshot.severity} (${riskSnapshot.score}/100). 2-minute safety countdown elapsed without cancellation."

            val result = smsDispatcher.dispatchEmergencySms(
                latitude = latestLatitude,
                longitude = latestLongitude,
                victimName = storedUser,
                victimPhone = storedPhone,
                accuracyMeters = latestAccuracyMeters,
                aiSummary = aiSummary,
                evidenceCount = evidenceFiles.size,
                evidenceDigests = evidenceDigests,
                isTimerExpired = true
            )
            when (result) {
                is com.saferescue.app.core.sms.SmsDispatchResult.Success -> {
                    appendTimeline(
                        TimelineEventType.CONFIRMED_INCIDENT,
                        "2-Min Timer Expired — SMS Sent",
                        "2-minute countdown expired. Dispatched emergency SMS with victim info ($storedUser), GPS ($latestLatitude, $latestLongitude), AI summary, and ${evidenceFiles.size} evidence photo(s) to ${result.contactCount} trusted contacts."
                    )
                }
                is com.saferescue.app.core.sms.SmsDispatchResult.PermissionDenied -> {
                    appendTimeline(
                        TimelineEventType.RISK_UPDATED,
                        "SMS Permission Required",
                        result.message
                    )
                }
                is com.saferescue.app.core.sms.SmsDispatchResult.Failure -> {
                    appendTimeline(
                        TimelineEventType.RISK_UPDATED,
                        "SMS Alert Notice",
                        result.message
                    )
                }
            }
        }
    }

    private fun enqueueConfirmedIncident() {
        val incidentId = UUID.randomUUID().toString()
        lastIncidentId = incidentId
        val now = System.currentTimeMillis()
        val evidencePaths = evidenceStore.encryptedEvidenceFiles().map { it.absolutePath }
        val payload = JSONObject().apply {
            put("incidentId", incidentId)
            put("confirmedAtMillis", now)
            put("riskScore", riskSnapshot.score)
            put("riskSeverity", riskSnapshot.severity.name)
            put("riskHeuristic", riskSnapshot.isHeuristic)
        }.toString()
        val item = OfflineQueueItem(
            id = UUID.randomUUID().toString(),
            incidentId = incidentId,
            idempotencyKey = "saferescue-$incidentId",
            payload = payload,
            evidencePaths = evidencePaths,
            createdAtMillis = now
        )
        if (offlineQueue.enqueue(item)) {
            appendTimeline(TimelineEventType.RISK_UPDATED, "Offline sync queued", "Confirmed incident is durably queued for network sync.")
            OfflineSyncWorker.schedule(this)
        }
    }

    private fun tryDispatchSms() {
        if (emergencySmsDispatched) return
        emergencySmsDispatched = true
        scope.launch(Dispatchers.IO) {
            val storedUser = sessionStore.displayName() ?: "Venkata Ram"
            val storedPhone = sessionStore.phone() ?: "+1 555-0199"
            val aiSummary = "SOS triggered via 5-second emergency hold. Protection mode engaged with on-device sensors."
            val result = smsDispatcher.dispatchEmergencySms(
                latitude = latestLatitude,
                longitude = latestLongitude,
                victimName = storedUser,
                victimPhone = storedPhone,
                accuracyMeters = latestAccuracyMeters,
                aiSummary = aiSummary,
                evidenceCount = 0,
                isTimerExpired = false
            )
            when (result) {
                is com.saferescue.app.core.sms.SmsDispatchResult.Success -> {
                    appendTimeline(
                        TimelineEventType.CONFIRMED_INCIDENT,
                        "Emergency SMS Sent",
                        "Dispatched initial emergency SMS alert with victim details and GPS to ${result.contactCount} trusted contacts via device SIM card."
                    )
                }
                is com.saferescue.app.core.sms.SmsDispatchResult.PermissionDenied -> {
                    appendTimeline(
                        TimelineEventType.RISK_UPDATED,
                        "SMS Permission Required",
                        result.message
                    )
                }
                is com.saferescue.app.core.sms.SmsDispatchResult.Failure -> {
                    appendTimeline(
                        TimelineEventType.RISK_UPDATED,
                        "SMS Alert Notice",
                        result.message
                    )
                }
            }
        }
    }

    private fun recalculateRisk(force: Boolean = false) {
        val now = System.currentTimeMillis()
        performancePolicy = performanceMonitor.policy()
        if (!force && now - lastRiskCalculationMillis < performancePolicy.riskRecalculationIntervalMillis) return
        lastRiskCalculationMillis = now
        val active = manager.state() == EmergencyState.EMERGENCY_ACTIVE ||
            manager.state() == EmergencyState.MONITORING ||
            manager.state() == EmergencyState.CANCELLATION_PENDING
        riskSnapshot = riskEngine.evaluate(
            RiskInput(
                emergencyActive = active,
                locationStatus = latestLocationStatus,
                accuracyMeters = latestAccuracyMeters,
                voiceMetrics = latestVoiceMetrics
            )
        )
        broadcastRisk()
        if (riskSnapshot.score != lastTimelineRiskScore) {
            lastTimelineRiskScore = riskSnapshot.score
            appendTimeline(
                TimelineEventType.RISK_UPDATED,
                "Risk score updated",
                "Heuristic local risk score: ${riskSnapshot.score}/100 (${riskSnapshot.severity.name}).",
                riskSnapshot.score
            )
        }
    }

    private fun broadcastRisk() {
        sendBroadcast(Intent(ACTION_RISK).apply {
            setPackage(packageName)
            putExtra(EXTRA_RISK_SCORE, riskSnapshot.score)
            putExtra(EXTRA_RISK_SEVERITY, riskSnapshot.severity.name)
            putExtra(EXTRA_RISK_UPDATED_AT, riskSnapshot.updatedAtMillis)
            putExtra(EXTRA_RISK_HEURISTIC, riskSnapshot.isHeuristic)
            putExtra(EXTRA_RISK_FACTOR_COUNT, riskSnapshot.factors.size)
        })
    }

    private fun appendTimeline(type: TimelineEventType, title: String, detail: String, riskScore: Int? = null) {
        val event = TimelineEvent(UUID.randomUUID().toString(), System.currentTimeMillis(), type, title, detail, riskScore)
        TimelineStore.append(event)
        sendBroadcast(Intent(ACTION_TIMELINE).apply {
            setPackage(packageName)
            putExtra(EXTRA_TIMELINE_ID, event.id)
            putExtra(EXTRA_TIMELINE_TIMESTAMP, event.timestampMillis)
            putExtra(EXTRA_TIMELINE_TYPE, event.type.name)
            putExtra(EXTRA_TIMELINE_TITLE, event.title)
            putExtra(EXTRA_TIMELINE_DETAIL, event.detail)
            if (riskScore != null) putExtra(EXTRA_TIMELINE_RISK, riskScore)
        })
    }

    private fun broadcastState(remaining: Int? = null) {
        sendBroadcast(Intent(ACTION_STATE).apply {
            putExtra(EXTRA_INCIDENT_ID, lastIncidentId)
            setPackage(packageName)
            putExtra(EXTRA_STATE, manager.state().name)
            if (remaining != null) putExtra(EXTRA_REMAINING, remaining)
        })
    }

    private fun notification(remaining: Int, confirmed: Boolean = false): Notification {
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val text = if (confirmed) "Emergency confirmed" else "SOS active • ${format(remaining)} remaining"
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_saferescue)
            .setContentTitle("SafeRescue")
            .setContentText(text)
            .setOngoing(!confirmed)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(open)
            .build()
    }

    private fun updateNotification(remaining: Int, confirmed: Boolean = false) {
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification(remaining, confirmed))
    }

    private fun format(seconds: Int): String = "%02d:%02d".format(seconds / 60, seconds % 60)

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "Emergency mode", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "Persistent notification while SafeRescue emergency mode is active"
                setShowBadge(false)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        locationRepository.stop()
        locationTracking = false
        stopVoice()
        countdownJob?.cancel()
        scope.coroutineContext.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        const val ACTION_START = "com.saferescue.app.emergency.START"
        const val ACTION_ACCESSIBLE_START = "com.saferescue.app.emergency.ACCESSIBLE_START"
        const val ACTION_ACCESSIBLE_CANCEL = "com.saferescue.app.emergency.ACCESSIBLE_CANCEL"
        const val ACTION_CANCEL_BEGIN = "com.saferescue.app.emergency.CANCEL_BEGIN"
        const val ACTION_CANCEL_HOLD_COMPLETE = "com.saferescue.app.emergency.CANCEL_COMPLETE"
        const val ACTION_CANCEL_HOLD_ABORT = "com.saferescue.app.emergency.CANCEL_ABORT"
        const val ACTION_LOCATION_START = "com.saferescue.app.emergency.LOCATION_START"
        const val ACTION_LOCATION = "com.saferescue.app.emergency.LOCATION"
        const val ACTION_VOICE_START = "com.saferescue.app.emergency.VOICE_START"
        const val ACTION_VOICE_STOP = "com.saferescue.app.emergency.VOICE_STOP"
        const val ACTION_VOICE = "com.saferescue.app.emergency.VOICE"
        const val EXTRA_LATITUDE = "latitude"
        const val EXTRA_LONGITUDE = "longitude"
        const val EXTRA_ACCURACY = "accuracy"
        const val EXTRA_TIMESTAMP = "timestamp"
        const val EXTRA_LOCATION_STATUS = "location_status"
        const val EXTRA_LOCATION_MESSAGE = "location_message"
        const val EXTRA_VOICE_STATUS = "voice_status"
        const val EXTRA_VOICE_TIMESTAMP = "voice_timestamp"
        const val EXTRA_VOICE_DURATION = "voice_duration"
        const val EXTRA_VOICE_RMS_DB = "voice_rms_db"
        const val EXTRA_VOICE_PEAK = "voice_peak"
        const val EXTRA_VOICE_BYTES = "voice_bytes"
        const val EXTRA_VOICE_MESSAGE = "voice_message"
        const val ACTION_RISK = "com.saferescue.app.emergency.RISK"
        const val ACTION_TIMELINE = "com.saferescue.app.emergency.TIMELINE"
        const val ACTION_TIMELINE_CLEAR = "com.saferescue.app.emergency.TIMELINE_CLEAR"
        const val ACTION_STATE = "com.saferescue.app.emergency.STATE"
        const val EXTRA_STATE = "state"
        const val EXTRA_INCIDENT_ID = "incidentId"
        const val EXTRA_REMAINING = "remaining"
        const val EXTRA_RISK_SCORE = "risk_score"
        const val EXTRA_RISK_SEVERITY = "risk_severity"
        const val EXTRA_RISK_UPDATED_AT = "risk_updated_at"
        const val EXTRA_RISK_HEURISTIC = "risk_heuristic"
        const val EXTRA_RISK_FACTOR_COUNT = "risk_factor_count"
        const val EXTRA_TIMELINE_ID = "timeline_id"
        const val EXTRA_TIMELINE_TIMESTAMP = "timeline_timestamp"
        const val EXTRA_TIMELINE_TYPE = "timeline_type"
        const val EXTRA_TIMELINE_TITLE = "timeline_title"
        const val EXTRA_TIMELINE_DETAIL = "timeline_detail"
        const val EXTRA_TIMELINE_RISK = "timeline_risk"
        private const val CHANNEL_ID = "saferescue_emergency"
        private const val NOTIFICATION_ID = 7001
    }
}
