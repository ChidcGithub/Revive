package com.music.revive.presentation.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import com.music.revive.data.local.ColorSource
import com.music.revive.presentation.theme.toDarkColorScheme
import com.music.revive.presentation.theme.toLightColorScheme

private fun findActivity(context: Context): Activity {
    var contextInner = context
    while (contextInner is ContextWrapper) {
        if (contextInner is Activity) return contextInner
        contextInner = contextInner.baseContext
    }
    return error("Couldn't find activity")
}

// MARK: - Apple Music Color Schemes

/**
 * Light Color Scheme - Apple Music Style
 * Clean, bright, with vibrant accent colors
 */
private val LightColorScheme = lightColorScheme(
    primary = AppleMusicPrimary,
    onPrimary = AppleMusicOnPrimary,
    primaryContainer = AppleMusicPrimaryLight,
    onPrimaryContainer = AppleMusicOnPrimary,
    secondary = AppleMusicBlue,
    onSecondary = AppleMusicOnBlue,
    secondaryContainer = Color(0xFFD4E5FF),
    onSecondaryContainer = AppleMusicBlueDark,
    tertiary = AppleMusicPrimary,
    onTertiary = AppleMusicOnPrimary,
    tertiaryContainer = Color(0xFFFFD6E0),
    onTertiaryContainer = AppleMusicPrimaryDark,
    error = ErrorRed,
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD8),
    onErrorContainer = Color(0xFF740000),
    background = AppleMusicLightSurface,
    onBackground = AppleMusicLightPrimaryText,
    surface = AppleMusicLightSurfaceSecondary,
    onSurface = AppleMusicLightPrimaryText,
    surfaceVariant = AppleMusicLightGroupedBackground,
    onSurfaceVariant = AppleMusicLightSecondaryText,
    outline = AppleMusicLightSeparator,
    outlineVariant = Color(0xFFE5E5E5),
    inverseSurface = AppleMusicDarkSurface,
    inverseOnSurface = AppleMusicDarkPrimaryText,
    inversePrimary = AppleMusicPrimaryLight,
    surfaceTint = AppleMusicPrimary,
    scrim = Color.Black,
)

/**
 * Dark Color Scheme - Apple Music Style
 * Deep blacks with subtle gray layers for depth
 */
private val DarkColorScheme = darkColorScheme(
    primary = AppleMusicPrimary,
    onPrimary = AppleMusicOnPrimary,
    primaryContainer = AppleMusicPrimaryDark,
    onPrimaryContainer = AppleMusicOnPrimary,
    secondary = AppleMusicBlue,
    onSecondary = AppleMusicOnBlue,
    secondaryContainer = Color(0xFF003D73),
    onSecondaryContainer = AppleMusicBlueLight,
    tertiary = AppleMusicPrimary,
    onTertiary = AppleMusicOnPrimary,
    tertiaryContainer = AppleMusicPrimaryDark,
    onTertiaryContainer = AppleMusicOnPrimary,
    error = ErrorRed,
    onError = Color.White,
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = AppleMusicBlack,
    onBackground = AppleMusicDarkPrimaryText,
    surface = AppleMusicDarkSurface,
    onSurface = AppleMusicDarkPrimaryText,
    surfaceVariant = AppleMusicDarkSurfaceSecondary,
    onSurfaceVariant = AppleMusicDarkSecondaryText,
    outline = AppleMusicDarkSeparator,
    outlineVariant = Color(0xFF28282A),
    inverseSurface = AppleMusicLightSurface,
    inverseOnSurface = AppleMusicLightPrimaryText,
    inversePrimary = AppleMusicPrimary,
    surfaceTint = AppleMusicPrimary,
    scrim = Color.Black,
)

// MARK: - Apple Music Shape System

/**
 * Apple Music Shape System
 * Optimized for modern, rounded aesthetic matching iOS design language
 */
val AppleMusicShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),      // Small chips, tags
    small = RoundedCornerShape(8.dp),           // Buttons, inputs
    medium = RoundedCornerShape(12.dp),         // Cards, dialogs
    large = RoundedCornerShape(16.dp),          // Large cards, bottom sheets
    extraLarge = RoundedCornerShape(24.dp)      // FABs, full-width elements
)

// Legacy compatibility
val ReviveShapes = AppleMusicShapes

// MARK: - Album Color Scheme Extensions

// MARK: - Revive Theme Composable

/**
 * Revive Material 3 Theme with Apple Music Styling
 * 
 * @param darkTheme Whether to use dark theme
 * @param dynamicColor Whether to use dynamic colors (Material You) on Android 12+
 * @param themeMode Override theme mode (light, dark, system)
 * @param colorSource The source of theme colors
 * @param albumColors Optional album-based colors for album color mode
 */
@Composable
fun ReviveTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    colorSource: ColorSource = if (dynamicColor) ColorSource.DYNAMIC else ColorSource.STATIC,
    albumColors: AlbumColors? = null,
    content: @Composable () -> Unit
) {
    val useDarkTheme = when (themeMode) {
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
        ThemeMode.SYSTEM -> darkTheme
    }

    val targetColorScheme = when {
        // Album colors take priority when provided and color source is ALBUM
        colorSource == ColorSource.ALBUM && albumColors != null -> {
            if (useDarkTheme) albumColors.toDarkColorScheme() else albumColors.toLightColorScheme()
        }
        // Dynamic colors (Material You)
        colorSource == ColorSource.DYNAMIC && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (useDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        // Static Apple Music colors
        useDarkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    
    // Animate color transitions smoothly with Apple-like easing
    val colorScheme = animateColorScheme(targetColorScheme)

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = findActivity(view.context).window
            
            // Set transparent status and navigation bars for edge-to-edge
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            
            // Configure window insets controller
            WindowCompat.getInsetsController(window, view).apply {
                // Status bar icons contrast
                isAppearanceLightStatusBars = !useDarkTheme
                
                // Navigation bar icons contrast  
                isAppearanceLightNavigationBars = !useDarkTheme
            }
            
            // Enable drawing behind system bars
            window.setDecorFitsSystemWindows(false)
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppleMusicTypography,
        shapes = AppleMusicShapes,
        content = content
    )
}

// MARK: - Color Animation

/**
 * Animate color scheme transitions with Apple-style smooth easing
 * Uses cubic bezier curve for natural, fluid transitions
 */
@Composable
private fun animateColorScheme(targetColorScheme: ColorScheme): ColorScheme {
    // Apple-style smooth easing curve
    val appleEasing = CubicBezierEasing(0.25f, 0.1f, 0.25f, 1.0f)
    
    // Faster spring for snappier response
    val springSpec = spring<Color>(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessMedium
    )
    
    // Tween animation with Apple easing for key properties
    val tweenSpec: AnimationSpec<Color> = tween(
        durationMillis = 300,
        easing = appleEasing
    )
    
    // Only animate the most noticeable colors for performance
    return ColorScheme(
        primary = animateColorAsState(targetColorScheme.primary, springSpec, label = "primary").value,
        onPrimary = animateColorAsState(targetColorScheme.onPrimary, tweenSpec, label = "onPrimary").value,
        primaryContainer = animateColorAsState(targetColorScheme.primaryContainer, tweenSpec, label = "primaryContainer").value,
        onPrimaryContainer = animateColorAsState(targetColorScheme.onPrimaryContainer, tweenSpec, label = "onPrimaryContainer").value,
        inversePrimary = animateColorAsState(targetColorScheme.inversePrimary, tweenSpec, label = "inversePrimary").value,
        secondary = animateColorAsState(targetColorScheme.secondary, tweenSpec, label = "secondary").value,
        onSecondary = animateColorAsState(targetColorScheme.onSecondary, tweenSpec, label = "onSecondary").value,
        secondaryContainer = animateColorAsState(targetColorScheme.secondaryContainer, tweenSpec, label = "secondaryContainer").value,
        onSecondaryContainer = animateColorAsState(targetColorScheme.onSecondaryContainer, tweenSpec, label = "onSecondaryContainer").value,
        tertiary = animateColorAsState(targetColorScheme.tertiary, tweenSpec, label = "tertiary").value,
        onTertiary = animateColorAsState(targetColorScheme.onTertiary, tweenSpec, label = "onTertiary").value,
        tertiaryContainer = animateColorAsState(targetColorScheme.tertiaryContainer, tweenSpec, label = "tertiaryContainer").value,
        onTertiaryContainer = animateColorAsState(targetColorScheme.onTertiaryContainer, tweenSpec, label = "onTertiaryContainer").value,
        background = animateColorAsState(targetColorScheme.background, tweenSpec, label = "background").value,
        onBackground = animateColorAsState(targetColorScheme.onBackground, tweenSpec, label = "onBackground").value,
        surface = animateColorAsState(targetColorScheme.surface, tweenSpec, label = "surface").value,
        onSurface = animateColorAsState(targetColorScheme.onSurface, tweenSpec, label = "onSurface").value,
        surfaceVariant = animateColorAsState(targetColorScheme.surfaceVariant, tweenSpec, label = "surfaceVariant").value,
        onSurfaceVariant = animateColorAsState(targetColorScheme.onSurfaceVariant, tweenSpec, label = "onSurfaceVariant").value,
        surfaceTint = animateColorAsState(targetColorScheme.surfaceTint, tweenSpec, label = "surfaceTint").value,
        inverseSurface = animateColorAsState(targetColorScheme.inverseSurface, tweenSpec, label = "inverseSurface").value,
        inverseOnSurface = animateColorAsState(targetColorScheme.inverseOnSurface, tweenSpec, label = "inverseOnSurface").value,
        error = animateColorAsState(targetColorScheme.error, tweenSpec, label = "error").value,
        onError = animateColorAsState(targetColorScheme.onError, tweenSpec, label = "onError").value,
        errorContainer = animateColorAsState(targetColorScheme.errorContainer, tweenSpec, label = "errorContainer").value,
        onErrorContainer = animateColorAsState(targetColorScheme.onErrorContainer, tweenSpec, label = "onErrorContainer").value,
        outline = animateColorAsState(targetColorScheme.outline, tweenSpec, label = "outline").value,
        outlineVariant = animateColorAsState(targetColorScheme.outlineVariant, tweenSpec, label = "outlineVariant").value,
        scrim = animateColorAsState(targetColorScheme.scrim, tweenSpec, label = "scrim").value,
        surfaceBright = animateColorAsState(targetColorScheme.surfaceBright, tweenSpec, label = "surfaceBright").value,
        surfaceDim = animateColorAsState(targetColorScheme.surfaceDim, tweenSpec, label = "surfaceDim").value,
        surfaceContainer = animateColorAsState(targetColorScheme.surfaceContainer, tweenSpec, label = "surfaceContainer").value,
        surfaceContainerHigh = animateColorAsState(targetColorScheme.surfaceContainerHigh, tweenSpec, label = "surfaceContainerHigh").value,
        surfaceContainerHighest = animateColorAsState(targetColorScheme.surfaceContainerHighest, tweenSpec, label = "surfaceContainerHighest").value,
        surfaceContainerLow = animateColorAsState(targetColorScheme.surfaceContainerLow, tweenSpec, label = "surfaceContainerLow").value,
        surfaceContainerLowest = animateColorAsState(targetColorScheme.surfaceContainerLowest, tweenSpec, label = "surfaceContainerLowest").value
    )
}

// MARK: - Theme Mode Enum

enum class ThemeMode {
    LIGHT,
    DARK,
    SYSTEM
}

// MARK: - Theme State

/**
 * Provides the current theme state for settings
 */
@Composable
fun rememberThemeState(): ThemeState {
    val context = LocalContext.current
    var themeMode by remember { mutableStateOf(ThemeMode.SYSTEM) }
    var colorSource by remember { mutableStateOf(ColorSource.DYNAMIC) }

    return remember(themeMode, colorSource) {
        ThemeState(
            themeMode = themeMode,
            colorSource = colorSource,
            setThemeMode = { themeMode = it },
            setColorSource = { colorSource = it }
        )
    }
}

data class ThemeState(
    val themeMode: ThemeMode,
    val colorSource: ColorSource,
    val setThemeMode: (ThemeMode) -> Unit,
    val setColorSource: (ColorSource) -> Unit
) {
    // Legacy compatibility
    val dynamicColorEnabled: Boolean get() = colorSource == ColorSource.DYNAMIC
    fun setDynamicColorEnabled(enabled: Boolean) = setColorSource(
        if (enabled) ColorSource.DYNAMIC else ColorSource.STATIC
    )
}


