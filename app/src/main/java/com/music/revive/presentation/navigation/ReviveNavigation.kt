package com.music.revive.presentation.navigation

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.music.revive.R
import com.music.revive.domain.model.Album
import com.music.revive.domain.model.Artist
import com.music.revive.domain.model.Song
import com.music.revive.presentation.components.BottomPlayerBar
import com.music.revive.presentation.screen.album.AlbumDetailScreen
import com.music.revive.presentation.screen.album.AlbumDetailViewModel
import com.music.revive.presentation.screen.artist.ArtistDetailScreen
import com.music.revive.presentation.screen.artist.ArtistDetailViewModel
import com.music.revive.presentation.screen.home.HomeScreen
import com.music.revive.presentation.screen.home.HomeViewModel
import com.music.revive.presentation.screen.player.PlayerScreen
import com.music.revive.presentation.screen.player.PlayerViewModel
import com.music.revive.presentation.screen.player.QueueScreen
import com.music.revive.presentation.screen.playlist.PlaylistScreen
import com.music.revive.presentation.screen.playlist.PlaylistViewModel
import com.music.revive.presentation.screen.search.SearchScreen
import com.music.revive.presentation.screen.search.SearchViewModel
import com.music.revive.presentation.screen.settings.SettingsScreen
import com.music.revive.presentation.screen.settings.SettingsViewModel
import com.music.revive.presentation.screen.song.SongDetailScreen
import com.music.revive.service.MusicPlayer
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SharedMusicViewModel @Inject constructor(
    val musicPlayer: MusicPlayer
) : androidx.lifecycle.ViewModel()

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviveNavigation(
    navController: NavHostController = rememberNavController(),
    viewModel: SharedMusicViewModel = hiltViewModel()
) {
    val musicPlayer = viewModel.musicPlayer
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route ?: ""

    val playerState by musicPlayer.playerState.collectAsState()
    val isPlaying by musicPlayer.isPlaying.collectAsState()

    val homeViewModel: HomeViewModel = hiltViewModel()
    val homeUiState by homeViewModel.uiState.collectAsState()

    val bottomNavRoutes = listOf(
        Home::class.qualifiedName ?: "Home",
        Playlists::class.qualifiedName ?: "Playlists",
        Settings::class.qualifiedName ?: "Settings"
    )

    val isBottomBarVisible = currentRoute.contains("Home") ||
            currentRoute.contains("Playlists") ||
            currentRoute.contains("Settings")

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Column {
                // Bottom Player Bar
                AnimatedVisibility(
                    visible = playerState.currentSong != null,
                    enter = fadeIn() + slideInVertically { it },
                    exit = fadeOut() + slideOutVertically { it }
                ) {
                    BottomPlayerBar(
                        playerState = playerState,
                        onPlayPauseClick = { musicPlayer.playPause() },
                        onNextClick = { musicPlayer.playNext() },
                        onPreviousClick = { musicPlayer.playPrevious() },
                        onBarClick = { navController.navigate(Player) }
                    )
                }

                // Navigation Bar
                AnimatedVisibility(
                    visible = isBottomBarVisible,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        tonalElevation = 0.dp
                    ) {
                        NavigationBarItem(
                            selected = currentRoute.contains("Home"),
                            onClick = {
                                if (!currentRoute.contains("Home")) {
                                    navController.navigate(Home) {
                                        popUpTo(Home) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (currentRoute.contains("Home")) Icons.Rounded.Home else Icons.Outlined.Home,
                                    contentDescription = null
                                )
                            },
                            label = {
                                Text(
                                    text = stringResource(R.string.home),
                                    fontWeight = if (currentRoute.contains("Home")) FontWeight.Medium else FontWeight.Normal
                                )
                            }
                        )
                        NavigationBarItem(
                            selected = currentRoute.contains("Playlists"),
                            onClick = {
                                if (!currentRoute.contains("Playlists")) {
                                    navController.navigate(Playlists) {
                                        popUpTo(Home) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (currentRoute.contains("Playlists")) Icons.Rounded.PlaylistPlay else Icons.Outlined.PlaylistPlay,
                                    contentDescription = null
                                )
                            },
                            label = {
                                Text(
                                    text = stringResource(R.string.playlists),
                                    fontWeight = if (currentRoute.contains("Playlists")) FontWeight.Medium else FontWeight.Normal
                                )
                            }
                        )
                        NavigationBarItem(
                            selected = currentRoute.contains("Settings"),
                            onClick = {
                                if (!currentRoute.contains("Settings")) {
                                    navController.navigate(Settings) {
                                        popUpTo(Home) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (currentRoute.contains("Settings")) Icons.Rounded.Settings else Icons.Outlined.Settings,
                                    contentDescription = null
                                )
                            },
                            label = {
                                Text(
                                    text = stringResource(R.string.settings),
                                    fontWeight = if (currentRoute.contains("Settings")) FontWeight.Medium else FontWeight.Normal
                                )
                            }
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Home,
            modifier = Modifier.padding(paddingValues),
            enterTransition = { slideInHorizontally(initialOffsetX = { it }) + fadeIn() },
            exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) + fadeOut() },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) + fadeIn() },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) + fadeOut() }
        ) {
            composable<Home> {
                HomeScreen(
                    uiState = homeUiState,
                    onSongClick = { song, songList ->
                        musicPlayer.playSong(song, songList)
                        navController.navigate(Player)
                    },
                    onAlbumClick = { album ->
                        navController.navigate(AlbumDetail(album.id))
                    },
                    onArtistClick = { artist ->
                        navController.navigate(ArtistDetail(artist.id))
                    },
                    onFolderClick = { folderPath ->
                        navController.navigate(FolderDetail(folderPath))
                    },
                    onPlaylistClick = { playlistId ->
                        navController.navigate(PlaylistDetail(playlistId))
                    },
                    onSearchClick = {
                        navController.navigate(Search)
                    },
                    onFavoriteClick = { songId ->
                        homeViewModel.toggleFavorite(songId)
                    },
                    onCreatePlaylist = { name ->
                        homeViewModel.createPlaylist(name)
                    },
                    onAddToPlaylist = { playlistId, songId ->
                        homeViewModel.addToPlaylist(playlistId, songId)
                    },
                    onRefresh = {
                        homeViewModel.refresh()
                    },
                    onSongDetailClick = { songId ->
                        navController.navigate(com.music.revive.presentation.navigation.SongDetail(songId))
                    }
                )
            }

            composable<Playlists> {
                PlaylistScreen(
                    onPlaylistClick = { playlistId ->
                        navController.navigate(PlaylistDetail(playlistId))
                    },
                    onSongClick = { song, songList ->
                        musicPlayer.playSong(song, songList)
                        navController.navigate(Player)
                    },
                    onFavoriteClick = { songId -> }
                )
            }

            composable<Settings> {
                SettingsScreen()
            }

            composable<Search> {
                SearchScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onSongClick = { song, songList ->
                        musicPlayer.playSong(song, songList)
                        navController.navigate(Player)
                    },
                    onFavoriteClick = { songId -> }
                )
            }

            composable<Player> {
                PlayerScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onQueueClick = { navController.navigate(Queue) },
                    onSongDetailClick = { songId ->
                        navController.navigate(SongDetail(songId))
                    },
                    onAlbumClick = { albumId ->
                        albumId?.let { navController.navigate(AlbumDetail(it)) }
                    },
                    onArtistClick = { artistId ->
                        artistId?.let { navController.navigate(ArtistDetail(it)) }
                    }
                )
            }

            composable<Queue> {
                QueueScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable<AlbumDetail> {
                val viewModel: AlbumDetailViewModel = hiltViewModel()
                AlbumDetailScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onSongClick = { song, songList ->
                        musicPlayer.playSong(song, songList)
                        navController.navigate(Player)
                    }
                )
            }

            composable<ArtistDetail> {
                val viewModel: ArtistDetailViewModel = hiltViewModel()
                ArtistDetailScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onSongClick = { song, songList ->
                        musicPlayer.playSong(song, songList)
                        navController.navigate(Player)
                    }
                )
            }

            composable<FolderDetail> {
                // Folder detail screen
            }

            composable<PlaylistDetail> {
                // Playlist detail screen
            }

            composable<Favorites> {
                // Favorites screen
            }

            composable<Recent> {
                // Recent screen
            }

            composable<SongDetail> {
                SongDetailScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onPlaySong = { song, songList ->
                        musicPlayer.playSong(song, songList)
                        navController.navigate(Player)
                    },
                    onAlbumClick = { albumId ->
                        navController.navigate(AlbumDetail(albumId))
                    },
                    onArtistClick = { artistId ->
                        navController.navigate(ArtistDetail(artistId))
                    }
                )
            }
        }
    }
}
