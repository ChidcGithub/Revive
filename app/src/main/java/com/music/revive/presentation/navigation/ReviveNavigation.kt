package com.music.revive.presentation.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackAsState
import androidx.navigation.compose.rememberNavController
import com.music.revive.R
import com.music.revive.data.repository.MusicRepository
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
import com.music.revive.presentation.screen.playlist.PlaylistScreen
import com.music.revive.presentation.screen.playlist.PlaylistViewModel
import com.music.revive.presentation.screen.search.SearchScreen
import com.music.revive.presentation.screen.search.SearchViewModel
import com.music.revive.presentation.screen.settings.SettingsScreen
import com.music.revive.presentation.screen.settings.SettingsViewModel
import com.music.revive.service.MusicPlayer
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviveNavigation(
    navController: NavHostController = rememberNavController(),
    musicPlayer: MusicPlayer
) {
    val navBackStackEntry by navController.currentBackStackAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val playerState by musicPlayer.playerState.collectAsState()
    val isPlaying by musicPlayer.isPlaying.collectAsState()

    val homeViewModel: HomeViewModel = hiltViewModel()
    val homeUiState by homeViewModel.uiState.collectAsState()

    // Define bottom navigation items
    val bottomNavItems = listOf(
        BottomNavItem(
            route = Screen.Home::class.qualifiedName ?: "home",
            icon = Icons.Default.Home,
            labelRes = R.string.home
        ),
        BottomNavItem(
            route = Screen.Playlists::class.qualifiedName ?: "playlists",
            icon = Icons.Default.PlaylistPlay,
            labelRes = R.string.playlists
        ),
        BottomNavItem(
            route = Screen.Settings::class.qualifiedName ?: "settings",
            icon = Icons.Default.Settings,
            labelRes = R.string.settings
        )
    )

    Scaffold(
        bottomBar = {
            Column {
                // Bottom player bar
                if (playerState.currentSong != null) {
                    BottomPlayerBar(
                        playerState = playerState,
                        onPlayPauseClick = { musicPlayer.playPause() },
                        onNextClick = { musicPlayer.playNext() },
                        onPreviousClick = { musicPlayer.playPrevious() },
                        onBarClick = { navController.navigate(Screen.Player) }
                    )
                }

                // Bottom navigation
                if (currentRoute in bottomNavItems.map { it.route }) {
                    NavigationBar {
                        bottomNavItems.forEach { item ->
                            NavigationBarItem(
                                selected = currentRoute == item.route,
                                onClick = {
                                    if (currentRoute != item.route) {
                                        navController.navigate(item.route) {
                                            popUpTo(Screen.Home::class.qualifiedName ?: "home") {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                },
                                icon = {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = stringResource(item.labelRes)
                                    )
                                },
                                label = { Text(stringResource(item.labelRes)) }
                            )
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = Screen.Home::class.qualifiedName ?: "home",
            modifier = Modifier.padding(paddingValues),
            enterTransition = { slideInHorizontally(initialOffsetX = { it }) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) }
        ) {
            // Home screen
            composable(route = Screen.Home::class.qualifiedName ?: "home") {
                HomeScreen(
                    uiState = homeUiState,
                    onSongClick = { song, songList ->
                        musicPlayer.playSong(song, songList)
                        navController.navigate(Screen.Player)
                    },
                    onAlbumClick = { album ->
                        // Navigate to album detail
                    },
                    onArtistClick = { artist ->
                        // Navigate to artist detail
                    },
                    onFolderClick = { folderPath ->
                        // Navigate to folder detail
                    },
                    onPlaylistClick = { playlistId ->
                        // Navigate to playlist detail
                    },
                    onSearchClick = {
                        navController.navigate(Screen.Search::class.qualifiedName ?: "search")
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
                    }
                )
            }

            // Playlists screen
            composable(route = Screen.Playlists::class.qualifiedName ?: "playlists") {
                val playlistViewModel: PlaylistViewModel = hiltViewModel()
                PlaylistScreen(
                    onPlaylistClick = { playlistId ->
                        // Navigate to playlist detail
                    },
                    onSongClick = { song, songList ->
                        musicPlayer.playSong(song, songList)
                        navController.navigate(Screen.Player)
                    },
                    onFavoriteClick = { songId ->
                        // Toggle favorite
                    }
                )
            }

            // Settings screen
            composable(route = Screen.Settings::class.qualifiedName ?: "settings") {
                val settingsViewModel: SettingsViewModel = hiltViewModel()
                SettingsScreen()
            }

            // Search screen
            composable(route = Screen.Search::class.qualifiedName ?: "search") {
                val searchViewModel: SearchViewModel = hiltViewModel()
                SearchScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onSongClick = { song, songList ->
                        musicPlayer.playSong(song, songList)
                        navController.navigate(Screen.Player)
                    },
                    onFavoriteClick = { songId ->
                        // Toggle favorite
                    }
                )
            }

            // Player screen
            composable(route = Screen.Player::class.qualifiedName ?: "player") {
                val playerViewModel: PlayerViewModel = hiltViewModel()
                PlayerScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onQueueClick = { navController.navigate(Screen.Queue::class.qualifiedName ?: "queue") }
                )
            }

            // Queue screen
            composable(route = Screen.Queue::class.qualifiedName ?: "queue") {
                // Queue screen implementation
            }
        }
    }
}

data class BottomNavItem(
    val route: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val labelRes: Int
)
