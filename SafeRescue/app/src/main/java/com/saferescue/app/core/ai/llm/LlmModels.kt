package com.saferescue.app.core.ai.llm

/**
 * Local language models available to SafeRescue.
 *
 * Qwen 2.5 1.5B / 3B: Main local reasoning / incident-context analysis.
 * Phi-3.5 Mini 3.8B: More capable secondary reasoning model (analyses results,
 * automatically captures evidence photos, generates "what happened" narratives,
 * and assesses safe vs unsafe places on maps).
 */
enum class LocalLlmId {
    QWEN_2_5_1_5B,
    QWEN_2_5_3B,
    PHI_3_5_MINI_3_8B,
    PHI_4_MINI,
    QWEN3_1_7B
}

enum class LocalLlmStatus {
    NOT_LOADED,
    LOADING,
    READY,
    FAILED,
    DISABLED
}

data class LocalLlmState(
    val model: LocalLlmId,
    val status: LocalLlmStatus = LocalLlmStatus.NOT_LOADED,
    val modelVersion: String? = null,
    val lastError: String? = null
)

data class LlmRequest(
    val prompt: String,
    val maxOutputTokens: Int = 256
) {
    init {
        require(prompt.isNotBlank()) {
            "LLM prompt must not be blank."
        }

        require(maxOutputTokens in 1..1024) {
            "maxOutputTokens must be between 1 and 1024."
        }
    }
}

data class LlmResponse(
    val model: LocalLlmId,
    val text: String,
    val latencyMillis: Long
)

data class LocalLlmSnapshot(
    val activeModel: LocalLlmId?,
    val phiState: LocalLlmState,
    val qwenState: LocalLlmState,
    val lastResponse: LlmResponse? = null
)
