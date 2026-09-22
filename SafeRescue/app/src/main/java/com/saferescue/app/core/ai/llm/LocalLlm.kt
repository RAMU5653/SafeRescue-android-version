package com.saferescue.app.core.ai.llm

/**
 * Contract for an on-device language model.
 *
 * Implementations must keep inference local to the Android device.
 * No network access is permitted as part of this interface.
 */
interface LocalLlm {
    val id: LocalLlmId
    val version: String

    fun generate(request: LlmRequest): String

    fun close()
}
