package com.music.revive.presentation.screen.song

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.music.revive.R
import com.music.revive.domain.model.Song
import com.music.revive.presentation.components.CreatePlaylistDialog

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun SongsScreen(
    songs: List<Song>,
    favoriteSongIds: Set<Long>,
    playlists: List<com.music.revive.domain.model.Playlist>,
    onSongClick: (Song, List<Song>) -> Unit,
    onFavoriteClick: (Long) -> Unit,
    onSongDetailClick: (Long) -> Unit = {},
    onAddToPlaylist: (Long, Long) -> Unit,
    onCreatePlaylist: (String) -> Unit
) {
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var songToAddToPlaylist by remember { mutableStateOf<Long?>(null) }
    var expandedSongMenu by remember { mutableStateOf<Long?>(null) }
    var sortOrder by remember { mutableStateOf(SortOrder.TITLE) }
    var showSortMenu by remember { mutableStateOf(false) }
    
    // Sort songs
    val sortedSongs = remember(songs, sortOrder) {
        when (sortOrder) {
            SortOrder.TITLE -> songs.sortedBy { it.title.lowercase() }
            SortOrder.ARTIST -> songs.sortedBy { it.artist.lowercase() }
            SortOrder.ALBUM -> songs.sortedBy { it.album.lowercase() }
            SortOrder.DURATION -> songs.sortedBy { it.duration }
            SortOrder.DATE_ADDED -> songs.sortedByDescending { it.dateAdded }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.all_songs),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp
                        )
                        Text(
                            text = "${songs.size} ${stringResource(R.string.songs)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    // Shuffle all button
                    IconButton(
                        onClick = {
                            if (songs.isNotEmpty()) {
                                onSongClick(songs.random(), songs)
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Shuffle,
                            contentDescription = stringResource(R.string.shuffle)
                        )
                    }
                    // Sort menu
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(
                                imageVector = Icons.Rounded.Sort,
                                contentDescription = stringResource(R.string.sort)
                            )
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.sort_by_title)) },
                                onClick = {
                                    sortOrder = SortOrder.TITLE
                                    showSortMenu = false
                                },
                                leadingIcon = {
                                    if (sortOrder == SortOrder.TITLE) {
                                        Icon(Icons.Rounded.Check, contentDescription = null)
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.sort_by_artist)) },
                                onClick = {
                                    sortOrder = SortOrder.ARTIST
                                    showSortMenu = false
                                },
                                leadingIcon = {
                                    if (sortOrder == SortOrder.ARTIST) {
                                        Icon(Icons.Rounded.Check, contentDescription = null)
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.sort_by_album)) },
                                onClick = {
                                    sortOrder = SortOrder.ALBUM
                                    showSortMenu = false
                                },
                                leadingIcon = {
                                    if (sortOrder == SortOrder.ALBUM) {
                                        Icon(Icons.Rounded.Check, contentDescription = null)
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.sort_by_duration)) },
                                onClick = {
                                    sortOrder = SortOrder.DURATION
                                    showSortMenu = false
                                },
                                leadingIcon = {
                                    if (sortOrder == SortOrder.DURATION) {
                                        Icon(Icons.Rounded.Check, contentDescription = null)
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.sort_by_date_added)) },
                                onClick = {
                                    sortOrder = SortOrder.DATE_ADDED
                                    showSortMenu = false
                                },
                                leadingIcon = {
                                    if (sortOrder == SortOrder.DATE_ADDED) {
                                        Icon(Icons.Rounded.Check, contentDescription = null)
                                    }
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent
                )
            )
        }
    ) { paddingValues ->
        if (songs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Text(
                        text = stringResource(R.string.no_songs_found),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                itemsIndexed(
                    items = sortedSongs,
                    key = { _, song -> song.id }
                ) { index, song ->
                    val isFavorite = favoriteSongIds.contains(song.id)
                    
                    SongListItem(
                        song = song,
                        isFavorite = isFavorite,
                        index = index + 1,
                        onPlayClick = { onSongClick(song, sortedSongs) },
                        onFavoriteClick = { onFavoriteClick(song.id) },
                        onMenuClick = { expandedSongMenu = song.id },
                        onLongClick = { onSongDetailClick(song.id) }
                    )

                    // Dropdown menu
                    DropdownMenu(
                        expanded = expandedSongMenu == song.id,
                        onDismissRequest = { expandedSongMenu = null }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.song_info)) },
                            onClick = {
                                onSongDetailClick(song.id)
                                expandedSongMenu = null
                            },
                            leadingIcon = {
                                Icon(Icons.Rounded.Info, contentDescription = null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.add_to_playlist)) },
                            onClick = {
                                songToAddToPlaylist = song.id
                                expandedSongMenu = null
                            },
                            leadingIcon = {
                                Icon(Icons.Rounded.PlaylistAdd, contentDescription = null)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.add_to_queue)) },
                            onClick = { expandedSongMenu = null },
                            leadingIcon = {
                                Icon(Icons.Rounded.AddToQueue, contentDescription = null)
                            }
                        )
                    }
                }
            }
        }
    }

    // Dialogs
    if (showCreatePlaylistDialog) {
        CreatePlaylistDialog(
            onDismiss = { showCreatePlaylistDialog = false },
            onConfirm = { name ->
                onCreatePlaylist(name)
                showCreatePlaylistDialog = false
            }
        )
    }

    songToAddToPlaylist?.let { songId ->
        AddToPlaylistDialog(
            playlists = playlists,
            onDismiss = { songToAddToPlaylist = null },
            onPlaylistSelected = { playlistId ->
                onAddToPlaylist(playlistId, songId)
                songToAddToPlaylist = null
            },
            onCreateNew = {
                songToAddToPlaylist = null
                showCreatePlaylistDialog = true
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SongListItem(
    song: Song,
    isFavorite: Boolean,
    index: Int,
    onPlayClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onMenuClick: () -> Unit,
    onLongClick: () -> Unit
) {
    // Staggered entry animation
    var isVisible by remember { mutableStateOf(false) }
    
    val smoothEasing = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)
    
    val itemAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(300, index * 20, easing = smoothEasing),
        label = "itemAlpha"
    )
    
    val itemOffset by animateDpAsState(
        targetValue = if (isVisible) 0.dp else 16.dp,
        animationSpec = tween(300, index * 20, easing = smoothEasing),
        label = "itemOffset"
    )

    LaunchedEffect(Unit) {
        isVisible = true
    }

    ListItem(
        headlineContent = {
            Text(
                text = song.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge
            )
        },
        supportingContent = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = song.artist,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "·",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = song.formattedDuration,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        leadingContent = {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = MaterialTheme.shapes.small
            ) {
                if (song.albumArtUri != null) {
                    AsyncImage(
                        model = song.albumArtUri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Image(
                        painter = painterResource(id = R.mipmap.ic_launcher),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onFavoriteClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = if (isFavorite) stringResource(R.string.remove_from_favorites) else stringResource(R.string.add_to_favorites),
                        tint = if (isFavorite) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(
                    onClick = onMenuClick,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MoreVert,
                        contentDescription = stringResource(R.string.more_options),
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        },
        modifier = Modifier
            .combinedClickable(
                onClick = onPlayClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 8.dp)
            .graphicsLayer {
                alpha = itemAlpha
                translationY = itemOffset.toPx()
            }
    )
}

@Composable
private fun AddToPlaylistDialog(
    playlists: List<com.music.revive.domain.model.Playlist>,
    onDismiss: () -> Unit,
    onPlaylistSelected: (Long) -> Unit,
    onCreateNew: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.add_to_playlist)) },
        text = {
            LazyColumn {
                item {
                    ListItem(
                        headlineContent = { Text(stringResource(R.string.create_new_playlist)) },
                        leadingContent = {
                            Icon(Icons.Rounded.Add, contentDescription = null)
                        },
                        modifier = Modifier.clickable(onClick = onCreateNew)
                    )
                }
                itemsIndexed(playlists) { _, playlist ->
                    ListItem(
                        headlineContent = { Text(playlist.name) },
                        leadingContent = {
                            Icon(Icons.Rounded.PlaylistPlay, contentDescription = null)
                        },
                        modifier = Modifier.clickable(onClick = { onPlaylistSelected(playlist.id) })
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

private enum class SortOrder {
    TITLE, ARTIST, ALBUM, DURATION, DATE_ADDED
}
