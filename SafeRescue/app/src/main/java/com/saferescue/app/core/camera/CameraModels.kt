package com.saferescue.app.core.camera

enum class CameraLens { BACK, FRONT }

enum class CameraStatus { INACTIVE, PERMISSION_REQUIRED, READY, CAPTURING, SAVED, UNAVAILABLE, ERROR }

data class CameraSnapshot(
    val status: CameraStatus = CameraStatus.INACTIVE,
    val lens: CameraLens = CameraLens.BACK,
    val lastCapturePath: String? = null,
    val message: String? = null
)
