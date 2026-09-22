package com.saferescue.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.saferescue.app.auth.AuthScreenState
import com.saferescue.app.feature.home.DashboardRoot
import com.saferescue.app.auth.AuthViewModel
import com.saferescue.app.auth.RegistrationInput
import com.saferescue.app.ui.theme.SafeRescueTheme
import com.saferescue.app.core.notification.SafeRescueNotificationManager

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // If notification access is already granted, keep the emergency-access
        // notification available. Android/device settings still control whether
        // notifications appear on the secure lock screen.
        SafeRescueNotificationManager(this).showLockScreenEmergencyAccess()
        setContent { SafeRescueApp() }
    }
}

@Composable
private fun SafeRescueApp(vm: AuthViewModel = viewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    SafeRescueTheme {
        Surface(modifier = Modifier.fillMaxSize()) {
            AnimatedContent(
                targetState = state.screen,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "auth-screen"
            ) { screen ->
                when (screen) {
                    AuthScreenState.Login -> LoginScreen(state.loading, state.error, state.message, vm)
                    is AuthScreenState.RegisterOtp -> RegisterOtpScreen(screen, state.loading, state.error, vm)
                    is AuthScreenState.ResetOtp -> ResetOtpScreen(screen, state.loading, state.error, vm)
                    is AuthScreenState.SignedIn -> HomeScreen(screen.user, state.message, vm)
                }
            }
        }
    }
}

@Composable
private fun LoginScreen(loading: Boolean, error: String?, message: String?, vm: AuthViewModel) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var mode by remember { mutableStateOf("login") }
    var resetUsername by remember { mutableStateOf("") }

    AuthBackground {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xE80A1735)),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text("Welcome back", modifier = Modifier.semantics { heading() }, fontSize = 25.sp, fontWeight = FontWeight.Bold, color = Color.White)
                Text("Secure access to your safety dashboard", color = Color(0xFFD6DCEF))
                Spacer(Modifier.height(20.dp))

                if (mode == "login") {
                    OutlinedTextField(username, { username = it }, label = { Text("Username") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(12.dp))
                    OutlinedTextField(password, { password = it }, label = { Text("Password") }, visualTransformation = PasswordVisualTransformation(), singleLine = true, modifier = Modifier.fillMaxWidth())
                    AuthMessage(error, message)
                    Spacer(Modifier.height(18.dp))
                    Button(
                        onClick = { vm.login(username, password) },
                        enabled = !loading,
                        modifier = Modifier.fillMaxWidth().height(54.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF5B4BDB))
                    ) { if (loading) CircularProgressIndicator(Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp) else Text("Sign in", fontWeight = FontWeight.Bold) }
                    TextButton(onClick = { mode = "reset" }, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Forgot password?") }
                    TextButton(onClick = { mode = "register" }, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Create a SafeRescue account") }
                    Spacer(Modifier.height(4.dp))
                    DebugAccountNotice()
                } else if (mode == "reset") {
                    Text("Password reset", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(10.dp))
                    OutlinedTextField(resetUsername, { resetUsername = it }, label = { Text("Username") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    AuthMessage(error, message)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { vm.startPasswordReset(resetUsername) }, enabled = !loading, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Send reset OTP") }
                    TextButton(onClick = { mode = "login" }, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Back to sign in") }
                } else {
                    RegisterForm(loading, error, vm) { mode = "login" }
                }
            }
        }
    }
}

@Composable
private fun RegisterForm(loading: Boolean, error: String?, vm: AuthViewModel, onBack: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }

    Text("Create your account", modifier = Modifier.semantics { heading() }, fontSize = 21.sp, fontWeight = FontWeight.Bold)
    Text("Phone OTP verification is required before activation.", color = Color(0xFF5B6074), fontSize = 13.sp)
    Spacer(Modifier.height(16.dp))
    RegistrationField(name, { name = it }, "Full name")
    RegistrationField(username, { username = it }, "Username")
    RegistrationField(phone, { phone = it }, "Phone number")
    RegistrationField(email, { email = it }, "Email")
    RegistrationField(password, { password = it }, "Password", password = true)
    RegistrationField(confirm, { confirm = it }, "Confirm password", password = true)
    if (password.isNotEmpty() && password != confirm) Text("Passwords do not match.", color = MaterialTheme.colorScheme.error, fontSize = 12.sp)
    AuthMessage(error, null)
    Spacer(Modifier.height(12.dp))
    Button(
        onClick = {
            if (password != confirm) return@Button
            vm.startRegistration(RegistrationInput(name, username, phone, email, password.toCharArray()))
        },
        enabled = !loading && password == confirm,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(16.dp)
    ) { Text("Request OTP") }
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) { TextButton(onClick = onBack) { Text("Back to sign in") } }
}

@Composable
private fun RegistrationField(value: String, onValueChange: (String) -> Unit, label: String, password: Boolean = false) {
    OutlinedTextField(value, onValueChange, label = { Text(label) }, singleLine = true, modifier = Modifier.fillMaxWidth(), visualTransformation = if (password) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None)
    Spacer(Modifier.height(9.dp))
}

@Composable
private fun RegisterOtpScreen(screen: AuthScreenState.RegisterOtp, loading: Boolean, error: String?, vm: AuthViewModel) {
    OtpCard(
        title = "Verify phone number",
        subtitle = "Enter the 6-digit OTP. In this debug build, the generated test OTP is shown only for local testing.",
        debugOtp = screen.debugOtp,
        error = error,
        loading = loading,
        onVerify = { code -> vm.verifyRegistrationOtp(screen.challengeId, code) },
        onBack = vm::backToLogin
    )
}

@Composable
private fun ResetOtpScreen(screen: AuthScreenState.ResetOtp, loading: Boolean, error: String?, vm: AuthViewModel) {
    var newPassword by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF6F5FB)).padding(24.dp), verticalArrangement = Arrangement.Center) {
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(26.dp)) {
            Column(Modifier.padding(24.dp)) {
                Text("Reset password", modifier = Modifier.semantics { heading() }, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text("Verify the OTP and choose a new password.", color = Color(0xFF5B6074))
                Spacer(Modifier.height(16.dp))
                var code by remember { mutableStateOf("") }
                OutlinedTextField(code, { code = it.take(6) }, label = { Text("OTP") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(newPassword, { newPassword = it }, label = { Text("New password") }, visualTransformation = PasswordVisualTransformation(), singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(confirm, { confirm = it }, label = { Text("Confirm new password") }, visualTransformation = PasswordVisualTransformation(), singleLine = true, modifier = Modifier.fillMaxWidth())
                if (screen.debugOtp != null) DebugOtpChip(screen.debugOtp)
                AuthMessage(error, null)
                Spacer(Modifier.height(14.dp))
                Button(onClick = { if (newPassword == confirm) vm.completePasswordReset(screen.challengeId, code, newPassword) }, enabled = !loading && newPassword == confirm, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Update password") }
                TextButton(onClick = vm::backToLogin, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Back") }
            }
        }
    }
}

@Composable
private fun OtpCard(title: String, subtitle: String, debugOtp: String?, error: String?, loading: Boolean, onVerify: (String) -> Unit, onBack: () -> Unit) {
    var code by remember { mutableStateOf("") }
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF6F5FB)).padding(24.dp), verticalArrangement = Arrangement.Center) {
        Card(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(26.dp)) {
            Column(Modifier.padding(24.dp)) {
                Text(title, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                Text(subtitle, color = Color(0xFF5B6074), fontSize = 13.sp)
                Spacer(Modifier.height(18.dp))
                OutlinedTextField(code, { code = it.filter(Char::isDigit).take(6) }, label = { Text("6-digit OTP") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                if (debugOtp != null) DebugOtpChip(debugOtp)
                AuthMessage(error, null)
                Spacer(Modifier.height(14.dp))
                Button(onClick = { onVerify(code) }, enabled = !loading && code.length == 6, modifier = Modifier.fillMaxWidth().height(52.dp)) { Text("Verify OTP") }
                TextButton(onClick = onBack, modifier = Modifier.align(Alignment.CenterHorizontally)) { Text("Cancel") }
            }
        }
    }
}

@Composable
private fun DebugOtpChip(code: String) {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF1F7)), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
        Text("DEBUG ONLY • Test OTP: $code", modifier = Modifier.padding(12.dp), color = Color(0xFF9B245E), fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@Composable
private fun AuthMessage(error: String?, message: String?) {
    if (error != null) {
        Spacer(Modifier.height(8.dp))
        Text(error, color = MaterialTheme.colorScheme.error, fontSize = 13.sp)
    }
    if (message != null) {
        Spacer(Modifier.height(8.dp))
        Text(message, color = Color(0xFF2E6B4F), fontSize = 13.sp)
    }
}

@Composable
private fun DebugAccountNotice() {
    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFF1EFFF)), shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.Lock, null, tint = Color(0xFF5B4BDB), modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("DEBUG TEST ACCOUNT\nUsername: admin  •  Password: admin", fontSize = 12.sp, color = Color(0xFF4B436F))
        }
    }
}

@Composable
private fun HomeScreen(user: com.saferescue.app.auth.AuthUser, message: String?, vm: AuthViewModel) {
    DashboardRoot(user = user, message = message, onLogout = vm::logout)
}

@Composable
private fun AuthBackground(content: @Composable () -> Unit) {
    Box(Modifier.fillMaxSize()) {
        Image(painterResource(R.drawable.saferescue_background), null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xB9101633), Color(0xE8151735), Color(0xF50B0D1B)))))
        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 30.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Image(
                painter = painterResource(R.drawable.saferescue_logo),
                contentDescription = "SafeRescue — Your Safety Our Priority",
                modifier = Modifier.width(285.dp).height(250.dp),
                contentScale = ContentScale.Fit
            )
            Spacer(Modifier.height(4.dp))
            content()
            Spacer(Modifier.height(14.dp))
            Text("Phase 2 • Secure authentication foundation", color = Color(0xFFD9D3EF), fontSize = 12.sp)
        }
    }
}

