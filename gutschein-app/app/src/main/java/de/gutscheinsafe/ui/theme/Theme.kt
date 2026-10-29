package de.gutscheinsafe.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Light = lightColorScheme(
    primary = Color(0xFF0F766E),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB4EFE6),
    onPrimaryContainer = Color(0xFF00201D),
    secondary = Color(0xFFB45309),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFE3C2),
    onSecondaryContainer = Color(0xFF2E1500),
    tertiary = Color(0xFF7C3AED),
    tertiaryContainer = Color(0xFFEDE4FF),
    onTertiaryContainer = Color(0xFF22005D),
    background = Color(0xFFF6FAF9),
    surface = Color(0xFFF6FAF9),
    surfaceVariant = Color(0xFFDCE5E3),
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFF0F5F4),
    surfaceContainer = Color(0xFFEAF0EF),
    surfaceContainerHigh = Color(0xFFE4EAE9),
    error = Color(0xFFBA1A1A),
    errorContainer = Color(0xFFFFDAD6),
)

private val Dark = darkColorScheme(
    primary = Color(0xFF7DD8CB),
    onPrimary = Color(0xFF003732),
    primaryContainer = Color(0xFF005049),
    onPrimaryContainer = Color(0xFFB4EFE6),
    secondary = Color(0xFFFFB873),
    onSecondary = Color(0xFF4A2800),
    secondaryContainer = Color(0xFF6A3B00),
    onSecondaryContainer = Color(0xFFFFE3C2),
    tertiary = Color(0xFFD0BCFF),
    tertiaryContainer = Color(0xFF4F2A9E),
    onTertiaryContainer = Color(0xFFEDE4FF),
    background = Color(0xFF0E1514),
    surface = Color(0xFF0E1514),
    surfaceVariant = Color(0xFF3F4947),
    surfaceContainerLowest = Color(0xFF090F0E),
    surfaceContainerLow = Color(0xFF161D1C),
    surfaceContainer = Color(0xFF1A2120),
    surfaceContainerHigh = Color(0xFF252B2A),
    error = Color(0xFFFFB4AB),
    errorContainer = Color(0xFF93000A),
)

/** Colors for expiry badges, readable in light and dark mode. */
object StatusColors {
    val urgent = Color(0xFFDC2626)
    val soon = Color(0xFFEA580C)
    val upcoming = Color(0xFFCA8A04)
    val ok = Color(0xFF16A34A)
    val neutral = Color(0xFF64748B)
}

@Composable
fun GutscheinTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) Dark else Light,
        content = content,
    )
}
