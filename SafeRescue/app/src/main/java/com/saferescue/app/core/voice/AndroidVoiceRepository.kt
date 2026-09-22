package com.saferescue.app.core.voice

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import androidx.core.content.ContextCompat
import java.io.OutputStream
import java.io.File
import com.saferescue.app.core.evidence.SecureEvidenceStore
import com.saferescue.app.core.performance.AndroidPerformanceMonitor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Phase 8: victim-voice capture pipeline.
 * Captures raw PCM only into app-private temporary storage. No cloud upload and no
 * claim of "crime" or "distress" classification are made here; Phase 9 consumes metrics.
 */
class AndroidVoiceRepository(context: Context) : VoiceRepository {
    private val appContext = context.applicationContext
    private val executor: ExecutorService = Executors.newSingleThreadExecutor()
    @Volatile private var recording = false
    private var recorder: AudioRecord? = null
    private var output: OutputStream? = null
    private val evidenceStore = SecureEvidenceStore(appContext)
    private val performanceMonitor = AndroidPerformanceMonitor(appContext)
    private var lastMetricsCallbackMillis = 0L
    private var startedAt = 0L
    private var bytesCaptured = 0L
    private var currentEncryptedFile: File? = null

    override fun start(onMetrics: (VoiceSignalMetrics) -> Unit, onStatus: (VoiceStatus, String?) -> Unit): Boolean {
        if (recording) return true
        if (ContextCompat.checkSelfPermission(appContext, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            onStatus(VoiceStatus.PERMISSION_REQUIRED, "Microphone permission is required.")
            return false
        }
        recording = true
        onStatus(VoiceStatus.STARTING, null)
        executor.execute {
            try {
                val sampleRate = 16_000
                val channel = AudioFormat.CHANNEL_IN_MONO
                val encoding = AudioFormat.ENCODING_PCM_16BIT
                val minBuffer = AudioRecord.getMinBufferSize(sampleRate, channel, encoding)
                if (minBuffer <= 0) throw IllegalStateException("Audio input is unavailable.")
                val bufferSize = max(minBuffer * 2, 4096)
                val dir = evidenceStore.pendingDirectory()
                val file = File(dir, "voice_${System.currentTimeMillis()}.pcm.enc")
                currentEncryptedFile = file
                val localRecorder = AudioRecord(MediaRecorder.AudioSource.MIC, sampleRate, channel, encoding, bufferSize)
                if (localRecorder.state != AudioRecord.STATE_INITIALIZED) {
                    localRecorder.release()
                    throw IllegalStateException("Microphone could not be initialized.")
                }
                recorder = localRecorder
                output = evidenceStore.createEncryptedOutput(file)
                startedAt = System.currentTimeMillis()
                bytesCaptured = 0L
                lastMetricsCallbackMillis = 0L
                localRecorder.startRecording()
                onStatus(VoiceStatus.LISTENING, "Victim voice monitoring is active.")
                val buffer = ShortArray(bufferSize / 2)
                while (recording) {
                    val read = localRecorder.read(buffer, 0, buffer.size)
                    if (read <= 0) continue
                    val bytes = ByteArray(read * 2)
                    var peak = 0
                    var sumSquares = 0.0
                    for (i in 0 until read) {
                        val sample = buffer[i].toInt()
                        val magnitude = kotlin.math.abs(sample)
                        if (magnitude > peak) peak = magnitude
                        sumSquares += sample.toDouble() * sample.toDouble()
                        bytes[i * 2] = (sample and 0xFF).toByte()
                        bytes[i * 2 + 1] = ((sample shr 8) and 0xFF).toByte()
                    }
                    output?.write(bytes)
                    bytesCaptured += bytes.size
                    val rms = sqrt(sumSquares / read).coerceAtLeast(1.0)
                    val db = (20.0 * log10(rms / 32768.0)).toFloat()
                    val now = System.currentTimeMillis()
                    val policy = performanceMonitor.policy()
                    if (now - lastMetricsCallbackMillis >= policy.voiceMetricIntervalMillis) {
                        lastMetricsCallbackMillis = now
                        onMetrics(VoiceSignalMetrics(now, now - startedAt, db, peak, bytesCaptured))
                    }
                }
                runCatching { localRecorder.stop() }
                localRecorder.release()
                recorder = null
                output?.flush()
                output?.close()
                output = null
                currentEncryptedFile = null
                onStatus(VoiceStatus.STOPPED, "Voice monitoring stopped.")
            } catch (security: SecurityException) {
                recording = false
                recorder?.release()
                recorder = null
                output?.close()
                output = null
                currentEncryptedFile?.let { evidenceStore.secureDelete(it) }
                currentEncryptedFile = null
                onStatus(VoiceStatus.PERMISSION_REQUIRED, "Microphone permission is unavailable.")
            } catch (t: Throwable) {
                recording = false
                recorder?.release()
                recorder = null
                output?.close()
                output = null
                currentEncryptedFile?.let { evidenceStore.secureDelete(it) }
                currentEncryptedFile = null
                onStatus(VoiceStatus.ERROR, "Microphone is unavailable.")
            }
        }
        return true
    }

    override fun stop() {
        recording = false
        runCatching { recorder?.stop() }
    }

    fun shutdown() {
        stop()
        executor.shutdownNow()
    }
}
