package com.saferescue.app.feature.camera

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.lifecycle.LifecycleOwner
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class CameraEvidenceController(context: Context) {
    private val appContext = context.applicationContext
    private val cameraExecutor: ExecutorService = Executors.newSingleThreadExecutor()
    private var imageCapture: ImageCapture? = null

    fun hasPermission(): Boolean = ContextCompat.checkSelfPermission(appContext, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED

    fun bind(
        lifecycleOwner: LifecycleOwner,
        preview: Preview,
        lens: CameraSelector,
        onReady: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (!hasPermission()) {
            onError("Camera permission is required.")
            return
        }
        val future = ProcessCameraProvider.getInstance(appContext)
        future.addListener({
            try {
                val provider = future.get()
                val capture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()
                provider.unbindAll()
                provider.bindToLifecycle(lifecycleOwner, lens, preview, capture)
                imageCapture = capture
                onReady()
            } catch (e: Exception) {
                imageCapture = null
                onError("Camera unavailable.")
            }
        }, ContextCompat.getMainExecutor(appContext))
    }

    fun capture(outputFile: File, onResult: (Result<File>) -> Unit) {
        val capture = imageCapture ?: run {
            onResult(Result.failure(IllegalStateException("Camera is not ready.")))
            return
        }
        capture.takePicture(
            ImageCapture.OutputFileOptions.Builder(outputFile).build(),
            cameraExecutor,
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    onResult(Result.success(outputFile))
                }
                override fun onError(exception: ImageCaptureException) {
                    onResult(Result.failure(exception))
                }
            }
        )
    }

    fun shutdown() {
        imageCapture = null
        cameraExecutor.shutdown()
    }
}
