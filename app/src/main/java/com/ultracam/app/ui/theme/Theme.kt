package com.ultracam.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// ---------------------------------------------------------------- palette

val Void = Color(0xFF0F0E13)
val VoidSoft = Color(0xFF1D1B20)
val PixelYellow = Color(0xFFFABB05)
val PixelPillBg = Color(0xCC1F1F23)
val PixelPurpleAccent = Color(0xFFE8DEF8)
val PixelPurpleDark = Color(0xFF1D1B20)
val GlassWhite = Color(0xFFFFFFFF)
val PrismCyan = Color(0xFF66E5FF)
val PrismMagenta = Color(0xFFFF5FD2)
val PrismViolet = Color(0xFF8E7BFF)
val PrismAmber = Color(0xFFFFD166)
val PrismGreen = Color(0xFF81C784)
val PrismRed = Color(0xFFFF6B6B)

// ---------------------------------------------------------------- type

val ReadoutTextStyle = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontWeight = FontWeight.Medium,
    fontSize = 11.sp,
    letterSpacing = 0.6.sp,
    color = GlassWhite
)

val TinyLabelStyle = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontWeight = FontWeight.Medium,
    fontSize = 9.sp,
    letterSpacing = 1.2.sp,
    color = GlassWhite.copy(alpha = 0.7f)
)

val ChipTextStyle = TextStyle(
    fontFamily = FontFamily.Monospace,
    fontWeight = FontWeight.SemiBold,
    fontSize = 10.sp,
    letterSpacing = 0.8.sp,
    color = GlassWhite
)

private val UltraTypography = Typography(
    labelSmall = TinyLabelStyle,
    labelMedium = ChipTextStyle,
    labelLarge = ReadoutTextStyle
)

// ---------------------------------------------------------------- theme

private val UltraColors = darkColorScheme(
    primary = PrismCyan,
    onPrimary = Void,
    secondary = PrismViolet,
    onSecondary = Void,
    tertiary = PrismMagenta,
    background = Void,
    onBackground = GlassWhite,
    surface = VoidSoft,
    onSurface = GlassWhite,
    surfaceVariant = VoidSoft,
    onSurfaceVariant = GlassWhite.copy(alpha = 0.72f),
    outline = GlassWhite.copy(alpha = 0.28f)
)

@Composable
fun UltraCamTheme(content: @Composable () -> Unit) {
    // A camera instrument is always dark; the system theme is intentionally
    // ignored so the viewfinder never fights the glass UI.
    isSystemInDarkTheme() // referenced to keep the call site intentional
    MaterialTheme(
        colorScheme = UltraColors,
        typography = UltraTypography,
        content = content
    )
}
