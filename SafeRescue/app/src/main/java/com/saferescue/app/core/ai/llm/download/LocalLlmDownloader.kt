package com.saferescue.app.core.ai.llm.download

import com.saferescue.app.core.ai.llm.LocalLlmId
import com.saferescue.app.core.ai.llm.storage.LocalLlmStorage
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URI
import java.security.MessageDigest

class LocalLlmDownloader(
    private val storage: LocalLlmStorage
) {

    fun download(
        model: LocalLlmId,
        url: String,
        fileName: String,
        expectedSha256: String
    ): File {

        require(url.startsWith("https://")) {
            "Local LLM downloads require HTTPS."
        }

        require(expectedSha256.matches(Regex("[A-Fa-f0-9]{64}"))) {
            "Expected SHA-256 must contain exactly 64 hexadecimal characters."
        }

        URI(url)

        val directory = storage.modelDirectory(model)
        directory.mkdirs()

        val destination = storage.modelFile(model, fileName)
        val temporary = File(directory, "$fileName.part")

        if (temporary.exists()) {
            temporary.delete()
        }

        val connection =
            URI(url).toURL().openConnection() as HttpURLConnection

        connection.requestMethod = "GET"
        connection.connectTimeout = CONNECT_TIMEOUT_MS
        connection.readTimeout = READ_TIMEOUT_MS
        connection.instanceFollowRedirects = false

        try {
            connection.connect()

            require(connection.responseCode in 200..299) {
                "Model download failed with HTTP ${connection.responseCode}."
            }

            BufferedInputStream(connection.inputStream).use { input ->
                FileOutputStream(temporary).use { output ->
                    val buffer = ByteArray(BUFFER_SIZE)

                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break

                        if (count > 0) {
                            output.write(buffer, 0, count)
                        }
                    }

                    output.fd.sync()
                }
            }

            val actualSha256 = sha256(temporary)

            require(
                actualSha256.equals(
                    expectedSha256,
                    ignoreCase = true
                )
            ) {
                "Downloaded model SHA-256 verification failed."
            }

            if (destination.exists()) {
                destination.delete()
            }

            require(temporary.renameTo(destination)) {
                "Could not finalize verified model file."
            }

            return destination
        } catch (t: Throwable) {
            temporary.delete()
            throw t
        } finally {
            connection.disconnect()
        }
    }

    private fun sha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")

        file.inputStream().use { input ->
            val buffer = ByteArray(BUFFER_SIZE)

            while (true) {
                val count = input.read(buffer)
                if (count < 0) break

                if (count > 0) {
                    digest.update(buffer, 0, count)
                }
            }
        }

        return digest.digest()
            .joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val BUFFER_SIZE = 64 * 1024
        private const val CONNECT_TIMEOUT_MS = 15_000
        private const val READ_TIMEOUT_MS = 60_000
    }
}
