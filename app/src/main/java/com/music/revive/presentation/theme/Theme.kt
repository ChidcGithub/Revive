package com.music.revive.presentation.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat

private fun findActivity(context: Context): Activity {
    var contextInner = context
    while (contextInner is ContextWrapper) {
        if (contextInner is Activity) return contextInner
        contextInner = contextInner.baseContext
    }
    return error("Couldn't find activity")
}

private val LightColorScheme = lightColorScheme(
    primary = md_theme_light_primary,
    onPrimary = md_theme_light_onPrimary,
    primaryContainer = md_theme_light_primaryContainer,
    onPrimaryContainer = md_theme_light_onPrimaryContainer,
    secondary = md_theme_light_secondary,
    onSecondary = md_theme_light_onSecondary,
    secondaryContainer = md_theme_light_secondaryContainer,
    onSecondaryContainer = md_theme_light_onSecondaryContainer,
    tertiary = md_theme_light_tertiary,
    onTertiary = md_theme_light_onTertiary,
    tertiaryContainer = md_theme_light_tertiaryContainer,
    onTertiaryContainer = md_theme_light_onTertiaryContainer,
    error = md_theme_light_error,
    errorContainer = md_theme_light_errorContainer,
    onError = md_theme_light_onError,
    onErrorContainer = md_theme_light_onErrorContainer,
    background = md_theme_light_background,
    onBackground = md_theme_light_onBackground,
    surface = md_theme_light_surface,
    onSurface = md_theme_light_onSurface,
    surfaceVariant = md_theme_light_surfaceVariant,
    onSurfaceVariant = md_theme_light_onSurfaceVariant,
    outline = md_theme_light_outline,
    outlineVariant = md_theme_light_outlineVariant,
    inverseSurface = md_theme_light_inverseSurface,
    inverseOnSurface = md_theme_light_inverseOnSurface,
    inversePrimary = md_theme_light_inversePrimary,
    surfaceTint = md_theme_light_surfaceTint,
    scrim = md_theme_light_scrim,
)

private val DarkColorScheme = darkColorScheme(
    primary = md_theme_dark_primary,
    onPrimary = md_theme_dark_onPrimary,
    primaryContainer = md_theme_dark_primaryContainer,
    onPrimaryContainer = md_theme_dark_onPrimaryContainer,
    secondary = md_theme_dark_secondary,
    onSecondary = md_theme_dark_onSecondary,
    secondaryContainer = md_theme_dark_secondaryContainer,
    onSecondaryContainer = md_theme_dark_onSecondaryContainer,
    tertiary = md_theme_dark_tertiary,
    onTertiary = md_theme_dark_onTertiary,
    tertiaryContainer = md_theme_dark_tertiaryContainer,
    onTertiaryContainer = md_theme_dark_onTertiaryContainer,
    error = md_theme_dark_error,
    errorContainer = md_theme_dark_errorContainer,
    onError = md_theme_dark_onError,
    onErrorContainer = md_theme_dark_onErrorContainer,
    background = md_theme_dark_background,
    onBackground = md_theme_dark_onBackground,
    surface = md_theme_dark_surface,
    onSurface = md_theme_dark_onSurface,
    surfaceVariant = md_theme_dark_surfaceVariant,
    onSurfaceVariant = md_theme_dark_onSurfaceVariant,
    outline = md_theme_dark_outline,
    outlineVariant = md_theme_dark_outlineVariant,
    inverseSurface = md_theme_dark_inverseSurface,
    inverseOnSurface = md_theme_dark_inverseOnSurface,
    inversePrimary = md_theme_dark_inversePrimary,
    surfaceTint = md_theme_dark_surfaceTint,
    scrim = md_theme_dark_scrim,
)

/**
 * Material 3 Shape System
 * Defines corner radii for different component sizes
 */
val ReviveShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),      // Chips, small buttons
    small = RoundedCornerShape(8.dp),           // Buttons, text fields
    medium = RoundedCornerShape(12.dp),         // Cards, dialogs
    large = RoundedCornerShape(16.dp),          // Bottom sheets, large cards
    extraLarge = RoundedCornerShape(28.dp)      // Floating action buttons, navigation drawer
)

/**
 * Revive Material 3 Theme
 * 
 * @param darkTheme Whether to use dark theme
 * @param dynamicColor Whether to use dynamic colors (Material You) on Android 12+
 * @param themeMode Override theme mode (light, dark, system)
 */
@Composable
fun ReviveTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit
) {
    val useDarkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> darkTheme
    }

    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (useDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        useDarkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = findActivity(view.context).window
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !useDarkTheme
                isAppearanceLightNavigationBars = !useDarkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = ReviveTypography,
        shapes = ReviveShapes,
        content = content
    )
}

enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM
}

/**
 * Provides the current theme state for settings
 */
@Composable
fun rememberThemeState(): ThemeState {
    val context = LocalContext.current
    var themeMode by remember { mutableStateOf(ThemeMode.SYSTEM) }
    var dynamicColorEnabled by remember { mutableStateOf(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) }

    return remember(themeMode, dynamicColorEnabled) {
        ThemeState(
            themeMode = themeMode,
            dynamicColorEnabled = dynamicColorEnabled,
            setThemeMode = { themeMode = it },
            setDynamicColorEnabled = { dynamicColorEnabled = it }
        )
    }
}

data class ThemeState(
    val themeMode: ThemeMode,
    val dynamicColorEnabled: Boolean,
    val setThemeMode: (ThemeMode) -> Unit,
    val setDynamicColorEnabled: (Boolean) -> Unit
)