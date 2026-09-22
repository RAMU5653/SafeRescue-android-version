package com.saferescue.app.core.ai

import android.content.Context
import android.content.res.AssetFileDescriptor
import java.io.FileNotFoundException
import kotlin.math.max
import kotlin.math.min

/**
 * Loads real local TFLite models from APK assets. Missing models are reported as
 * unavailable instead of being replaced with a fabricated classifier.
 */
class LocalAiRepository(context: Context) {
    private val appContext = context.applicationContext
    private var voiceModel: LocalAiModel? = null

    fun loadVoiceModel(): LocalAiSnapshot {
        if (voiceModel != null) {
            val model = voiceModel!!
            return LocalAiSnapshot(LocalAiModelStatus.READY, model.modelId, model.modelVersion)
        }
        return try {
            val descriptor = appContext.assets.openFd(VOICE_MODEL_ASSET)
            val model = TfliteLocalAiModel(
                descriptor = descriptor,
                modelId = "saferescue.voice",
                modelVersion = "1.0.0"
            )
            voiceModel = model
            LocalAiSnapshot(LocalAiModelStatus.READY, model.modelId, model.modelVersion)
        } catch (_: FileNotFoundException) {
            LocalAiSnapshot(
                status = LocalAiModelStatus.NOT_INSTALLED,
                modelId = "saferescue.voice",
                modelVersion = "1.0.0",
                message = "No validated voice model is bundled. Local AI remains unavailable."
            )
        } catch (_: Throwable) {
            LocalAiSnapshot(
                status = LocalAiModelStatus.ERROR,
                modelId = "saferescue.voice",
                modelVersion = "1.0.0",
                message = "The local voice model could not be loaded."
            )
        }
    }

    /**
     * Runs a real loaded model on a caller-provided feature vector. This method
     * deliberately performs no heuristic classification when the model is absent.
     */
    fun inferVoice(features: FloatArray): LocalAiSignal? {
        val model = voiceModel ?: return null
        val input = FloatArray(INPUT_SIZE)
        val copySize = min(features.size, INPUT_SIZE)
        features.copyInto(input, 0, 0, copySize)
        val output = model.infer(input)
        if (output.size < 2) return null
        val distressProbability = output[1].coerceIn(0f, 1f)
        return LocalAiSignal(
            type = LocalAiSignalType.VOICE,
            label = if (distressProbability >= 0.5f) "voice_signal" else "no_voice_signal",
            confidence = max(distressProbability, 1f - distressProbability),
            timestampMillis = System.currentTimeMillis(),
            modelId = model.modelId,
            modelVersion = model.modelVersion
        )
    }

    fun close() {
        voiceModel?.close()
        voiceModel = null
    }

    companion object {
        const val VOICE_MODEL_ASSET = "models/voice_distress.tflite"
        const val INPUT_SIZE = 64
    }
}
