package com.saferescue.app.core.camera

import java.io.File

interface CameraRepository {
    fun capturePhoto(lens: CameraLens, outputFile: File, onResult: (Result<File>) -> Unit)
}
