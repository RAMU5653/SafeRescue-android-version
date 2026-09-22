package com.saferescue.app.feature.emergency

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.core.content.ContextCompat
import androidx.lifecycle.AndroidViewModel
import com.saferescue.app.core.emergency.EmergencyForegroundService
import com.saferescue.app.core.emergency.EmergencyState
import com.saferescue.app.core.location.LocationPoint
import com.saferescue.app.core.location.LocationStatus
import com.saferescue.app.core.voice.VoiceSignalMetrics
import com.saferescue.app.core.voice.VoiceStatus
import com.saferescue.app.core.risk.RiskSeverity
import com.saferescue.app.core.timeline.TimelineEvent
import com.saferescue.app.core.timeline.TimelineEventType
import com.saferescue.app.core.timeline.TimelineStore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class EmergencyUiViewModel(application: Application) : AndroidViewModel(application) {
    data class UiState(
        val state: EmergencyState = EmergencyState.IDLE,
        val remainingSeconds: Int = 0,
        val locationStatus: LocationStatus = LocationStatus.INACTIVE,
        val latestLocation: LocationPoint? = null,
        val locationMessage: String? = null,
        val voiceStatus: VoiceStatus = VoiceStatus.INACTIVE,
        val latestVoiceMetrics: VoiceSignalMetrics? = null,
        val voiceMessage: String? = null,
        val riskScore: Int = 0,
        val riskSeverity: RiskSeverity = RiskSeverity.LOW,
        val riskFactorCount: Int = 0,
        val riskIsHeuristic: Boolean = true,
        val riskUpdatedAtMillis: Long = 0L,
        val timeline: List<TimelineEvent> = emptyList(),
        val incidentId: String? = null
    )
    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                EmergencyForegroundService.ACTION_RISK -> {
                    val severity = runCatching { RiskSeverity.valueOf(intent.getStringExtra(EmergencyForegroundService.EXTRA_RISK_SEVERITY) ?: RiskSeverity.LOW.name) }.getOrDefault(RiskSeverity.LOW)
                    _state.value = _state.value.copy(
                        riskScore = intent.getIntExtra(EmergencyForegroundService.EXTRA_RISK_SCORE, 0).coerceIn(0, 100),
                        riskSeverity = severity,
                        riskFactorCount = intent.getIntExtra(EmergencyForegroundService.EXTRA_RISK_FACTOR_COUNT, 0),
                        riskIsHeuristic = intent.getBooleanExtra(EmergencyForegroundService.EXTRA_RISK_HEURISTIC, true),
                        riskUpdatedAtMillis = intent.getLongExtra(EmergencyForegroundService.EXTRA_RISK_UPDATED_AT, 0L)
                    )
                }
                EmergencyForegroundService.ACTION_LOCATION -> {
                    val status = runCatching { LocationStatus.valueOf(intent.getStringExtra(EmergencyForegroundService.EXTRA_LOCATION_STATUS) ?: LocationStatus.INACTIVE.name) }.getOrDefault(LocationStatus.ERROR)
                    val lat = intent.getDoubleExtra(EmergencyForegroundService.EXTRA_LATITUDE, Double.NaN)
                    val lon = intent.getDoubleExtra(EmergencyForegroundService.EXTRA_LONGITUDE, Double.NaN)
                    val point = if (!lat.isNaN() && !lon.isNaN()) LocationPoint(lat, lon, intent.getLongExtra(EmergencyForegroundService.EXTRA_TIMESTAMP, System.currentTimeMillis()), intent.getFloatExtra(EmergencyForegroundService.EXTRA_ACCURACY, 0f)) else _state.value.latestLocation
                    _state.value = _state.value.copy(locationStatus = status, latestLocation = point, locationMessage = intent.getStringExtra(EmergencyForegroundService.EXTRA_LOCATION_MESSAGE))
                }
                EmergencyForegroundService.ACTION_VOICE -> {
                    val status = runCatching { VoiceStatus.valueOf(intent.getStringExtra(EmergencyForegroundService.EXTRA_VOICE_STATUS) ?: VoiceStatus.INACTIVE.name) }.getOrDefault(VoiceStatus.ERROR)
                    val hasMetrics = intent.hasExtra(EmergencyForegroundService.EXTRA_VOICE_TIMESTAMP)
                    val metrics = if (hasMetrics) VoiceSignalMetrics(
                        intent.getLongExtra(EmergencyForegroundService.EXTRA_VOICE_TIMESTAMP, System.currentTimeMillis()),
                        intent.getLongExtra(EmergencyForegroundService.EXTRA_VOICE_DURATION, 0L),
                        intent.getFloatExtra(EmergencyForegroundService.EXTRA_VOICE_RMS_DB, -160f),
                        intent.getIntExtra(EmergencyForegroundService.EXTRA_VOICE_PEAK, 0),
                        intent.getLongExtra(EmergencyForegroundService.EXTRA_VOICE_BYTES, 0L)
                    ) else _state.value.latestVoiceMetrics
                    _state.value = _state.value.copy(voiceStatus = status, latestVoiceMetrics = metrics, voiceMessage = intent.getStringExtra(EmergencyForegroundService.EXTRA_VOICE_MESSAGE))
                }
                EmergencyForegroundService.ACTION_TIMELINE_CLEAR -> {
                    _state.value = _state.value.copy(timeline = emptyList())
                }
                EmergencyForegroundService.ACTION_TIMELINE -> {
                    val type = runCatching { TimelineEventType.valueOf(intent.getStringExtra(EmergencyForegroundService.EXTRA_TIMELINE_TYPE) ?: TimelineEventType.SYSTEM.name) }.getOrDefault(TimelineEventType.SYSTEM)
                    val event = TimelineEvent(
                        id = intent.getStringExtra(EmergencyForegroundService.EXTRA_TIMELINE_ID) ?: return,
                        timestampMillis = intent.getLongExtra(EmergencyForegroundService.EXTRA_TIMELINE_TIMESTAMP, System.currentTimeMillis()),
                        type = type,
                        title = intent.getStringExtra(EmergencyForegroundService.EXTRA_TIMELINE_TITLE) ?: "Event",
                        detail = intent.getStringExtra(EmergencyForegroundService.EXTRA_TIMELINE_DETAIL) ?: "",
                        riskScore = if (intent.hasExtra(EmergencyForegroundService.EXTRA_TIMELINE_RISK)) intent.getIntExtra(EmergencyForegroundService.EXTRA_TIMELINE_RISK, 0) else null
                    )
                    if (_state.value.timeline.none { it.id == event.id }) _state.value = _state.value.copy(timeline = (_state.value.timeline + event).takeLast(500))
                }
                EmergencyForegroundService.ACTION_STATE -> {
                    val raw = intent.getStringExtra(EmergencyForegroundService.EXTRA_STATE) ?: EmergencyState.IDLE.name
                    val remaining = intent.getIntExtra(EmergencyForegroundService.EXTRA_REMAINING, _state.value.remainingSeconds)
                    val incidentId = intent.getStringExtra(EmergencyForegroundService.EXTRA_INCIDENT_ID)
                    _state.value = _state.value.copy(state = runCatching { EmergencyState.valueOf(raw) }.getOrDefault(EmergencyState.IDLE), remainingSeconds = remaining, incidentId = incidentId ?: _state.value.incidentId)
                }
            }
        }
    }

    init {
        _state.value = _state.value.copy(timeline = TimelineStore.snapshot())
        ContextCompat.registerReceiver(getApplication(), receiver, IntentFilter().apply {
            addAction(EmergencyForegroundService.ACTION_STATE)
            addAction(EmergencyForegroundService.ACTION_LOCATION)
            addAction(EmergencyForegroundService.ACTION_VOICE)
            addAction(EmergencyForegroundService.ACTION_RISK)
            addAction(EmergencyForegroundService.ACTION_TIMELINE)
            addAction(EmergencyForegroundService.ACTION_TIMELINE_CLEAR)
        }, ContextCompat.RECEIVER_NOT_EXPORTED)
    }

    fun recordCameraCapture(fileName: String) {
        val event = TimelineEvent(
            id = java.util.UUID.randomUUID().toString(),
            timestampMillis = System.currentTimeMillis(),
            type = TimelineEventType.SYSTEM,
            title = "Camera evidence captured",
            detail = "Temporary encrypted evidence item created: $fileName"
        )
        TimelineStore.append(event)
        _state.value = _state.value.copy(timeline = (_state.value.timeline + event).takeLast(500))
    }

    fun startEmergency() {
        val intent = Intent(getApplication(), EmergencyForegroundService::class.java).setAction(EmergencyForegroundService.ACTION_START)
        ContextCompat.startForegroundService(getApplication(), intent)
    }

    fun startAccessibleEmergency() {
        val intent = Intent(getApplication(), EmergencyForegroundService::class.java).setAction(EmergencyForegroundService.ACTION_ACCESSIBLE_START)
        ContextCompat.startForegroundService(getApplication(), intent)
    }

    fun cancelAccessibleEmergency() {
        getApplication<Application>().startService(
            Intent(getApplication(), EmergencyForegroundService::class.java).setAction(EmergencyForegroundService.ACTION_ACCESSIBLE_CANCEL)
        )
    }

    fun startLocation() {
        getApplication<Application>().startService(Intent(getApplication(), EmergencyForegroundService::class.java).setAction(EmergencyForegroundService.ACTION_LOCATION_START))
    }

    fun startVoice() {
        getApplication<Application>().startService(Intent(getApplication(), EmergencyForegroundService::class.java).setAction(EmergencyForegroundService.ACTION_VOICE_START))
    }

    fun stopVoice() {
        getApplication<Application>().startService(Intent(getApplication(), EmergencyForegroundService::class.java).setAction(EmergencyForegroundService.ACTION_VOICE_STOP))
    }

    fun beginCancellationHold() {
        getApplication<Application>().startService(Intent(getApplication(), EmergencyForegroundService::class.java).setAction(EmergencyForegroundService.ACTION_CANCEL_BEGIN))
    }

    fun abortCancellationHold() {
        getApplication<Application>().startService(Intent(getApplication(), EmergencyForegroundService::class.java).setAction(EmergencyForegroundService.ACTION_CANCEL_HOLD_ABORT))
    }

    fun completeCancellationHold() {
        getApplication<Application>().startService(Intent(getApplication(), EmergencyForegroundService::class.java).setAction(EmergencyForegroundService.ACTION_CANCEL_HOLD_COMPLETE))
    }

    override fun onCleared() {
        getApplication<Application>().unregisterReceiver(receiver)
        super.onCleared()
    }
}
