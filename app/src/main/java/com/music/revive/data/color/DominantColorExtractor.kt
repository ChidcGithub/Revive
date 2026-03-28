package com.music.revive.data.color

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.palette.graphics.Palette
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.music.revive.presentation.theme.AlbumColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Dominant Color Extractor
 * 
 * Extracts dominant and accent colors from album artwork using AndroidX Palette API.
 * Optimized for performance with caching and efficient sampling.
 */
@Singleton
class DominantColorExtractor @Inject constructor(
    private val context: Context,
    private val imageLoader: ImageLoader
) {
    // In-memory cache for extracted colors
    private val colorCache = mutableMapOf<String, AlbumColors>()
    
    // Cache size limit to prevent memory issues
    private val maxCacheSize = 100
    
    /**
     * Extract colors from album art URL/URI
     * 
     * @param imageUrl The URL or URI of the album art
     * @return AlbumColors object containing extracted colors
     */
    suspend fun extractColors(imageUrl: String?): AlbumColors {
        if (imageUrl.isNullOrEmpty()) {
            return AlbumColors(null, null, null, null, null)
        }
        
        // Check cache first
        colorCache[imageUrl]?.let { return it }
        
        return withContext(Dispatchers.IO) {
            try {
                // Load bitmap using Coil
                val request = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .allowHardware(false) // Required for Palette
                    .build()
                
                val result = imageLoader.execute(request)
                
                if (result is SuccessResult) {
                    val bitmap = (result.drawable as? BitmapDrawable)?.bitmap
                    
                    if (bitmap != null && !bitmap.isRecycled) {
                        val colors = extractFromBitmap(bitmap)
                        
                        // Cache the result
                        if (colorCache.size >= maxCacheSize) {
                            // Remove oldest entry if cache is full
                            val iterator = colorCache.iterator()
                            if (iterator.hasNext()) {
                                iterator.next()
                                iterator.remove()
                            }
                        }
                        colorCache[imageUrl] = colors
                        
                        colors
                    } else {
                        AlbumColors(null, null, null, null, null)
                    }
                } else {
                    AlbumColors(null, null, null, null, null)
                }
            } catch (e: Exception) {
                // Return default colors on error
                AlbumColors(null, null, null, null, null)
            }
        }
    }
    
    /**
     * Extract colors from a Bitmap directly
     * 
     * @param bitmap The bitmap to extract colors from
     * @return AlbumColors object containing extracted colors
     */
    fun extractFromBitmap(bitmap: Bitmap): AlbumColors {
        if (bitmap.isRecycled) {
            return AlbumColors(null, null, null, null, null)
        }
        
        // Generate palette with optimized settings
        val palette = Palette.from(bitmap)
            .maximumColorCount(16) // More colors for better selection
            .addFilter(true) { _, rgb ->
                // Filter out very dark and very light colors
                val hsl = FloatArray(3)
                android.graphics.Color.RGBToHSV(
                    android.graphics.Color.red(rgb),
                    android.graphics.Color.green(rgb),
                    android.graphics.Color.blue(rgb),
                    hsl
                )
                // Keep colors with saturation > 0.15 and lightness between 0.15 and 0.85
                hsl[1] > 0.15f && hsl[2] in 0.15f..0.85f
            }
            .generate()
        
        // Extract individual color swatches
        val dominantSwatch = palette.dominantSwatch
        val vibrantSwatch = palette.vibrantSwatch
        val mutedSwatch = palette.mutedSwatch
        val lightVibrantSwatch = palette.lightVibrantSwatch
        val darkVibrantSwatch = palette.darkVibrantSwatch
        
        // Convert to Compose Colors
        val dominant = dominantSwatch?.rgb?.let { Color(it) }
        val vibrant = vibrantSwatch?.rgb?.let { Color(it) }
        val muted = mutedSwatch?.rgb?.let { Color(it) }
        val light = lightVibrantSwatch?.rgb?.let { Color(it) } ?: vibrantSwatch?.rgb?.let { Color(it) }
        val dark = darkVibrantSwatch?.rgb?.let { Color(it) } ?: mutedSwatch?.rgb?.let { Color(it) }
        
        return AlbumColors(
            dominant = dominant,
            vibrant = vibrant,
            muted = muted,
            light = light,
            dark = dark
        )
    }
    
    /**
     * Clear the color cache
     * Useful when memory is low or on explicit user action
     */
    fun clearCache() {
        colorCache.clear()
    }
    
    /**
     * Remove specific entry from cache
     */
    fun removeFromCache(imageUrl: String) {
        colorCache.remove(imageUrl)
    }
    
    /**
     * Get cached colors without extraction
     */
    fun getCachedColors(imageUrl: String?): AlbumColors? {
        if (imageUrl.isNullOrEmpty()) return null
        return colorCache[imageUrl]
    }
    
    /**
     * Preload colors for multiple images
     * Call this in background to warm up cache
     */
    suspend fun preloadColors(imageUrls: List<String>) {
        withContext(Dispatchers.IO) {
            imageUrls.forEach { url ->
                // Only extract if not already cached
                if (!colorCache.containsKey(url)) {
                    extractColors(url)
                }
            }
        }
    }
}
