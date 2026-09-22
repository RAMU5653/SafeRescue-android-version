package com.saferescue.app

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.saferescue.app.core.emergency.EmergencyForegroundService
import com.saferescue.app.ui.theme.SafeRescueTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeoutOrNull

/** Compact emergency-only surface launched from the SafeRescue lock-screen notification. */
class LockScreenSosActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION") window.addFlags(android.view.WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED)
            @Suppress("DEPRECATION") window.addFlags(android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON)
        }
        setContent { LockScreenSosScreen() }
    }

    private fun startImmediateEmergency() {
        startForegroundService(Intent(this, EmergencyForegroundService::class.java).setAction(EmergencyForegroundService.ACTION_START))
        finishAndRemoveTask()
    }

    private fun startAccessibleEmergency() {
        startForegroundService(Intent(this, EmergencyForegroundService::class.java).setAction(EmergencyForegroundService.ACTION_ACCESSIBLE_START))
        finishAndRemoveTask()
    }

    @androidx.compose.runtime.Composable
    private fun LockScreenSosScreen() {
        var pressed by remember { mutableStateOf(false) }
        var progressMillis by remember { mutableLongStateOf(0L) }
        val requiredMillis = 5_000L

        LaunchedEffect(pressed) {
            if (pressed) {
                val startedAt = SystemClock.elapsedRealtime()
                while (pressed) {
                    progressMillis = (SystemClock.elapsedRealtime() - startedAt).coerceAtMost(requiredMillis)
                    if (progressMillis >= requiredMillis) {
                        pressed = false
                        progressMillis = requiredMillis
                        startImmediateEmergency()
                        break
                    }
                    delay(50)
                }
            } else progressMillis = 0L
        }

        SafeRescueTheme {
            Box(
                modifier = Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0x33071126), Color(0x3305070F)))).padding(16.dp),
                contentAlignment = Alignment.BottomEnd
            ) {
                Box(
                    modifier = Modifier.size(96.dp).clip(CircleShape).background(Color(0xFFE5124D)).pointerInput(Unit) {
                        awaitEachGesture {
                            val down = awaitFirstDown(requireUnconsumed = false)
                            pressed = true
                            withTimeoutOrNull(requiredMillis + 250L) {
                                while (pressed) {
                                    val event = awaitPointerEvent()
                                    if (event.changes.any { !it.pressed }) { pressed = false; break }
                                }
                            }
                            if (pressed) pressed = false
                            down.consume()
                        }
                    }.semantics {
                        contentDescription = "Small Emergency SOS button. Press and continuously hold for 5 seconds."
                        role = Role.Button
                        customActions = listOf(CustomAccessibilityAction("Start SOS with 5-second safety delay") {
                            startAccessibleEmergency(); true
                        })
                    },
                    contentAlignment = Alignment.Center
                ) {
                    Text(if (pressed) "${((requiredMillis - progressMillis + 999) / 1000).coerceAtLeast(0)}s" else "SOS", color = Color.White, fontSize = if (pressed) 20.sp else 24.sp)
                }
            }
        }
    }
}
