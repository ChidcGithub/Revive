package com.music.revive.presentation.screen.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.music.revive.R
import com.music.revive.domain.model.Album
import com.music.revive.domain.model.Artist
import com.music.revive.domain.model.Song
import com.music.revive.presentation.components.*

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onSongClick: (Song, List<Song>) -> Unit,
    onAlbumClick: (Album) -> Unit,
    onArtistClick: (Artist) -> Unit,
    onFolderClick: (String) -> Unit,
    onPlaylistClick: (Long) -> Unit,
    onSearchClick: () -> Unit,
    onFavoriteClick: (Long) -> Unit,
    onCreatePlaylist: (String) -> Unit,
    onAddToPlaylist: (Long, Long) -> Unit,
    onRefresh: () -> Unit,
    onSongDetailClick: (Long) -> Unit = {}
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var songToAddToPlaylist by remember { mutableStateOf<Long?>(null) }
    var expandedSongMenu by remember { mutableStateOf<Long?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.app_name)) },
                actions = {
                    IconButton(onClick = onSearchClick) {
                        Icon(Icons.Default.Search, contentDescription = stringResource(R.string.search))
                    }
                    IconButton(onClick = onRefresh) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.refresh))
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            // Recent songs section
            if (uiState.recentSongs.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = stringResource(R.string.recently_played),
                        onSeeAllClick = {}
                    )
                }
                // Recent songs would be shown here
            }

            // Albums section
            if (uiState.albums.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = stringResource(R.string.albums),
                        onSeeAllClick = {}
                    )
                }
                item {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.albums.take(10)) { album ->
                            AlbumItem(
                                album = album,
                                onClick = { onAlbumClick(album) }
                            )
                        }
                    }
                }
            }

            // Artists section
            if (uiState.artists.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = stringResource(R.string.artists),
                        onSeeAllClick = {}
                    )
                }
                items(uiState.artists.take(5)) { artist ->
                    ArtistItem(
                        artist = artist,
                        onClick = { onArtistClick(artist) }
                    )
                }
            }

            // Folders section
            if (uiState.folders.isNotEmpty()) {
                item {
                    SectionHeader(
                        title = stringResource(R.string.folders),
                        onSeeAllClick = {}
                    )
                }
                items(uiState.folders.take(5)) { folder ->
                    FolderItem(
                        folder = folder,
                        onClick = { onFolderClick(folder.path) }
                    )
                }
            }

            // All songs section
            item {
                SectionHeader(
                    title = stringResource(R.string.all_songs),
                    onSeeAllClick = {}
                )
            }

            items(uiState.songs) { song ->
                val isFavorite = uiState.favoriteSongIds.contains(song.id)
                SongItemWithFavorite(
                    song = song,
                    isFavorite = isFavorite,
                    onPlayClick = {
                        onSongClick(song, uiState.songs)
                    },
                    onFavoriteClick = { onFavoriteClick(song.id) },
                    onMenuClick = { expandedSongMenu = song.id }
                )

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
                        leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.add_to_playlist)) },
                        onClick = {
                            songToAddToPlaylist = song.id
                            expandedSongMenu = null
                        },
                        leadingIcon = { Icon(Icons.Default.PlaylistAdd, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.add_to_queue)) },
                        onClick = { expandedSongMenu = null },
                        leadingIcon = { Icon(Icons.Default.AddToQueue, contentDescription = null) }
                    )
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
            playlists = uiState.playlists,
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

@Composable
private fun SectionHeader(
    title: String,
    onSeeAllClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )
        TextButton(onClick = onSeeAllClick) {
            Text(stringResource(R.string.see_all))
        }
    }
}
