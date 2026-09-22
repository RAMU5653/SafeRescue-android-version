package com.saferescue.app.feature.home

import com.saferescue.app.R
import androidx.compose.ui.platform.LocalContext
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LocationOn
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.People
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.Shield
import androidx.compose.material.icons.rounded.Sos
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.LocalPolice
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.LocalFireDepartment
import androidx.compose.material.icons.rounded.Place
import androidx.compose.material.icons.rounded.PersonAdd
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import com.saferescue.app.core.notification.SafeRescueNotificationManager
import com.saferescue.app.core.location.LocationStatus
import com.saferescue.app.core.voice.VoiceStatus
import com.saferescue.app.core.risk.RiskSeverity
import com.saferescue.app.core.timeline.TimelineEvent
import com.saferescue.app.core.report.IncidentReport
import com.saferescue.app.core.report.PdfIncidentReportGenerator
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.saferescue.app.auth.AuthUser
import com.saferescue.app.core.emergency.EmergencyState
import com.saferescue.app.feature.emergency.EmergencyUiViewModel
import com.saferescue.app.feature.camera.CameraEvidenceScreen
import kotlinx.coroutines.delay
import android.os.Build

@Composable
fun DashboardRoot(
    user: AuthUser,
    message: String?,
    onLogout: () -> Unit,
    vm: HomeViewModel = viewModel(),
    emergencyVm: EmergencyUiViewModel = viewModel()
) {
    val state by vm.state.collectAsStateWithLifecycle()
    val emergency by emergencyVm.state.collectAsStateWithLifecycle()
    var cameraOpen by remember { mutableStateOf(false) }
    var lastCaptureName by remember { mutableStateOf<String?>(null) }
    val contactsVm: TrustedContactsViewModel = viewModel()
    val safetyVm: SafetyGuidanceViewModel = viewModel()
    val context = LocalContext.current
    val sensorPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { grants ->
        val locationGranted = grants[Manifest.permission.ACCESS_FINE_LOCATION] == true || grants[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        val microphoneGranted = grants[Manifest.permission.RECORD_AUDIO] == true
        if (locationGranted) emergencyVm.startLocation()
        if (microphoneGranted) emergencyVm.startVoice()
    }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        contactsVm.refreshNotificationPermissionState()
        if (it) SafeRescueNotificationManager(context).showLockScreenEmergencyAccess()
    }

    if (cameraOpen) {
        CameraEvidenceScreen(
            onClose = { cameraOpen = false },
            onCaptureSaved = { lastCaptureName = it.name }
        )
        return
    }

    Surface(color = Color(0xFF050D20), modifier = Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            Box(Modifier) {
                AnimatedContent(
                    targetState = state.selectedTab,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    label = "dashboard-tab"
                ) { tab ->
                    when (tab) {
                        HomeTab.HOME -> HomeTabContent(user, message, vm, emergency, emergencyVm, sensorPermissionLauncher, onOpenCamera = { cameraOpen = true }, lastCaptureName = lastCaptureName)
                        HomeTab.SAFETY -> SafetyTabContent(contactsVm, notificationPermissionLauncher, emergency.latestLocation, safetyVm)
                        HomeTab.EVIDENCE -> EvidenceTabContent(emergency)
                        HomeTab.ME -> MeTabContent(user, onLogout)
                    }
                }
            }
            BottomNav(selected = state.selectedTab, onSelect = vm::selectTab)
        }
    }
}

@Composable
private fun HomeTabContent(
    user: AuthUser,
    message: String?,
    vm: HomeViewModel,
    emergency: EmergencyUiViewModel.UiState,
    emergencyVm: EmergencyUiViewModel,
    locationPermissionLauncher: androidx.activity.result.ActivityResultLauncher<Array<String>>,
    onOpenCamera: () -> Unit,
    lastCaptureName: String?
) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Image(
                painter = painterResource(R.drawable.saferescue_logo),
                contentDescription = "SafeRescue — Your Safety Our Priority",
                modifier = Modifier.width(172.dp).height(78.dp),
                contentScale = ContentScale.Fit
            )
            Spacer(Modifier)
            Box(Modifier.size(44.dp).clip(CircleShape).background(Color(0xFF12264A)), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.AccountCircle, contentDescription = "Profile", tint = Color(0xFF72D8FF), modifier = Modifier.size(29.dp))
            }
        }
        Column {
            Text("Good evening", color = Color(0xFF9DAAC4), fontSize = 13.sp)
            Text(user.name.ifBlank { "SafeRescue user" }, fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
            Text("Your safety dashboard", color = Color(0xFF8E9BB6), fontSize = 13.sp)
        }
        StatusCard(emergency.state)
        if (emergency.state == EmergencyState.IDLE || emergency.state == EmergencyState.CANCELLED || emergency.state == EmergencyState.COMPLETED) SosHoldCard(
            onComplete = {
                emergencyVm.startEmergency()
                locationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION,
                        Manifest.permission.RECORD_AUDIO,
                        Manifest.permission.CAMERA,
                        Manifest.permission.SEND_SMS
                    )
                )
            },
            onAccessibleStart = emergencyVm::startAccessibleEmergency,
            onAccessibleCancel = emergencyVm::cancelAccessibleEmergency
        )
        else ActiveEmergencyCard(emergency, emergencyVm)
        Text("Safety controls", modifier = Modifier.semantics { heading() }, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FeatureCard(Modifier, Icons.Rounded.LocationOn, "Location", "Phase 6", when (emergency.locationStatus) { LocationStatus.ACTIVE -> "Tracking active"; LocationStatus.PERMISSION_REQUIRED -> "Permission needed"; LocationStatus.UNAVAILABLE -> "Unavailable"; LocationStatus.ERROR -> "Service error"; else -> "Ready" })
            FeatureCard(Modifier, Icons.Rounded.Security, "Risk engine", "Phase 9", riskLabel(emergency.riskScore, emergency.riskSeverity))
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            FeatureCard(Modifier, Icons.Rounded.Videocam, "Evidence", "Phase 7", if (emergency.state == EmergencyState.EMERGENCY_ACTIVE || emergency.state == EmergencyState.MONITORING || emergency.state == EmergencyState.CANCELLATION_PENDING) "Camera available" else "Start SOS first", onClick = if (emergency.state == EmergencyState.EMERGENCY_ACTIVE || emergency.state == EmergencyState.MONITORING || emergency.state == EmergencyState.CANCELLATION_PENDING) onOpenCamera else null)
            FeatureCard(Modifier, Icons.Rounded.People, "Contacts", "Phase 14", "Manage trusted contacts", onClick = { vm.selectTab(HomeTab.SAFETY) })
        }
        if (message != null) InfoCard(Icons.Rounded.CheckCircle, "Account ready", message)
        InfoCard(Icons.Rounded.LocationOn, "Phase 6 • Location", emergency.locationMessage ?: if (emergency.latestLocation != null) "Latest fix ±${emergency.latestLocation.accuracyMeters.toInt()} m" else "Location monitoring starts after SOS when permission is granted. GPS failure never stops the emergency.")
        if (lastCaptureName != null) InfoCard(Icons.Rounded.Videocam, "Phase 7 • Camera", "Temporary local capture: $lastCaptureName")
        RiskCard(emergency)
        InfoCard(Icons.Rounded.Security, "Phase 8 • Victim voice", emergency.voiceMessage ?: when (emergency.voiceStatus) { VoiceStatus.LISTENING -> "Microphone monitoring active • raw voice stays local"; VoiceStatus.PERMISSION_REQUIRED -> "Microphone permission required"; VoiceStatus.UNAVAILABLE, VoiceStatus.ERROR -> "Microphone unavailable • emergency continues"; VoiceStatus.STOPPED -> "Voice monitoring stopped"; else -> "Starts after SOS when microphone permission is granted" })
        Spacer(Modifier.height(8.dp))
    }
}


@Composable
private fun RiskCard(emergency: EmergencyUiViewModel.UiState) {
    val active = emergency.state == EmergencyState.EMERGENCY_ACTIVE || emergency.state == EmergencyState.MONITORING || emergency.state == EmergencyState.CANCELLATION_PENDING
    val title = if (active) "Live risk assessment" else "Risk engine"
    val body = if (active) {
        "${emergency.riskScore}/100 • ${emergency.riskSeverity.name} • ${emergency.riskFactorCount} factor(s)"
    } else {
        "Waiting for an active SOS"
    }
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF0D2140))) {
        Column(Modifier.padding(17.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Security, contentDescription = "Risk score", tint = Color(0xFF55D7FF), modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(10.dp))
                Column(Modifier) {
                    Text(title, modifier = Modifier.semantics { heading() }, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(body, color = Color(0xFFB8C4D9), fontSize = 13.sp)
                }
                Text("${emergency.riskScore}", fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF5B4BDB))
            }
            Spacer(Modifier.height(7.dp))
            Text(if (emergency.riskIsHeuristic) "Heuristic signal only — not proof of danger. Manual SOS remains authoritative." else "Risk assessment", color = Color(0xFF8E9BB6), fontSize = 11.sp)
        }
    }
}

private fun riskLabel(score: Int, severity: RiskSeverity): String = when {
    score == 0 -> "Ready"
    else -> "$score/100 • ${severity.name.lowercase().replaceFirstChar { it.uppercase() }}"
}

@Composable
private fun StatusCard(state: EmergencyState) {
    val active = state == EmergencyState.EMERGENCY_ACTIVE || state == EmergencyState.MONITORING || state == EmergencyState.CANCELLATION_PENDING
    Card(Modifier.fillMaxWidth().semantics { liveRegion = LiveRegionMode.Polite }, shape = RoundedCornerShape(26.dp), colors = CardDefaults.cardColors(containerColor = if (active) Color(0xFF4B1724) else Color(0xFF17172A)), elevation = CardDefaults.cardElevation(4.dp)) {
        Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(54.dp).clip(CircleShape).background(Color(0xFF2C2A4C)), contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Shield, contentDescription = "Emergency status", tint = Color(0xFFC8BFFF), modifier = Modifier.size(31.dp)) }
            Spacer(Modifier.width(14.dp))
            Column(Modifier) {
                Text(if (active) "EMERGENCY ACTIVE" else state.name.replace('_', ' '), color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.ExtraBold)
                Text(if (active) "Manual SOS has priority" else "No emergency is active", color = Color(0xFFE2DFEA), fontSize = 13.sp)
                Text("Phase 5 • emergency control", color = Color(0xFFAAA6BA), fontSize = 12.sp)
            }
        }
    }
}

@Composable
private fun SosHoldCard(
    onComplete: () -> Unit,
    onAccessibleStart: () -> Unit,
    onAccessibleCancel: () -> Unit
) {
    var holding by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(holding) {
        if (!holding) { progress = 0f; return@LaunchedEffect }
        val started = System.currentTimeMillis()
        while (holding) {
            progress = ((System.currentTimeMillis() - started) / 5000f).coerceIn(0f, 1f)
            if (progress >= 1f) { holding = false; haptic.performHapticFeedback(HapticFeedbackType.LongPress); onComplete(); break }
            delay(50)
        }
    }
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF321126)), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF7C315E))) {
        Column(Modifier.fillMaxWidth().padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Emergency access", modifier = Modifier.semantics { heading() }, fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text("Press and continuously hold for 5 seconds.", color = Color(0xFFD5B9C8), fontSize = 12.sp)
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .semantics {
                        progressBarRangeInfo = ProgressBarRangeInfo(progress, 0f..1f)

                        contentDescription = "Emergency SOS. Press and continuously hold for 5 seconds."
                        role = Role.Button
                        customActions = listOf(
                            CustomAccessibilityAction("Start SOS with 5-second safety delay") {
                                onAccessibleStart()
                                true
                            },
                            CustomAccessibilityAction("Cancel pending SOS start") {
                                onAccessibleCancel()
                                true
                            }
                        )
                    }
                    .pointerInput(Unit) { detectTapGestures(onPress = { holding = true; tryAwaitRelease(); holding = false }) },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE51E4D))
            ) {
                Icon(Icons.Rounded.Sos, contentDescription = "Hold SOS for five seconds", modifier = Modifier.size(22.dp)); Spacer(Modifier.width(8.dp)); Text(if (holding) "HOLD SOS • ${(progress * 5).toInt() + 1}s" else "HOLD SOS • 5 SEC", fontWeight = FontWeight.ExtraBold)
            }
            Spacer(Modifier.height(8.dp)); androidx.compose.material3.LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().semantics { progressBarRangeInfo = ProgressBarRangeInfo(progress, 0f..1f) }, trackColor = Color(0xFFFFDCE6))
        }
    }
}

@Composable
private fun ActiveEmergencyCard(emergency: EmergencyUiViewModel.UiState, emergencyVm: EmergencyUiViewModel) {
    var holding by remember { mutableStateOf(false) }
    var progress by remember { mutableStateOf(0f) }
    val haptic = LocalHapticFeedback.current
    LaunchedEffect(holding) {
        if (!holding) { progress = 0f; return@LaunchedEffect }
        val started = System.currentTimeMillis()
        while (holding) {
            progress = ((System.currentTimeMillis() - started) / 5000f).coerceIn(0f, 1f)
            if (progress >= 1f) { holding = false; haptic.performHapticFeedback(HapticFeedbackType.LongPress); emergencyVm.completeCancellationHold(); break }
            delay(50)
        }
    }
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(24.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF26131A))) {
        Column(Modifier.padding(20.dp)) {
            Text("EMERGENCY MODE", modifier = Modifier.semantics { heading() }, color = Color.White, fontSize = 21.sp, fontWeight = FontWeight.ExtraBold)
            Text(if (emergency.state == EmergencyState.CANCELLATION_PENDING) "CANCELLATION PENDING" else formatRemaining(emergency.remainingSeconds), color = Color(0xFFFFD6E1), fontSize = 34.sp, fontWeight = FontWeight.ExtraBold)
            Text("SOS ACTIVE • manual activation remains authoritative", color = Color(0xFFE9CAD4), fontSize = 12.sp)
            Spacer(Modifier.height(14.dp)); SignalRow("Victim Voice", when (emergency.voiceStatus) { VoiceStatus.LISTENING -> "ACTIVE"; VoiceStatus.PERMISSION_REQUIRED -> "PERMISSION"; VoiceStatus.ERROR, VoiceStatus.UNAVAILABLE -> "UNAVAILABLE"; VoiceStatus.STOPPED -> "STOPPED"; else -> "READY" }) ; SignalRow("Camera", "Phase 7") ; SignalRow("GPS", when (emergency.locationStatus) { LocationStatus.ACTIVE -> "ACTIVE"; LocationStatus.PERMISSION_REQUIRED -> "PERMISSION"; LocationStatus.UNAVAILABLE -> "UNAVAILABLE"; LocationStatus.ERROR -> "ERROR"; else -> "READY" }) ; SignalRow("Risk score", "${emergency.riskScore}/100 • ${emergency.riskSeverity.name}") ; SignalRow("Local AI", "Phase 18 • model slot")
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {},
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .semantics {
                        contentDescription = "Cancel emergency. Press and continuously hold for 5 seconds."
                        role = Role.Button
                    }
                    .pointerInput(Unit) { detectTapGestures(onPress = { holding = true; emergencyVm.beginCancellationHold(); val released = tryAwaitRelease(); holding = false; if (released && progress < 1f) emergencyVm.abortCancellationHold() }) },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF8E1740))
            ) { Text(if (holding) "HOLD CANCEL • ${(progress * 5).toInt() + 1}s" else "HOLD 5 SEC TO CANCEL", fontWeight = FontWeight.ExtraBold) }
            androidx.compose.material3.LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().padding(top = 8.dp).semantics { progressBarRangeInfo = ProgressBarRangeInfo(progress, 0f..1f) }, trackColor = Color(0xFF512431))
        }
    }
}

@Composable private fun SignalRow(label: String, value: String) { Row(Modifier.fillMaxWidth().padding(vertical = 6.dp).semantics { contentDescription = "$label: $value" }, verticalAlignment = Alignment.CenterVertically) { Text("•", color = Color(0xFFFFA7BF), fontSize = 18.sp); Spacer(Modifier.width(8.dp)); Text(label, color = Color.White, fontWeight = FontWeight.SemiBold, modifier = Modifier); Text(value, color = Color(0xFFE9CAD4), fontSize = 11.sp) } }
private fun formatRemaining(seconds: Int): String = "%02d:%02d".format(seconds / 60, seconds % 60)

@Composable
private fun FeatureCard(
    modifier: Modifier,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    phase: String,
    status: String,
    onClick: (() -> Unit)? = null
) {
    Card(
        modifier.then(
            if (onClick != null) Modifier
                .semantics { role = Role.Button }
                .clickable(onClickLabel = "Open $title", onClick = onClick)
            else Modifier
        ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0D2140))
    ) {
        Column(Modifier.padding(15.dp)) {
            Icon(icon, contentDescription = title, tint = Color(0xFF55D7FF), modifier = Modifier.size(24.dp))
            Spacer(Modifier.height(12.dp))
            Text(title, fontWeight = FontWeight.Bold, color = Color.White)
            Text(status, color = Color(0xFFB8C4D9), fontSize = 11.sp)
            Spacer(Modifier.height(6.dp))
            Text(phase, color = Color(0xFFFF72C8), fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun InfoCard(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, body: String) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF0D2140))) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
            Icon(icon, contentDescription = null, tint = Color(0xFF55D7FF), modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(Modifier.height(3.dp))
                Text(body, color = Color(0xFFB8C4D9), fontSize = 12.sp, lineHeight = 17.sp)
            }
        }
    }
}

@Composable
private fun SafetyTabContent(
    contactsVm: TrustedContactsViewModel,
    notificationPermissionLauncher: androidx.activity.result.ActivityResultLauncher<String>,
    latestLocation: com.saferescue.app.core.location.LocationPoint?,
    safetyVm: SafetyGuidanceViewModel
) {
    val state by contactsVm.state.collectAsStateWithLifecycle()
    var editing by remember { mutableStateOf<com.saferescue.app.core.contacts.TrustedContact?>(null) }
    var showEditor by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<com.saferescue.app.core.contacts.TrustedContact?>(null) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Trusted contacts", modifier = Modifier.semantics { heading() }, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF17172A))
        Text("Phase 14 • configure up to 3 trusted contacts for future secure emergency delivery.", color = Color(0xFF6D6D7D), fontSize = 13.sp)

        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.People, contentDescription = null, tint = Color(0xFF5B4BDB), modifier = Modifier.size(24.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier) {
                        Text("${state.contacts.size}/3 configured", fontWeight = FontWeight.Bold, color = Color(0xFF222236))
                        Text("Contact details are encrypted locally with Android Keystore.", color = Color(0xFF777788), fontSize = 12.sp)
                    }
                    if (state.contacts.size < 3) {
                        IconButton(onClick = { editing = null; showEditor = true }) {
                            Icon(Icons.Rounded.PersonAdd, contentDescription = "Add trusted contact", tint = Color(0xFF5B4BDB))
                        }
                    }
                }
                Text("Verification here is an explicit local confirmation flag. It is not remote SMS/OTP identity verification.", color = Color(0xFF777788), fontSize = 11.sp, lineHeight = 15.sp)
            }
        }

        state.error?.let { InfoCard(Icons.Rounded.Security, "Action blocked", it) }
        state.message?.let { InfoCard(Icons.Rounded.CheckCircle, "Updated", it) }

        SafetyGuidanceCard(latestLocation = latestLocation, vm = safetyVm)

        state.contacts.forEach { contact ->
            TrustedContactCard(
                contact = contact,
                onEdit = { editing = contact; showEditor = true },
                onDelete = { pendingDelete = contact },
                onVerify = { contactsVm.toggleVerified(contact) },
                onTest = { contactsVm.testNotification(contact) }
            )
        }

        if (state.contacts.isEmpty()) {
            InfoCard(Icons.Rounded.People, "No trusted contacts yet", "Add up to 3 people you trust. Keep their phone numbers current before using the emergency workflow.")
        }

        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Notifications, contentDescription = null, tint = Color(0xFF5B4BDB), modifier = Modifier.size(23.dp))
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier) {
                        Text("SafeRescue notifications", fontWeight = FontWeight.Bold, color = Color(0xFF222236))
                        Text(if (state.notificationsAllowed) "Permission allowed" else "Permission required", color = Color(0xFF777788), fontSize = 12.sp)
                    }
                }
                if (!state.notificationsAllowed && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    OutlinedButton(onClick = { notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) }, modifier = Modifier.fillMaxWidth()) {
                        Text("Enable notifications")
                    }
                }
                Text("Test alerts are local app notifications. Phase 13's secure backend remains the future external-delivery path; this phase does not pretend an SMS or police message was sent.", color = Color(0xFF777788), fontSize = 11.sp, lineHeight = 15.sp)
            }
        }

        Text("Emergency access", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF17172A), modifier = Modifier.padding(top = 6.dp))
        listOf(
            "Emergency access" to "Active from Phase 5",
            "Safe places & weather" to "Phase 16 — active when a location fix is available",
            "Solo travel" to "Phase 17 — not active",
            "Fake call" to "Phase 18 — not active"
        ).forEach { (name, status) ->
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(19.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Row(Modifier.padding(17.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.HealthAndSafety, contentDescription = null, tint = Color(0xFF5B4BDB), modifier = Modifier.size(23.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier) { Text(name, fontWeight = FontWeight.Bold, color = Color(0xFF222236)); Text(status, color = Color(0xFF777788), fontSize = 12.sp) }
                }
            }
        }
    }

    if (showEditor) {
        TrustedContactEditor(
            existing = editing,
            onDismiss = { showEditor = false },
            onSave = { name, phone -> contactsVm.save(name, phone, editing?.id); showEditor = false }
        )
    }
    pendingDelete?.let { contact ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Remove trusted contact?") },
            text = { Text("Remove ${contact.name} from this device's trusted-contact list?") },
            confirmButton = { TextButton(onClick = { contactsVm.remove(contact.id); pendingDelete = null }) { Text("Remove") } },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Keep") } }
        )
    }
}

@Composable
private fun TrustedContactCard(
    contact: com.saferescue.app.core.contacts.TrustedContact,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onVerify: () -> Unit,
    onTest: () -> Unit
) {
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.AccountCircle, contentDescription = null, tint = Color(0xFF5B4BDB), modifier = Modifier.size(38.dp))
                Spacer(Modifier.width(11.dp))
                Column(Modifier) {
                    Text(contact.name, fontWeight = FontWeight.Bold, color = Color(0xFF222236))
                    Text(maskPhone(contact.phone), color = Color(0xFF777788), fontSize = 12.sp)
                }
                Text(if (contact.verified) "VERIFIED" else "UNVERIFIED", color = if (contact.verified) Color(0xFF2D7A4B) else Color(0xFF8A6570), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onVerify, modifier = Modifier) { Text(if (contact.verified) "Unverify" else "Verify") }
                OutlinedButton(onClick = onTest, modifier = Modifier, enabled = contact.verified) { Text("Test alert") }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onEdit, modifier = Modifier) { Icon(Icons.Rounded.Edit, contentDescription = null, modifier = Modifier.size(17.dp)); Spacer(Modifier.width(4.dp)); Text("Edit") }
                TextButton(onClick = onDelete, modifier = Modifier) { Icon(Icons.Rounded.Delete, contentDescription = null, modifier = Modifier.size(17.dp)); Spacer(Modifier.width(4.dp)); Text("Remove") }
            }
        }
    }
}

@Composable
private fun TrustedContactEditor(
    existing: com.saferescue.app.core.contacts.TrustedContact?,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var name by remember(existing?.id) { mutableStateOf(existing?.name.orEmpty()) }
    var phone by remember(existing?.id) { mutableStateOf(existing?.phone.orEmpty()) }
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Add trusted contact" else "Edit trusted contact") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                OutlinedTextField(phone, { phone = it }, label = { Text("Phone number") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Text("Saving changes resets the local verification flag.", color = Color(0xFF777788), fontSize = 11.sp)
            }
        },
        confirmButton = { TextButton(onClick = { onSave(name, phone) }, enabled = name.isNotBlank() && phone.isNotBlank()) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun maskPhone(phone: String): String {
    val digits = phone.filter(Char::isDigit)
    return if (digits.length <= 4) "••••" else "•••• ${digits.takeLast(4)}"
}

@Composable
private fun EvidenceTabContent(emergency: EmergencyUiViewModel.UiState) {
    var reportMessage by remember { mutableStateOf<String?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Evidence & Reports", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF17172A))
        Text("Phase 15 • local incident report generation", color = Color(0xFF6E6E7C), fontSize = 13.sp)
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Incident report", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                Text("Create a PDF containing the incident summary and chronological timeline. Raw camera and voice recordings are not embedded.", color = Color(0xFF666676), fontSize = 12.sp)
                Button(onClick = {
                    val report = IncidentReport(
                        incidentId = emergency.incidentId ?: "local-report-${System.currentTimeMillis()}",
                        generatedAtMillis = System.currentTimeMillis(),
                        finalRiskScore = emergency.riskScore,
                        finalRiskSeverity = emergency.riskSeverity,
                        timeline = emergency.timeline,
                        evidenceCount = 0,
                        locationAvailable = emergency.latestLocation != null,
                        voiceMonitoringUsed = emergency.latestVoiceMetrics != null
                    )
                    runCatching { PdfIncidentReportGenerator(context).generate(report) }
                        .onSuccess { reportMessage = "PDF saved securely in app storage: ${it.name}" }
                        .onFailure { reportMessage = "Report generation failed safely: ${it.message ?: "unknown error"}" }
                }, modifier = Modifier.fillMaxWidth()) { Text("Generate PDF report") }
                reportMessage?.let { Text(it, color = Color(0xFF5B4BDB), fontSize = 11.sp) }
            }
        }
        if (emergency.timeline.isEmpty()) {
            InfoCard(Icons.Rounded.Description, "No timeline events", "Start an SOS to build a local chronological timeline. Sensitive monitoring entries are cleared when an SOS is cancelled.")
        } else {
            emergency.timeline.asReversed().forEach { event -> TimelineEventCard(event) }
        }
    }
}

@Composable
private fun TimelineEventCard(event: TimelineEvent) {
    val time = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(event.timestampMillis))
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Row(Modifier.padding(15.dp), verticalAlignment = Alignment.Top) {
            Icon(Icons.Rounded.Description, contentDescription = null, tint = Color(0xFF5B4BDB), modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Column(Modifier) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Text(event.title, fontWeight = FontWeight.Bold, color = Color(0xFF202033), modifier = Modifier)
                    Text(time, color = Color(0xFF777788), fontSize = 11.sp)
                }
                Spacer(Modifier.height(4.dp))
                Text(event.detail, color = Color(0xFF6E6E7C), fontSize = 12.sp, lineHeight = 17.sp)
                if (event.riskScore != null) Text("Risk: ${event.riskScore}/100", color = Color(0xFF9A4D83), fontSize = 11.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 5.dp))
            }
        }
    }
}

@Composable
private fun MeTabContent(user: AuthUser, onLogout: () -> Unit) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Me", fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF17172A))
        Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
            Column(Modifier.padding(20.dp)) {
                Icon(Icons.Rounded.AccountCircle, contentDescription = "Profile", tint = Color(0xFF5B4BDB), modifier = Modifier.size(48.dp))
                Spacer(Modifier.height(10.dp))
                Text(user.name.ifBlank { "SafeRescue User" }, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text("@${user.username}", color = Color(0xFF777788))
                Spacer(Modifier.height(10.dp))
                Text("Phone and email remain under the authentication layer. Phase 3 does not expose or edit them here.", color = Color(0xFF666676), fontSize = 12.sp)
            }
        }
        InfoCard(Icons.Rounded.Lock, "Session security", "Authentication sessions are managed by the Phase 2 authentication layer and Android Keystore-backed storage.")
        OutlinedButton(onClick = onLogout, modifier = Modifier.fillMaxWidth().height(50.dp), shape = RoundedCornerShape(15.dp)) {
            Text("Log out", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun PlaceholderTab(title: String, subtitle: String, items: List<Pair<String, String>>) {
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(title, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF17172A))
        Text(subtitle, color = Color(0xFF6D6D7D), fontSize = 13.sp)
        items.forEach { (name, status) ->
            Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(19.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                Row(Modifier.padding(17.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.HealthAndSafety, contentDescription = null, tint = Color(0xFF5B4BDB), modifier = Modifier.size(23.dp))
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier) {
                        Text(name, fontWeight = FontWeight.Bold, color = Color(0xFF222236))
                        Text(status, color = Color(0xFF777788), fontSize = 12.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun BottomNav(selected: HomeTab, onSelect: (HomeTab) -> Unit) {
    NavigationBar(Modifier.navigationBarsPadding(), containerColor = Color(0xFF07152C)) {
        NavItem(HomeTab.HOME, Icons.Rounded.Home, selected, onSelect)
        NavItem(HomeTab.SAFETY, Icons.Rounded.Security, selected, onSelect)
        NavItem(HomeTab.EVIDENCE, Icons.Rounded.Description, selected, onSelect)
        NavItem(HomeTab.ME, Icons.Rounded.AccountCircle, selected, onSelect)
    }
}

@Composable
private fun NavItem(
    tab: HomeTab,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    selected: HomeTab,
    onSelect: (HomeTab) -> Unit
) {
    Column(
        modifier = Modifier
            
            .clickable { onSelect(tab) }
            .padding(vertical = 8.dp)
            .semantics { contentDescription = tab.label },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null
        )
        Text(
            text = tab.label,
            fontSize = 11.sp
        )
    }
}

@Composable
private fun SafetyGuidanceCard(
    latestLocation: com.saferescue.app.core.location.LocationPoint?,
    vm: SafetyGuidanceViewModel
) {
    val state by vm.state.collectAsStateWithLifecycle()
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(20.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.HealthAndSafety, contentDescription = null, tint = Color(0xFF5B4BDB), modifier = Modifier.size(24.dp))
                Spacer(Modifier.width(10.dp))
                Column(Modifier) {
                    Text("Nearby safety guidance", fontWeight = FontWeight.Bold, color = Color(0xFF222236))
                    Text("Phase 16 • live mapped places + weather", color = Color(0xFF777788), fontSize = 12.sp)
                }
                IconButton(onClick = { vm.refresh(latestLocation) }, enabled = latestLocation != null && !state.loading) {
                    Icon(Icons.Rounded.Refresh, contentDescription = "Refresh safety guidance", tint = Color(0xFF5B4BDB))
                }
            }
            Text(
                if (latestLocation == null) "No current location fix. This feature does not request location on its own; start SOS and allow location access first."
                else "Uses your latest location fix. Refreshing sends coordinates over HTTPS to OpenStreetMap/Open-Meteo. Places are guidance only and are not safety-certified or emergency dispatch endpoints.",
                color = Color(0xFF777788), fontSize = 11.sp, lineHeight = 15.sp
            )
            if (latestLocation != null && state.places.isEmpty() && state.weather == null && !state.loading) {
                OutlinedButton(onClick = { vm.refresh(latestLocation) }, modifier = Modifier.fillMaxWidth()) {
                    Text("Find nearby places & weather")
                }
            }
            if (state.loading) {
                androidx.compose.material3.LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text("Loading live safety data…", color = Color(0xFF6E6E7C), fontSize = 12.sp)
            }
            state.error?.let { error ->
                InfoCard(Icons.Rounded.Security, "Live data unavailable", error)
            }
            state.weather?.let { weather ->
                WeatherMiniCard(weather)
            }
            state.places.take(6).forEach { place ->
                SafePlaceRow(place)
            }
            if (!state.loading && state.weather != null && state.places.isEmpty()) {
                Text("No mapped safety places were returned within 3 km. This does not mean no safe place exists.", color = Color(0xFF777788), fontSize = 11.sp)
            }
        }
    }
}

@Composable
private fun WeatherMiniCard(weather: com.saferescue.app.core.safety.WeatherSnapshot) {
    val condition = weatherDescription(weather.weatherCode)
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFFF6F4FF))) {
        Row(Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Cloud, contentDescription = "Current weather", tint = Color(0xFF5B4BDB), modifier = Modifier.size(27.dp))
            Spacer(Modifier.width(11.dp))
            Column(Modifier) {
                Text("Current weather • $condition", fontWeight = FontWeight.Bold, color = Color(0xFF29263D))
                Text("${formatNumber(weather.temperatureC)}°C • feels ${formatNumber(weather.apparentTemperatureC)}°C • wind ${formatNumber(weather.windSpeedKmh)} km/h", color = Color(0xFF666676), fontSize = 11.sp)
                Text("Precipitation ${formatNumber(weather.precipitationMm)} mm • source: ${weather.source}", color = Color(0xFF777788), fontSize = 10.sp)
            }
        }
    }
}

@Composable
private fun SafePlaceRow(place: com.saferescue.app.core.safety.SafePlace) {
    val icon = when (place.category) {
        com.saferescue.app.core.safety.SafePlaceCategory.POLICE -> Icons.Rounded.LocalPolice
        com.saferescue.app.core.safety.SafePlaceCategory.HOSPITAL -> Icons.Rounded.LocalHospital
        com.saferescue.app.core.safety.SafePlaceCategory.FIRE_STATION -> Icons.Rounded.LocalFireDepartment
        com.saferescue.app.core.safety.SafePlaceCategory.PUBLIC_PLACE -> Icons.Rounded.Place
    }
    Card(Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Row(Modifier.padding(13.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, contentDescription = place.category.label, tint = Color(0xFF5B4BDB), modifier = Modifier.size(25.dp))
            Spacer(Modifier.width(10.dp))
            Column(Modifier) {
                Text(place.name, fontWeight = FontWeight.Bold, color = Color(0xFF222236))
                Text("${place.category.label} • ${formatDistance(place.distanceMeters)} away", color = Color(0xFF666676), fontSize = 11.sp)
                Text("Mapped data • ${place.source}", color = Color(0xFF888895), fontSize = 10.sp)
            }
        }
    }
}

private fun formatDistance(meters: Double): String = if (meters < 1000) "${meters.toInt()} m" else "${"%.1f".format(java.util.Locale.US, meters / 1000.0)} km"
private fun formatNumber(value: Double): String = if (value.isFinite()) "%.1f".format(java.util.Locale.US, value) else "—"
private fun weatherDescription(code: Int): String = when (code) {
    0 -> "Clear"
    1, 2, 3 -> "Cloudy"
    45, 48 -> "Fog"
    51, 53, 55, 56, 57 -> "Drizzle"
    61, 63, 65, 66, 67 -> "Rain"
    71, 73, 75, 77 -> "Snow"
    80, 81, 82 -> "Rain showers"
    85, 86 -> "Snow showers"
    95 -> "Thunderstorm"
    96, 99 -> "Thunderstorm with hail"
    else -> "Weather code $code"
}
