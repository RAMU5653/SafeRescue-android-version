package com.saferescue.app.core.ai.llm

/**
 * SafeRescue local LLM failover controller.
 *
 * Priority:
 * 1. Phi-4-mini
 * 2. Qwen3 1.7B
 *
 * Qwen is a full-work fallback, not a reduced-capability emergency mode.
 *
 * The orchestrator is deliberately isolated from the emergency state machine.
 * LLM failure must never stop or alter SOS, countdown, evidence, GPS,
 * notifications, or emergency-state transitions.
 */
class LocalLlmOrchestrator(
    private val phi: LocalLlm?,
    private val qwen: LocalLlm?
) {

    private var activeModel: LocalLlmId? = null
    private var phiState = LocalLlmState(LocalLlmId.PHI_4_MINI)
    private var qwenState = LocalLlmState(LocalLlmId.QWEN3_1_7B)

    /**
     * Selects the preferred model.
     *
     * Phi is always preferred when it is available.
     * Qwen is selected only when Phi cannot be used.
     */
    fun initialize(): LocalLlmSnapshot {
        if (phi != null) {
            phiState = LocalLlmState(
                model = LocalLlmId.PHI_4_MINI,
                status = LocalLlmStatus.READY,
                modelVersion = phi.version
            )
            activeModel = LocalLlmId.PHI_4_MINI
        } else {
            phiState = LocalLlmState(
                model = LocalLlmId.PHI_4_MINI,
                status = LocalLlmStatus.FAILED,
                lastError = "Phi-4-mini runtime is unavailable."
            )
        }

        if (qwen != null) {
            qwenState = LocalLlmState(
                model = LocalLlmId.QWEN3_1_7B,
                status = LocalLlmStatus.READY,
                modelVersion = qwen.version
            )

            if (activeModel == null) {
                activeModel = LocalLlmId.QWEN3_1_7B
            }
        } else {
            qwenState = LocalLlmState(
                model = LocalLlmId.QWEN3_1_7B,
                status = LocalLlmStatus.FAILED,
                lastError = "Qwen3 1.7B runtime is unavailable."
            )
        }

        return snapshot()
    }

    /**
     * Runs the request using Phi first.
     *
     * If Phi fails during inference, Qwen receives the same full request.
     *
     * If both fail, null is returned. The caller must continue operating
     * without AI assistance.
     */
    @Synchronized
    fun generate(request: LlmRequest): LlmResponse? {
        val phiResult = runModel(phi, LocalLlmId.PHI_4_MINI, request)

        if (phiResult != null) {
            activeModel = LocalLlmId.PHI_4_MINI
            return phiResult
        }

        val qwenResult = runModel(qwen, LocalLlmId.QWEN3_1_7B, request)

        if (qwenResult != null) {
            activeModel = LocalLlmId.QWEN3_1_7B
            return qwenResult
        }

        activeModel = null
        return null
    }

    /**
     * Attempts to restore Phi as the preferred model.
     *
     * No emergency operation depends on this recovery.
     */
    @Synchronized
    fun preferPhiAgain(): LocalLlmSnapshot {
        if (phi != null && phiState.status != LocalLlmStatus.FAILED) {
            activeModel = LocalLlmId.PHI_4_MINI
        }

        return snapshot()
    }

    fun snapshot(): LocalLlmSnapshot {
        return LocalLlmSnapshot(
            activeModel = activeModel,
            phiState = phiState,
            qwenState = qwenState
        )
    }

    fun close() {
        runCatching { phi?.close() }
        runCatching { qwen?.close() }

        activeModel = null
        phiState = phiState.copy(status = LocalLlmStatus.NOT_LOADED)
        qwenState = qwenState.copy(status = LocalLlmStatus.NOT_LOADED)
    }

    private fun runModel(
        model: LocalLlm?,
        id: LocalLlmId,
        request: LlmRequest
    ): LlmResponse? {
        if (model == null) {
            markFailed(id, "Local runtime is unavailable.")
            return null
        }

        val start = System.currentTimeMillis()

        return try {
            val text = model.generate(request)

            if (text.isBlank()) {
                markFailed(id, "Local model returned an empty response.")
                null
            } else {
                markReady(id, model.version)

                LlmResponse(
                    model = id,
                    text = text,
                    latencyMillis = System.currentTimeMillis() - start
                )
            }
        } catch (t: Throwable) {
            markFailed(
                id,
                t.message?.take(MAX_ERROR_LENGTH)
                    ?: "Local model inference failed."
            )
            null
        }
    }

    private fun markReady(id: LocalLlmId, version: String) {
        when (id) {
            LocalLlmId.PHI_4_MINI -> {
                phiState = LocalLlmState(
                    model = id,
                    status = LocalLlmStatus.READY,
                    modelVersion = version
                )
            }

            LocalLlmId.QWEN3_1_7B -> {
                qwenState = LocalLlmState(
                    model = id,
                    status = LocalLlmStatus.READY,
                    modelVersion = version
                )
            }
        }
    }

    private fun markFailed(id: LocalLlmId, error: String) {
        when (id) {
            LocalLlmId.PHI_4_MINI -> {
                phiState = phiState.copy(
                    status = LocalLlmStatus.FAILED,
                    lastError = error
                )
            }

            LocalLlmId.QWEN3_1_7B -> {
                qwenState = qwenState.copy(
                    status = LocalLlmStatus.FAILED,
                    lastError = error
                )
            }
        }
    }

    companion object {
        private const val MAX_ERROR_LENGTH = 160
    }
}
