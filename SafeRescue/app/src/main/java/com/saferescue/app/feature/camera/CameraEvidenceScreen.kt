package com.saferescue.app.feature.camera

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Cameraswitch
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PhotoCamera
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.saferescue.app.core.camera.CameraLens
import java.io.File
import com.saferescue.app.core.evidence.SecureEvidenceStore

@Composable
fun CameraEvidenceScreen(
    onClose: () -> Unit,
    onCaptureSaved: (File) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val controller = remember { CameraEvidenceController(context) }
    val evidenceStore = remember { SecureEvidenceStore(context) }
    var lens by remember { mutableStateOf(CameraLens.BACK) }
    var permissionGranted by remember { mutableStateOf(controller.hasPermission()) }
    var message by remember { mutableStateOf<String?>(null) }
    var ready by remember { mutableStateOf(false) }
    var previewView by remember { mutableStateOf<PreviewView?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        permissionGranted = granted
        if (!granted) message = "Camera access was denied. Emergency monitoring continues."
    }

    DisposableEffect(Unit) { onDispose { controller.shutdown() } }

    LaunchedEffect(permissionGranted, lens, previewView) {
        val view = previewView ?: return@LaunchedEffect
        if (!permissionGranted) return@LaunchedEffect
        val preview = Preview.Builder().build().also { it.surfaceProvider = view.surfaceProvider }
        val selector = if (lens == CameraLens.FRONT) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA
        controller.bind(lifecycleOwner, preview, selector, { ready = true; message = "Camera ready" }) { ready = false; message = it }
    }

    Surface(color = MaterialTheme.colorScheme.background, modifier = Modifier.fillMaxSize()) {
        if (!permissionGranted) {
            Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(Icons.Rounded.PhotoCamera, contentDescription = null, modifier = Modifier.size(56.dp))
                Spacer(Modifier.height(14.dp))
                Text("Camera evidence", modifier = Modifier.semantics { heading() }, style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(8.dp))
                Text("Camera access is optional. It never blocks or cancels an active SOS.")
                Spacer(Modifier.height(18.dp))
                Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) { Text("Allow camera") }
                Spacer(Modifier.height(8.dp))
                Button(onClick = onClose) { Text("Continue without camera") }
            }
        } else {
            Box(Modifier.fillMaxSize()) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = {
                        PreviewView(it).apply {
                            scaleType = PreviewView.ScaleType.FILL_CENTER
                            previewView = this
                        }
                    }
                )
                Column(Modifier.fillMaxWidth().align(Alignment.TopCenter).padding(18.dp)) {
                    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onClose) { Icon(Icons.Rounded.Close, contentDescription = "Close camera") }
                        Spacer(Modifier.weight(1f))
                        Text(if (lens == CameraLens.FRONT) "Front camera" else "Back camera")
                        IconButton(onClick = { lens = if (lens == CameraLens.FRONT) CameraLens.BACK else CameraLens.FRONT }) { Icon(Icons.Rounded.Cameraswitch, contentDescription = "Switch between front and back camera") }
                    }
                }
                Column(Modifier.align(Alignment.BottomCenter).fillMaxWidth().padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    if (message != null) Text(message!!, color = MaterialTheme.colorScheme.onSurface, modifier = Modifier.padding(8.dp))
                    Button(
                        enabled = ready,
                        onClick = {
                            val dir = File(context.cacheDir, "camera_capture_tmp").apply { mkdirs() }
                            val file = File(dir, "capture_${System.currentTimeMillis()}_${if (lens == CameraLens.FRONT) "front" else "back"}.jpg")
                            message = "Capturing…"
                            ready = false
                            controller.capture(file) { result ->
                                result.onSuccess { saved ->
                                    runCatching {
                                        val secure = File(evidenceStore.pendingDirectory(), saved.nameWithoutExtension + ".jpg.enc")
                                        val encrypted = evidenceStore.encryptFile(saved, secure)
                                        evidenceStore.secureDelete(saved)
                                        message = "Captured and encrypted locally. SHA-256: ${encrypted.sha256.take(12)}…"
                                        ready = true
                                        onCaptureSaved(File(encrypted.path))
                                    }.onFailure {
                                        evidenceStore.secureDelete(saved)
                                        message = "Capture could not be secured. Emergency monitoring continues."
                                        ready = true
                                    }
                                }.onFailure {
                                    message = "Capture failed. Emergency monitoring continues."
                                    ready = true
                                }
                            }
                        }
                    ) { Text("Capture photo") }
                }
            }
        }
    }
}
