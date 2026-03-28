package com.music.revive.data.color

import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import coil.ImageLoader
import coil.request.ImageRequest
import coil.request.SuccessResult
import com.music.revive.presentation.theme.AlbumColors
import com.music.revive.presentation.theme.ColorExtractor
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
    suspend fun extractColors(imageUrl: String?, isDarkTheme: Boolean = false): AlbumColors {
        if (imageUrl.isNullOrEmpty()) {
            return AlbumColors.Default
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
                        val colors = ColorExtractor.extractFromBitmap(bitmap, isDarkTheme)
                        
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
                        AlbumColors.Default
                    }
                } else {
                    AlbumColors.Default
                }
            } catch (e: Exception) {
                // Return default colors on error
                AlbumColors.Default
            }
        }
    }
    
    /**
     * Extract colors from a Bitmap directly
     * 
     * @param bitmap The bitmap to extract colors from
     * @return AlbumColors object containing extracted colors
     */
    fun extractFromBitmap(bitmap: Bitmap, isDarkTheme: Boolean = false): AlbumColors {
        if (bitmap.isRecycled) {
            return AlbumColors.Default
        }
        
        return ColorExtractor.extractFromBitmap(bitmap, isDarkTheme)
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
