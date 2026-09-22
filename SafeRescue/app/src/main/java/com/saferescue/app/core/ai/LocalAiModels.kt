package com.saferescue.app.core.ai

/**
 * Explicit model lifecycle. A model is never represented as available unless a
 * real local model file was successfully loaded by the runtime.
 */
enum class LocalAiModelStatus {
    NOT_INSTALLED,
    LOADING,
    READY,
    ERROR
}

enum class LocalAiSignalType { VOICE, VISION, MOVEMENT }

data class LocalAiSignal(
    val type: LocalAiSignalType,
    val label: String,
    val confidence: Float,
    val timestampMillis: Long,
    val modelId: String,
    val modelVersion: String
)

data class LocalAiSnapshot(
    val status: LocalAiModelStatus = LocalAiModelStatus.NOT_INSTALLED,
    val modelId: String? = null,
    val modelVersion: String? = null,
    val signals: List<LocalAiSignal> = emptyList(),
    val message: String? = null
)

/** Contract for a local model. Implementations must not send input off-device. */
interface LocalAiModel {
    val modelId: String
    val modelVersion: String
    fun infer(input: FloatArray): FloatArray
    fun close()
}
