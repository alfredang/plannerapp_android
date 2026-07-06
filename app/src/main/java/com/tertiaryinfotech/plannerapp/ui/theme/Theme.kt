package com.tertiaryinfotech.plannerapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Brand tokens mirrored from the iOS asset catalog (AccentColor / Background / Card).
val BrandIndigo = Color(0xFF565AE6)
val BrandIndigoDark = Color(0xFF7A7EF5)
val BgLight = Color(0xFFF1F2F5)
val BgDark = Color(0xFF16181C)
val CardLight = Color(0xFFFFFFFF)
val CardDark = Color(0xFF26282E)

private val LightColors = lightColorScheme(
    primary = BrandIndigo,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE2E3FC),
    onPrimaryContainer = Color(0xFF14166B),
    secondary = Color(0xFF5C5D72),
    background = BgLight,
    onBackground = Color(0xFF1B1B1F),
    surface = CardLight,
    onSurface = Color(0xFF1B1B1F),
    surfaceVariant = Color(0xFFE3E1EC),
    onSurfaceVariant = Color(0xFF46464F),
    surfaceContainer = Color(0xFFF6F6FA),
    surfaceContainerLow = Color(0xFFFAFAFE),
)

private val DarkColors = darkColorScheme(
    primary = BrandIndigoDark,
    onPrimary = Color(0xFF14166B),
    primaryContainer = Color(0xFF3A3EC0),
    onPrimaryContainer = Color(0xFFE2E3FC),
    secondary = Color(0xFFC5C4DD),
    background = BgDark,
    onBackground = Color(0xFFE4E1E6),
    surface = CardDark,
    onSurface = Color(0xFFE4E1E6),
    surfaceVariant = Color(0xFF46464F),
    onSurfaceVariant = Color(0xFFC7C5D0),
    surfaceContainer = Color(0xFF1F2126),
    surfaceContainerLow = Color(0xFF1B1D21),
)

@Composable
fun PlannerAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content,
    )
}
