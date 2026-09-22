package com.saferescue.app.core.ai

import android.content.res.AssetFileDescriptor
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.tensorflow.lite.Interpreter

/**
 * Generic TensorFlow Lite adapter for a bundled on-device model.
 *
 * The model is loaded exclusively from APK assets.
 * No model download or network inference is performed.
 *
 * Tensor shapes are inspected at runtime so this adapter is not tied to the
 * previous fixed 64-input/2-output placeholder contract.
 */
class TfliteLocalAiModel(
    private val descriptor: AssetFileDescriptor,
    override val modelId: String,
    override val modelVersion: String
) : LocalAiModel {

    private val modelBuffer: ByteBuffer =
        FileInputStream(descriptor.fileDescriptor).use { input ->
            input.channel.position(descriptor.startOffset)

            val bytes = ByteArray(descriptor.declaredLength.toInt())
            var offset = 0

            while (offset < bytes.size) {
                val read = input.read(bytes, offset, bytes.size - offset)
                if (read <= 0) break
                offset += read
            }

            require(offset == bytes.size) {
                "Incomplete local model asset"
            }

            ByteBuffer.allocateDirect(bytes.size)
                .order(ByteOrder.nativeOrder())
                .apply {
                    put(bytes)
                    rewind()
                }
        }

    private val interpreter = Interpreter(
        modelBuffer,
        Interpreter.Options().apply {
            setNumThreads(2)
        }
    )

    val inputTensorShape: IntArray
        get() = interpreter.getInputTensor(0).shape()

    val inputTensorType: org.tensorflow.lite.DataType
        get() = interpreter.getInputTensor(0).dataType()

    val outputTensorShape: IntArray
        get() = interpreter.getOutputTensor(0).shape()

    val outputTensorType: org.tensorflow.lite.DataType
        get() = interpreter.getOutputTensor(0).dataType()

    /**
     * Runs inference using the caller-provided input tensor.
     *
     * The caller is responsible for providing data compatible with the model's
     * input tensor shape and type.
     */
    override fun infer(input: FloatArray): FloatArray {
        val expectedElements =
            inputTensorShape.fold(1) { total, dimension -> total * dimension }

        require(input.size == expectedElements) {
            "Model expects $expectedElements float values, received ${input.size}"
        }

        val outputElements =
            outputTensorShape.fold(1) { total, dimension -> total * dimension }

        val output = FloatArray(outputElements)

        interpreter.run(
            input.reshapeForTensor(inputTensorShape),
            output.reshapeForTensor(outputTensorShape)
        )

        return output
    }

    override fun close() {
        interpreter.close()
        descriptor.close()
    }

    private fun FloatArray.reshapeForTensor(shape: IntArray): Any {
        return when {
            shape.size == 1 -> this

            shape.size == 2 -> {
                Array(shape[0]) { row ->
                    FloatArray(shape[1]) { column ->
                        this[row * shape[1] + column]
                    }
                }
            }

            shape.size == 3 -> {
                Array(shape[0]) {
                    Array(shape[1]) { row ->
                        FloatArray(shape[2]) { column ->
                            this[row * shape[2] + column]
                        }
                    }
                }
            }

            shape.size == 4 -> {
                Array(shape[0]) {
                    Array(shape[1]) {
                        Array(shape[2]) { row ->
                            FloatArray(shape[3]) { column ->
                                this[
                                    (((it * shape[2] + row) * shape[3]) + column)
                                ]
                            }
                        }
                    }
                }
            }

            else -> error(
                "Unsupported TFLite tensor rank: ${shape.size}"
            )
        }
    }
}
