package com.airwave.wallet.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Holds the Airwave dark color tokens used by Material3.
 * Screens and the unlock gradient live in their own files; this object does not draw them.
 */
object AirwaveColors {
    val Bg = Color(0xFF0F1419)
    val BgElevated = Color(0xFF161C24)
    val Fill = Color(0xFF1E2630)
    val Stroke = Color(0xFF2A3542)
    val StrokeSoft = Color(0xFF222A34)
    val Text = Color(0xFFE8EEF4)
    val TextMuted = Color(0xFF9AA7B4)
    val TextDim = Color(0xFF6B7785)
    val Accent = Color(0xFF3D9CF0)
    val AccentDim = Color(0xFF2A6EA8)
    val Danger = Color(0xFFC75050)
    val Ok = Color(0xFF4ECB8D)
    val Warn = Color(0xFFE8B84A)
}

private val scheme = darkColorScheme(
    primary = AirwaveColors.Accent,
    onPrimary = Color(0xFF061018),
    primaryContainer = AirwaveColors.AccentDim,
    onPrimaryContainer = AirwaveColors.Text,
    secondary = AirwaveColors.AccentDim,
    background = AirwaveColors.Bg,
    onBackground = AirwaveColors.Text,
    surface = AirwaveColors.BgElevated,
    onSurface = AirwaveColors.Text,
    surfaceVariant = AirwaveColors.Fill,
    onSurfaceVariant = AirwaveColors.TextMuted,
    outline = AirwaveColors.Stroke,
    error = AirwaveColors.Danger,
)

@Composable
fun AirwaveTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = scheme,
        content = content,
    )
}
