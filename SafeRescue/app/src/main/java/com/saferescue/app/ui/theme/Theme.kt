package com.saferescue.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes

private val SafeRescueColors = lightColorScheme(
    primary = Color(0xFF5B4BDB),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE9E4FF),
    onPrimaryContainer = Color(0xFF21145F),
    secondary = Color(0xFFB51F4F),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFD9E4),
    onSecondaryContainer = Color(0xFF3F0016),
    background = Color(0xFFF7F6FB),
    onBackground = Color(0xFF17172A),
    surface = Color.White,
    onSurface = Color(0xFF17172A),
    surfaceVariant = Color(0xFFEDEBF3),
    onSurfaceVariant = Color(0xFF5F5D6B),
    outline = Color(0xFF777482),
    error = Color(0xFFB3261E),
    onError = Color.White
)

private val SafeRescueTypography = Typography().run {
    copy(
        headlineLarge = headlineLarge.copy(fontWeight = FontWeight.ExtraBold),
        headlineMedium = headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
        titleLarge = titleLarge.copy(fontWeight = FontWeight.Bold),
        titleMedium = titleMedium.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = labelLarge.copy(fontWeight = FontWeight.Bold)
    )
}

private val SafeRescueShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(30.dp)
)

@Composable
fun SafeRescueTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = SafeRescueColors,
        typography = SafeRescueTypography,
        shapes = SafeRescueShapes,
        content = content
    )
}
