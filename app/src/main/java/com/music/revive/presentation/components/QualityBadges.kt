package com.music.revive.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.revive.domain.model.Song
import com.music.revive.domain.model.AudioQuality
import com.music.revive.presentation.theme.*

/**
 * Audio Quality Badge
 * 
 * Displays audio quality indicator (Lossless, Hi-Res, etc.)
 * Apple Music style badge with appropriate colors.
 */
@Composable
fun AudioQualityBadge(
    audioQuality: AudioQuality,
    modifier: Modifier = Modifier,
    showLabel: Boolean = true
) {
    val (badgeColor, label) = when (audioQuality) {
        AudioQuality.HI_RES -> Pair(BadgeHiRes, "HI-RES")
        AudioQuality.LOSSLESS -> Pair(BadgeLossless, "LOSSLESS")
        AudioQuality.EXTREME -> Pair(BadgeDolbyAtmos, "HIGH QUALITY")
        AudioQuality.HIGH -> Pair(AppleMusicBlue, "HIGH")
        AudioQuality.GOOD -> Pair(SuccessGreen, "GOOD")
        AudioQuality.STANDARD -> Pair(AppleMusicDarkSecondaryText, "STANDARD")
        AudioQuality.LOW -> Pair(Color.Gray, "LOW")
        AudioQuality.UNKNOWN -> Pair(Color.Gray, null)
    }
    
    if (label == null) return
    
    Surface(
        modifier = modifier
            .height(20.dp),
        shape = RoundedCornerShape(4.dp),
        color = badgeColor
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showLabel) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp,
                    letterSpacing = 0.5.sp,
                    color = Color.White,
                    maxLines = 1
                )
            }
        }
    }
}

/**
 * Explicit Content Badge
 * 
 * Small "E" label for explicit content warning.
 */
@Composable
fun ExplicitBadge(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .size(20.dp),
        shape = RoundedCornerShape(4.dp),
        color = BadgeExplicit
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "E",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                color = Color.White
            )
        }
    }
}

/**
 * Spatial Audio Badge (Future Enhancement)
 * 
 * Placeholder for Dolby Atmos / Spatial Audio indicator.
 */
@Composable
fun SpatialAudioBadge(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier
            .height(20.dp),
        shape = RoundedCornerShape(4.dp),
        color = BadgeDolbyAtmos
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 6.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "DOLBY ATMOS",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                fontSize = 8.sp,
                letterSpacing = 0.8.sp,
                color = Color.White,
                maxLines = 1
            )
        }
    }
}
