package com.saferescue.app.core.ai.llm

/**
 * SafeRescue placeholder for a local LLM that is not installed.
 *
 * This is intentionally NOT an AI implementation.
 * It allows the application architecture to represent an unavailable
 * model without pretending that inference is available.
 */
class UnavailableLocalLlm(
    override val id: LocalLlmId,
    override val version: String = "not-installed"
) : LocalLlm {

    override fun generate(request: LlmRequest): String {
        throw IllegalStateException(
            "${id.name} local model runtime is not installed."
        )
    }

    override fun close() {
        // Nothing to release.
    }
}
