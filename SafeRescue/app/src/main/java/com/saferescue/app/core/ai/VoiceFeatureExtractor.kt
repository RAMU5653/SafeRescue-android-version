package com.saferescue.app.core.ai

import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Lightweight deterministic feature extraction. It creates model input only; it
 * does not label speech or claim distress by itself.
 */
object VoiceFeatureExtractor {
    fun fromPcm16(samples: ShortArray): FloatArray {
        val features = FloatArray(LocalAiRepository.INPUT_SIZE)
        if (samples.isEmpty()) return features
        val frameSize = maxOf(1, samples.size / features.size)
        for (i in features.indices) {
            val start = i * frameSize
            val end = minOf(samples.size, start + frameSize)
            if (start >= end) break
            var sum = 0.0
            var absolute = 0.0
            for (j in start until end) {
                val normalized = samples[j] / 32768f
                sum += normalized * normalized
                absolute += abs(normalized.toDouble())
            }
            val count = (end - start).toDouble()
            val rms = sqrt(sum / count).toFloat()
            val meanAbs = (absolute / count).toFloat()
            features[i] = (rms * 0.7f + meanAbs * 0.3f).coerceIn(0f, 1f)
        }
        return features
    }
}
