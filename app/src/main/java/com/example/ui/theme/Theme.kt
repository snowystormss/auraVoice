package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Color
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween

@Composable
fun MyApplicationTheme(
    themePalette: String = "AURA_INDIGO",
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val colorScheme = when (themePalette.uppercase()) {
        "SOLAR_FLARE" -> darkColorScheme(
            primary = Color(0xFFFFB300),
            secondary = Color(0xFFFFD54F),
            tertiary = Color(0xFFD84315),
            background = Color(0xFF1B1A17),
            surface = Color(0xFF1B1A17),
            surfaceVariant = Color(0xFF2A2824),
            onPrimary = Color.Black,
            onSecondary = Color.Black,
            onTertiary = Color.White,
            onBackground = Color(0xFFF4EBE1),
            onSurface = Color(0xFFF4EBE1),
            onSurfaceVariant = Color(0xFFE5D5C5),
            outline = Color(0xFF4E463D),
            primaryContainer = Color(0xFFD84315),
            secondaryContainer = Color(0xFF2A2824)
        )
        "COSMIC_MINT" -> darkColorScheme(
            primary = Color(0xFF00BFA5),
            secondary = Color(0xFF64FFDA),
            tertiary = Color(0xFF006064),
            background = Color(0xFF111D1C),
            surface = Color(0xFF111D1C),
            surfaceVariant = Color(0xFF1C2D2B),
            onPrimary = Color.Black,
            onSecondary = Color.Black,
            onTertiary = Color.White,
            onBackground = Color(0xFFE0F2F1),
            onSurface = Color(0xFFE0F2F1),
            onSurfaceVariant = Color(0xFFB2DFDB),
            outline = Color(0xFF334D4A),
            primaryContainer = Color(0xFF006064),
            secondaryContainer = Color(0xFF1C2D2B)
        )
        "FOREST_OBSIDIAN" -> darkColorScheme(
            primary = Color(0xFF81C784),
            secondary = Color(0xFFA5D6A7),
            tertiary = Color(0xFF2E7D32),
            background = Color(0xFF1B201C),
            surface = Color(0xFF1B201C),
            surfaceVariant = Color(0xFF252C27),
            onPrimary = Color.Black,
            onSecondary = Color.Black,
            onTertiary = Color.White,
            onBackground = Color(0xFFECEFF1),
            onSurface = Color(0xFFECEFF1),
            onSurfaceVariant = Color(0xFFCFD8DC),
            outline = Color(0xFF3B463F),
            primaryContainer = Color(0xFF2E7D32),
            secondaryContainer = Color(0xFF252C27)
        )
        "MONOCHROME_STEALTH" -> darkColorScheme(
            primary = Color(0xFFFFFFFF),
            secondary = Color(0xFFCFD8DC),
            tertiary = Color(0xFF37474F),
            background = Color(0xFF101213),
            surface = Color(0xFF101213),
            surfaceVariant = Color(0xFF1E2124),
            onPrimary = Color.Black,
            onSecondary = Color.Black,
            onTertiary = Color.White,
            onBackground = Color(0xFFF5F7F8),
            onSurface = Color(0xFFF5F7F8),
            onSurfaceVariant = Color(0xFFB0BEC5),
            outline = Color(0xFF37474F),
            primaryContainer = Color(0xFF37474F),
            secondaryContainer = Color(0xFF1E2124)
        )
        else -> darkColorScheme( // AURA_INDIGO (Default Immersive Dark Theme)
            primary = Color(0xFF8E75FF),
            secondary = Color(0xFFD0BCFF),
            tertiary = Color(0xFF4F378B),
            background = Color(0xFF1C1B1F),
            surface = Color(0xFF1C1B1F),
            surfaceVariant = Color(0xFF2B2930),
            onPrimary = Color.Black,
            onSecondary = Color.Black,
            onTertiary = Color.White,
            onBackground = Color(0xFFE6E1E5),
            onSurface = Color(0xFFE6E1E5),
            onSurfaceVariant = Color(0xFFCCC2DC),
            outline = Color(0xFF49454F),
            primaryContainer = Color(0xFF4F378B),
            secondaryContainer = Color(0xFF2B2930)
        )
    }

    val animationSpec = tween<Color>(durationMillis = 600)
    
    val animatedPrimary = animateColorAsState(targetValue = colorScheme.primary, animationSpec = animationSpec, label = "primary").value
    val animatedSecondary = animateColorAsState(targetValue = colorScheme.secondary, animationSpec = animationSpec, label = "secondary").value
    val animatedTertiary = animateColorAsState(targetValue = colorScheme.tertiary, animationSpec = animationSpec, label = "tertiary").value
    val animatedBackground = animateColorAsState(targetValue = colorScheme.background, animationSpec = animationSpec, label = "background").value
    val animatedSurface = animateColorAsState(targetValue = colorScheme.surface, animationSpec = animationSpec, label = "surface").value
    val animatedSurfaceVariant = animateColorAsState(targetValue = colorScheme.surfaceVariant, animationSpec = animationSpec, label = "surfaceVariant").value
    val animatedOnPrimary = animateColorAsState(targetValue = colorScheme.onPrimary, animationSpec = animationSpec, label = "onPrimary").value
    val animatedOnSecondary = animateColorAsState(targetValue = colorScheme.onSecondary, animationSpec = animationSpec, label = "onSecondary").value
    val animatedOnTertiary = animateColorAsState(targetValue = colorScheme.onTertiary, animationSpec = animationSpec, label = "onTertiary").value
    val animatedOnBackground = animateColorAsState(targetValue = colorScheme.onBackground, animationSpec = animationSpec, label = "onBackground").value
    val animatedOnSurface = animateColorAsState(targetValue = colorScheme.onSurface, animationSpec = animationSpec, label = "onSurface").value
    val animatedOnSurfaceVariant = animateColorAsState(targetValue = colorScheme.onSurfaceVariant, animationSpec = animationSpec, label = "onSurfaceVariant").value
    val animatedOutline = animateColorAsState(targetValue = colorScheme.outline, animationSpec = animationSpec, label = "outline").value
    val animatedPrimaryContainer = animateColorAsState(targetValue = colorScheme.primaryContainer, animationSpec = animationSpec, label = "primaryContainer").value
    val animatedSecondaryContainer = animateColorAsState(targetValue = colorScheme.secondaryContainer, animationSpec = animationSpec, label = "secondaryContainer").value

    val animatedColorScheme = colorScheme.copy(
        primary = animatedPrimary,
        secondary = animatedSecondary,
        tertiary = animatedTertiary,
        background = animatedBackground,
        surface = animatedSurface,
        surfaceVariant = animatedSurfaceVariant,
        onPrimary = animatedOnPrimary,
        onSecondary = animatedOnSecondary,
        onTertiary = animatedOnTertiary,
        onBackground = animatedOnBackground,
        onSurface = animatedOnSurface,
        onSurfaceVariant = animatedOnSurfaceVariant,
        outline = animatedOutline,
        primaryContainer = animatedPrimaryContainer,
        secondaryContainer = animatedSecondaryContainer
    )

    MaterialTheme(colorScheme = animatedColorScheme, typography = Typography, content = content)
}
