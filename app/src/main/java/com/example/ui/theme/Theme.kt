package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = EmeraldPrimary,
    onPrimary = SurfacePureWhite,
    primaryContainer = EmeraldSoftContainer,
    onPrimaryContainer = EmeraldOnContainer,
    secondary = EmeraldVibrant,
    onSecondary = SurfacePureWhite,
    secondaryContainer = SurfaceContainerHigh,
    onSecondaryContainer = EmeraldDark,
    tertiary = GoldAccent,
    onTertiary = SurfacePureWhite,
    tertiaryContainer = GoldSoftContainer,
    onTertiaryContainer = GoldOnContainer,
    background = SurfaceMintCanvas,
    onBackground = InkDark,
    surface = SurfacePureWhite,
    onSurface = InkDark,
    surfaceVariant = SurfaceContainerLow,
    onSurfaceVariant = InkMuted,
    outline = OutlineSubtle,
    outlineVariant = Color(0xFFDCE8E2),
    error = ErrorCrimson,
    onError = SurfacePureWhite,
    errorContainer = ErrorContainer,
    onErrorContainer = Color(0xFF6B1115)
)

private val DarkColorScheme = darkColorScheme(
    primary = DarkEmeraldPrimary,
    onPrimary = DarkEmeraldOnPrimary,
    primaryContainer = DarkEmeraldContainer,
    onPrimaryContainer = EmeraldSoftContainer,
    secondary = Color(0xFF91C9B0),
    onSecondary = Color(0xFF0A3324),
    secondaryContainer = DarkSurfaceHigh,
    onSecondaryContainer = DarkTextPrimary,
    tertiary = GoldAccent,
    onTertiary = Color(0xFF2E2104),
    tertiaryContainer = Color(0xFF4A360D),
    onTertiaryContainer = GoldSoftContainer,
    background = DarkSurfaceCanvas,
    onBackground = DarkTextPrimary,
    surface = DarkSurfaceCard,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceHigh,
    onSurfaceVariant = DarkTextSecondary,
    outline = Color(0xFF345246),
    error = Color(0xFFFF8989),
    onError = Color(0xFF5D0A0E)
)

val AcademyShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun MesahaAcademyTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    // Force authentic Arabic RTL direction across the entire app regardless of device locale
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            shapes = AcademyShapes,
            content = content
        )
    }
}
