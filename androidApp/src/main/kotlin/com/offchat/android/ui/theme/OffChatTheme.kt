package com.offchat.android.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// HOPE NET Design Tokens
val HopeNetBackground = Color(0xFF0D1818)
val HopeNetSurface = Color(0xFF162525)
val HopeNetCyan = Color(0xFF00E5FF)
val HopeNetOrange = Color(0xFFFF9800)
val HopeNetGreen = Color(0xFF00E676)
val HopeNetRed = Color(0xFFF44336)
val HopeNetTextGray = Color(0xFF7A8B8B)
val HopeNetCardColor = Color(0xFF162525)

private val HopeNetColors = darkColorScheme(
    primary = HopeNetCyan,
    secondary = HopeNetOrange,
    background = HopeNetBackground,
    surface = HopeNetSurface,
    error = HopeNetRed,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
    onBackground = Color.White,
    onSurface = Color.White,
    onError = Color.White,
    surfaceVariant = HopeNetCardColor,
    onSurfaceVariant = HopeNetTextGray
)

@Composable
fun OffChatTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = HopeNetColors,
        content = content
    )
}
