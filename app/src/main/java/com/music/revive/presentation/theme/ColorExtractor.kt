package com.music.revive.presentation.theme

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.core.graphics.ColorUtils
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Extracted color data from album art
 */
data class AlbumColors(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val secondary: Color,
    val onSecondary: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,
    val tertiary: Color,
    val onTertiary: Color,
    val tertiaryContainer: Color,
    val onTertiaryContainer: Color,
    val surface: Color,
    val onSurface: Color,
    val surfaceVariant: Color,
    val onSurfaceVariant: Color,
    val background: Color,
    val onBackground: Color
) {
    companion object {
        val Default = AlbumColors(
            primary = Color(0xFF6750A4),
            onPrimary = Color.White,
            primaryContainer = Color(0xFFEADDFF),
            onPrimaryContainer = Color(0xFF21005D),
            secondary = Color(0xFF625B71),
            onSecondary = Color.White,
            secondaryContainer = Color(0xFFE8DEF8),
            onSecondaryContainer = Color(0xFF1D192B),
            tertiary = Color(0xFF7D5260),
            onTertiary = Color.White,
            tertiaryContainer = Color(0xFFFFD8E4),
            onTertiaryContainer = Color(0xFF31111D),
            surface = Color(0xFFFFFBFE),
            onSurface = Color(0xFF1C1B1F),
            surfaceVariant = Color(0xFFE7E0EC),
            onSurfaceVariant = Color(0xFF49454F),
            background = Color(0xFFFFFBFE),
            onBackground = Color(0xFF1C1B1F)
        )
    }
}

/**
 * Color extraction utilities for album art
 */
object ColorExtractor {
    
    /**
     * Extract colors from a bitmap using Palette
     */
    fun extractFromBitmap(bitmap: Bitmap, isDarkTheme: Boolean): AlbumColors {
        val palette = Palette.from(bitmap)
            .maximumColorCount(24)
            .generate()
        
        // Get dominant colors from palette
        val vibrant = palette.vibrantSwatch
        val darkVibrant = palette.darkVibrantSwatch
        val lightVibrant = palette.lightVibrantSwatch
        val muted = palette.mutedSwatch
        val darkMuted = palette.darkMutedSwatch
        val lightMuted = palette.lightMutedSwatch
        val dominant = palette.dominantSwatch
        
        // Choose primary based on theme
        val primarySwatch = if (isDarkTheme) {
            vibrant ?: lightVibrant ?: dominant ?: muted
        } else {
            vibrant ?: darkVibrant ?: dominant ?: muted
        }
        
        val secondarySwatch = if (isDarkTheme) {
            muted ?: lightMuted ?: vibrant
        } else {
            muted ?: darkMuted ?: vibrant
        }
        
        val tertiarySwatch = if (isDarkTheme) {
            darkVibrant ?: lightVibrant ?: muted
        } else {
            lightVibrant ?: darkVibrant ?: muted
        }
        
        val primaryColor = primarySwatch?.rgb?.let { Color(it) } ?: AlbumColors.Default.primary
        val secondaryColor = secondarySwatch?.rgb?.let { Color(it) } ?: AlbumColors.Default.secondary
        val tertiaryColor = tertiarySwatch?.rgb?.let { Color(it) } ?: AlbumColors.Default.tertiary
        
        return if (isDarkTheme) {
            createDarkAlbumColors(primaryColor, secondaryColor, tertiaryColor)
        } else {
            createLightAlbumColors(primaryColor, secondaryColor, tertiaryColor)
        }
    }
    
    /**
     * Create dark theme album colors from primary color
     */
    private fun createDarkAlbumColors(
        primary: Color,
        secondary: Color,
        tertiary: Color
    ): AlbumColors {
        val primaryHsl = FloatArray(3)
        ColorUtils.colorToHSL(primary.toArgb(), primaryHsl)
        
        val secondaryHsl = FloatArray(3)
        ColorUtils.colorToHSL(secondary.toArgb(), secondaryHsl)
        
        val tertiaryHsl = FloatArray(3)
        ColorUtils.colorToHSL(tertiary.toArgb(), tertiaryHsl)
        
        // Ensure good contrast for dark theme
        val adjustedPrimary = adjustLuminanceForDark(primaryHsl, 0.7f)
        val adjustedSecondary = adjustLuminanceForDark(secondaryHsl, 0.6f)
        val adjustedTertiary = adjustLuminanceForDark(tertiaryHsl, 0.65f)
        
        return AlbumColors(
            primary = adjustedPrimary,
            onPrimary = Color(0xFF000000),
            primaryContainer = adjustLuminanceForDark(primaryHsl, 0.25f),
            onPrimaryContainer = adjustLuminanceForDark(primaryHsl, 0.9f),
            secondary = adjustedSecondary,
            onSecondary = Color(0xFF000000),
            secondaryContainer = adjustLuminanceForDark(secondaryHsl, 0.2f),
            onSecondaryContainer = adjustLuminanceForDark(secondaryHsl, 0.85f),
            tertiary = adjustedTertiary,
            onTertiary = Color(0xFF000000),
            tertiaryContainer = adjustLuminanceForDark(tertiaryHsl, 0.22f),
            onTertiaryContainer = adjustLuminanceForDark(tertiaryHsl, 0.88f),
            surface = Color(0xFF1C1B1F),
            onSurface = Color(0xFFE6E1E5),
            surfaceVariant = Color(0xFF49454F),
            onSurfaceVariant = Color(0xFFCAC4D0),
            background = Color(0xFF1C1B1F),
            onBackground = Color(0xFFE6E1E5)
        )
    }
    
    /**
     * Create light theme album colors from primary color
     */
    private fun createLightAlbumColors(
        primary: Color,
        secondary: Color,
        tertiary: Color
    ): AlbumColors {
        val primaryHsl = FloatArray(3)
        ColorUtils.colorToHSL(primary.toArgb(), primaryHsl)
        
        val secondaryHsl = FloatArray(3)
        ColorUtils.colorToHSL(secondary.toArgb(), secondaryHsl)
        
        val tertiaryHsl = FloatArray(3)
        ColorUtils.colorToHSL(tertiary.toArgb(), tertiaryHsl)
        
        // Ensure good contrast for light theme
        val adjustedPrimary = adjustLuminanceForLight(primaryHsl, 0.4f)
        val adjustedSecondary = adjustLuminanceForLight(secondaryHsl, 0.45f)
        val adjustedTertiary = adjustLuminanceForLight(tertiaryHsl, 0.42f)
        
        return AlbumColors(
            primary = adjustedPrimary,
            onPrimary = Color.White,
            primaryContainer = adjustLuminanceForLight(primaryHsl, 0.9f),
            onPrimaryContainer = adjustLuminanceForLight(primaryHsl, 0.15f),
            secondary = adjustedSecondary,
            onSecondary = Color.White,
            secondaryContainer = adjustLuminanceForLight(secondaryHsl, 0.88f),
            onSecondaryContainer = adjustLuminanceForLight(secondaryHsl, 0.18f),
            tertiary = adjustedTertiary,
            onTertiary = Color.White,
            tertiaryContainer = adjustLuminanceForLight(tertiaryHsl, 0.87f),
            onTertiaryContainer = adjustLuminanceForLight(tertiaryHsl, 0.16f),
            surface = Color(0xFFFFFBFE),
            onSurface = Color(0xFF1C1B1F),
            surfaceVariant = Color(0xFFE7E0EC),
            onSurfaceVariant = Color(0xFF49454F),
            background = Color(0xFFFFFBFE),
            onBackground = Color(0xFF1C1B1F)
        )
    }
    
    private fun adjustLuminanceForDark(hsl: FloatArray, targetLuminance: Float): Color {
        val adjusted = hsl.copyOf()
        adjusted[2] = targetLuminance.coerceIn(0f, 1f)
        // Slightly desaturate for dark theme
        adjusted[1] = (adjusted[1] * 0.85f).coerceIn(0f, 1f)
        return Color(ColorUtils.HSLToColor(adjusted))
    }
    
    private fun adjustLuminanceForLight(hsl: FloatArray, targetLuminance: Float): Color {
        val adjusted = hsl.copyOf()
        adjusted[2] = targetLuminance.coerceIn(0f, 1f)
        return Color(ColorUtils.HSLToColor(adjusted))
    }
    
    /**
     * Load image and extract colors
     */
    suspend fun extractFromUri(
        context: Context,
        uri: String?,
        isDarkTheme: Boolean
    ): AlbumColors? = withContext(Dispatchers.IO) {
        if (uri.isNullOrBlank()) return@withContext null
        
        try {
            val loader = ImageLoader(context)
            val request = ImageRequest.Builder(context)
                .data(uri)
                .size(128) // Small size for faster extraction
                .allowHardware(false) // Required for Palette
                .build()
            
            val result = loader.execute(request)
            if (result is SuccessResult) {
                val bitmap = (result.drawable as? BitmapDrawable)?.bitmap
                if (bitmap != null) {
                    return@withContext extractFromBitmap(bitmap, isDarkTheme)
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }
}

/**
 * Convert AlbumColors to Material 3 ColorScheme for dark theme
 */
fun AlbumColors.toDarkColorScheme(): ColorScheme = darkColorScheme(
    primary = primary,
    onPrimary = onPrimary,
    primaryContainer = primaryContainer,
    onPrimaryContainer = onPrimaryContainer,
    secondary = secondary,
    onSecondary = onSecondary,
    secondaryContainer = secondaryContainer,
    onSecondaryContainer = onSecondaryContainer,
    tertiary = tertiary,
    onTertiary = onTertiary,
    tertiaryContainer = tertiaryContainer,
    onTertiaryContainer = onTertiaryContainer,
    surface = surface,
    onSurface = onSurface,
    surfaceVariant = surfaceVariant,
    onSurfaceVariant = onSurfaceVariant,
    background = background,
    onBackground = onBackground
)

/**
 * Convert AlbumColors to Material 3 ColorScheme for light theme
 */
fun AlbumColors.toLightColorScheme(): ColorScheme = lightColorScheme(
    primary = primary,
    onPrimary = onPrimary,
    primaryContainer = primaryContainer,
    onPrimaryContainer = onPrimaryContainer,
    secondary = secondary,
    onSecondary = onSecondary,
    secondaryContainer = secondaryContainer,
    onSecondaryContainer = onSecondaryContainer,
    tertiary = tertiary,
    onTertiary = onTertiary,
    tertiaryContainer = tertiaryContainer,
    onTertiaryContainer = onTertiaryContainer,
    surface = surface,
    onSurface = onSurface,
    surfaceVariant = surfaceVariant,
    onSurfaceVariant = onSurfaceVariant,
    background = background,
    onBackground = onBackground
)

/**
 * Composable function to extract and remember album colors
 */
@Composable
fun rememberAlbumColors(
    albumArtUri: String?,
    isDarkTheme: Boolean
): AlbumColors {
    val context = LocalContext.current
    var albumColors by remember { mutableStateOf(AlbumColors.Default) }
    
    LaunchedEffect(albumArtUri, isDarkTheme) {
        val colors = ColorExtractor.extractFromUri(context, albumArtUri, isDarkTheme)
        if (colors != null) {
            albumColors = colors
        }
    }
    
    return albumColors
}
