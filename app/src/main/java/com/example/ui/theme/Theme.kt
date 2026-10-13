package com.example.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val CozyDarkColorScheme = darkColorScheme(
    primary = ToastedCaramel,
    onPrimary = EspressoBlack,
    primaryContainer = WarmCocoaContainer,
    onPrimaryContainer = WarmParchment,
    secondary = SoftCinnamon,
    onSecondary = EspressoBlack,
    secondaryContainer = CozyMochaSurface,
    onSecondaryContainer = WarmParchment,
    tertiary = AmberCrema,
    onTertiary = EspressoBlack,
    background = EspressoBlack,
    onBackground = WarmParchment,
    surface = RoastedBark,
    onSurface = WarmParchment,
    surfaceVariant = CozyMochaSurface,
    onSurfaceVariant = MutedLatte,
    surfaceContainerLow = RoastedBark,
    surfaceContainer = CozyMochaSurface,
    surfaceContainerHigh = WarmCocoaContainer,
    outline = DarkWalnutOutline,
    outlineVariant = DarkWalnutOutline.copy(alpha = 0.6f),
    error = Color(0xFFFFB4AB)
)

private val CozyLightColorScheme = lightColorScheme(
    primary = RichEspressoPrimary,
    onPrimary = AlabasterWhite,
    primaryContainer = WarmOatContainer,
    onPrimaryContainer = DeepRoastText,
    secondary = ChestnutSecondary,
    onSecondary = AlabasterWhite,
    secondaryContainer = CozyCreamSurface,
    onSecondaryContainer = DeepRoastText,
    tertiary = SpicedChaiTertiary,
    onTertiary = AlabasterWhite,
    background = AlabasterWhite,
    onBackground = DeepRoastText,
    surface = CozyCreamSurface,
    onSurface = DeepRoastText,
    surfaceVariant = WarmOatContainer,
    onSurfaceVariant = WarmTaupeSubtext,
    surfaceContainerLow = AlabasterWhite,
    surfaceContainer = CozyCreamSurface,
    surfaceContainerHigh = WarmOatContainer,
    outline = CreamOutline,
    outlineVariant = CreamOutline.copy(alpha = 0.6f),
    error = Color(0xFFBA1A1A)
)

@Composable
fun AuraVoiceTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) CozyDarkColorScheme else CozyLightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = colorScheme.surface.toArgb()
            window.navigationBarColor = colorScheme.surface.toArgb()
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    AuraVoiceTheme(darkTheme = darkTheme, content = content)
}