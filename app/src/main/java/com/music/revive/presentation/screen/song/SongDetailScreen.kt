package com.music.revive.presentation.screen.song

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.music.revive.R
import com.music.revive.data.repository.MusicRepository
import com.music.revive.domain.model.AudioQuality
import com.music.revive.domain.model.Song
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * Audio quality badge component with M3 styling
 */
@Composable
fun AudioQualityBadge(
    quality: AudioQuality,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, contentColor) = when (quality) {
        AudioQuality.LOSSLESS -> 
            MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.onPrimaryContainer
        AudioQuality.HIGH -> 
            MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.onTertiaryContainer
        AudioQuality.MEDIUM -> 
            MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.onSecondaryContainer
        AudioQuality.STANDARD -> 
            MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
        AudioQuality.LOW -> 
            MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
        AudioQuality.UNKNOWN -> 
            MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(4.dp),
        color = backgroundColor
    ) {
        Text(
            text = quality.shortLabel,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

data class SongDetailUiState(
    val song: Song? = null,
    val isFavorite: Boolean = false,
    val isLoading: Boolean = true
)

@HiltViewModel
class SongDetailViewModel @Inject constructor(
    private val repository: MusicRepository,
    savedStateHandle: androidx.lifecycle.SavedStateHandle
) : ViewModel() {

    private val songId: Long = savedStateHandle["songId"] ?: -1L

    private val _uiState = MutableStateFlow(SongDetailUiState())
    val uiState: StateFlow<SongDetailUiState> = _uiState.asStateFlow()

    init {
        loadSong()
    }

    private fun loadSong() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            val songs = repository.getAllSongs()
            val song = songs.find { it.id == songId }

            if (song != null) {
                repository.isFavorite(songId).collect { isFav ->
                    _uiState.value = SongDetailUiState(
                        song = song,
                        isFavorite = isFav,
                        isLoading = false
                    )
                }
            } else {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun toggleFavorite() {
        viewModelScope.launch {
            if (_uiState.value.isFavorite) {
                repository.removeFromFavorites(songId)
            } else {
                repository.addToFavorites(songId)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongDetailScreen(
    onNavigateBack: () -> Unit,
    onPlaySong: (Song, List<Song>) -> Unit,
    onAlbumClick: (Long) -> Unit,
    onArtistClick: (Long) -> Unit,
    viewModel: SongDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var showDeleteDialog by remember { mutableStateOf(false) }

    if (uiState.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    val song = uiState.song ?: run {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(stringResource(R.string.no_songs))
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.song_info)) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleFavorite() }) {
                        Icon(
                            imageVector = if (uiState.isFavorite) Icons.Default.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = if (uiState.isFavorite) stringResource(R.string.remove_from_favorites) else stringResource(R.string.add_to_favorites),
                            tint = if (uiState.isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            // Album Art
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    modifier = Modifier
                        .size(240.dp)
                        .clip(RoundedCornerShape(16.dp)),
                    tonalElevation = 4.dp
                ) {
                    AsyncImage(
                        model = song.albumArtUri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                    if (song.albumArtUri == null) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.MusicNote,
                                contentDescription = null,
                                modifier = Modifier.size(80.dp),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            // Song Info Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.headlineSmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = song.artist,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = song.album,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Action Buttons
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { onPlaySong(song, listOf(song)) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.play))
                }
                OutlinedButton(
                    onClick = { /* Add to queue */ },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.AddToQueue, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.add_to_queue))
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Details Section
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = stringResource(R.string.song_info),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(16.dp))

                    // Audio Quality Badge
                    if (song.audioQuality != AudioQuality.UNKNOWN) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.audio_quality),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            AudioQualityBadge(quality = song.audioQuality)
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    }

                    DetailRow(
                        label = stringResource(R.string.duration),
                        value = song.formattedDuration
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    // Bitrate
                    if (song.bitrate > 0) {
                        DetailRow(
                            label = stringResource(R.string.bitrate),
                            value = "${song.bitrate} kbps"
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    }

                    // Sample Rate
                    if (song.sampleRate > 0) {
                        DetailRow(
                            label = stringResource(R.string.sample_rate),
                            value = song.formattedSampleRate
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    }

                    DetailRow(
                        label = stringResource(R.string.album),
                        value = song.album
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    DetailRow(
                        label = stringResource(R.string.artist),
                        value = song.artist
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    val file = File(song.path)
                    DetailRow(
                        label = stringResource(R.string.file_name),
                        value = file.name
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    DetailRow(
                        label = stringResource(R.string.file_path),
                        value = song.path
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    val fileSize = file.length()
                    val fileSizeFormatted = formatFileSize(fileSize)
                    DetailRow(
                        label = stringResource(R.string.file_size),
                        value = fileSizeFormatted
                    )
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                    val dateAdded = dateFormat.format(Date(song.dateAdded))
                    DetailRow(
                        label = stringResource(R.string.date_added),
                        value = dateAdded
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quick Actions
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column {
                    if (song.albumId != null) {
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.go_to_album)) },
                            leadingContent = {
                                Icon(Icons.Default.Album, contentDescription = null)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ListItemDefaults.colors(
                                containerColor = Color.Transparent
                            )
                        )
                    }
                    if (song.artistId != null) {
                        ListItem(
                            headlineContent = { Text(stringResource(R.string.go_to_artist)) },
                            leadingContent = {
                                Icon(Icons.Default.Person, contentDescription = null)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ListItemDefaults.colors(
                                containerColor = Color.Transparent
                            )
                        )
                    }
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.share)) },
                        leadingContent = {
                            Icon(Icons.Default.Share, contentDescription = null)
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ListItemDefaults.colors(
                            containerColor = Color.Transparent
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false)
        )
    }
}

private fun formatFileSize(bytes: Long): String {
    return when {
        bytes < 1024 -> "$bytes B"
        bytes < 1024 * 1024 -> String.format("%.1f KB", bytes / 1024.0)
        bytes < 1024 * 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024))
        else -> String.format("%.1f GB", bytes / (1024.0 * 1024 * 1024))
    }
}
