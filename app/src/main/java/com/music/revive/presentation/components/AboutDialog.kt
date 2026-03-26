package com.music.revive.presentation.components

import android.content.Intent
import android.net.Uri
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.music.revive.BuildConfig
import com.music.revive.R
import kotlinx.coroutines.delay

/**
 * About dialog showing app information
 */
@Composable
fun AboutDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Animation states
    var isVisible by remember { mutableStateOf(false) }
    
    val iconScale by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "iconScale"
    )
    
    val contentAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(400, easing = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)),
        label = "contentAlpha"
    )
    
    LaunchedEffect(Unit) {
        isVisible = true
    }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            dismissOnBackPress = true,
            dismissOnClickOutside = true,
            usePlatformDefaultWidth = false
        )
    ) {
        Card(
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = MaterialTheme.shapes.extraLarge,
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // App Icon with animation and gradient background
                Surface(
                    modifier = Modifier
                        .size(120.dp)
                        .scale(iconScale),
                    shape = CircleShape,
                    tonalElevation = 4.dp
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        MaterialTheme.colorScheme.primaryContainer,
                                        MaterialTheme.colorScheme.secondaryContainer
                                    )
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            painter = painterResource(id = R.mipmap.ic_launcher),
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(0.7f),
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(24.dp))
                
                // App name
                Text(
                    text = stringResource(R.string.app_name),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                // Codename
                Text(
                    text = "\"${BuildConfig.VERSION_CODENAME}\"",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Version info
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceContainerHighest
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        InfoRow(
                            icon = Icons.Rounded.Tag,
                            label = "版本",
                            value = BuildConfig.VERSION_NAME
                        )
                        
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                        
                        InfoRow(
                            icon = Icons.Rounded.Build,
                            label = "Build",
                            value = getBuildInfo()
                        )
                        
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                        
                        InfoRow(
                            icon = Icons.Rounded.Code,
                            label = "Git SHA",
                            value = BuildConfig.GIT_SHA
                        )
                        
                        HorizontalDivider(
                            modifier = Modifier.padding(vertical = 8.dp),
                            thickness = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                        
                        InfoRow(
                            icon = Icons.Rounded.Version,
                            label = "完整版本",
                            value = getFullVersionString()
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Developer info
                DeveloperInfo()
                
                Spacer(modifier = Modifier.height(16.dp))
                
                // Links section
                LinkSection()
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Copyright
                Text(
                    text = "© ${java.time.Year.now().value} Revive Team",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = "Made with ❤️ for music lovers",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun InfoRow(
    icon: ImageVector,
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun DeveloperInfo() {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "开发者",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                DeveloperChip(
                    name = "ChidcGithub",
                    role = "主开发者"
                )
                
                DeveloperChip(
                    name = "Revive Team",
                    role = "贡献者"
                )
            }
        }
    }
}

@Composable
private fun DeveloperChip(
    name: String,
    role: String
) {
    Surface(
        shape = MaterialTheme.shapes.small,
        color = MaterialTheme.colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = name,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = role,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun LinkSection() {
    val context = LocalContext.current
    
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.2f)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "链接",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // GitHub
            LinkItem(
                icon = Icons.Rounded.Code,
                text = "GitHub 仓库",
                onClick = {
                    try {
                        val intent = Intent(Intent.ACTION_VIEW).apply {
                            data = Uri.parse("https://github.com/ChidcGithub/Revive")
                        }
                        context.startActivity(intent)
                    } catch (e: Exception) {
                        // Ignore
                    }
                }
            )
            
            Spacer(modifier = Modifier.height(4.dp))
            
            // Licenses
            LinkItem(
                icon = Icons.Rounded.Description,
                text = stringResource(R.string.open_source_licenses),
                onClick = {
                    // TODO: Show licenses dialog
                }
            )
        }
    }
}

@Composable
private fun LinkItem(
    icon: ImageVector,
    text: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.size(20.dp)
        )
        
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.weight(1f))
        
        Icon(
            imageVector = Icons.Rounded.OpenInNew,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
        )
    }
}

/**
 * Get build info string matching workflow format
 * Format: type + number (e.g., "b45" or "r12")
 */
private fun getBuildInfo(): String {
    val typeLetter = when (BuildConfig.BUILD_TYPE) {
        "r" -> "r"  // Release
        "b" -> "b"  // Beta
        else -> BuildConfig.BUILD_TYPE.take(1).lowercase()
    }
    return "$typeLetter${BuildConfig.BUILD_NUMBER}"
}

/**
 * Get full version string matching workflow format
 * Format: versionName.buildType+buildNumber-gitSha
 * Example: 0.1.1.b45-a1b2c3d
 */
private fun getFullVersionString(): String {
    return "${BuildConfig.VERSION_NAME}.${getBuildInfo()}-${BuildConfig.GIT_SHA}"
}
