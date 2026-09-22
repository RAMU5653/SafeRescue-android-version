package com.saferescue.app.core.ai.llm.storage

import android.content.Context
import com.saferescue.app.core.ai.llm.LocalLlmId
import java.io.File

class LocalLlmStorage(
    context: Context
) {

    private val root =
        File(context.filesDir, "local_llm_models")

    init {
        root.mkdirs()
    }

    fun modelDirectory(id: LocalLlmId): File {
        return File(root, id.name.lowercase())
    }

    fun modelFile(id: LocalLlmId, fileName: String): File {
        return File(modelDirectory(id), fileName)
    }

    fun isInstalled(
        id: LocalLlmId,
        fileName: String
    ): Boolean {
        val file = modelFile(id, fileName)
        return file.exists() && file.isFile && file.length() > 0
    }

    fun deleteModel(id: LocalLlmId) {
        modelDirectory(id).deleteRecursively()
    }
}
