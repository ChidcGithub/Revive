package com.music.revive.presentation.navigation

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
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
import com.music.revive.presentation.screen.song.SongDetailScreen
import com.music.revive.service.MusicPlayer
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.reflect.KClass

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviveNavigation(
    navController: NavHostController = rememberNavController(),
    musicPlayer: MusicPlayer = hiltViewModel<PlayerViewModel>().musicPlayer
) {
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
        bottomBar = {
            Column {
                if (playerState.currentSong != null) {
                    BottomPlayerBar(
                        playerState = playerState,
                        onPlayPauseClick = { musicPlayer.playPause() },
                        onNextClick = { musicPlayer.playNext() },
                        onPreviousClick = { musicPlayer.playPrevious() },
                        onBarClick = { navController.navigate(Player) }
                    )
                }

                if (isBottomBarVisible) {
                    NavigationBar {
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
                            icon = { Icon(Icons.Default.Home, contentDescription = null) },
                            label = { Text(stringResource(R.string.home)) }
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
                            icon = { Icon(Icons.Default.PlaylistPlay, contentDescription = null) },
                            label = { Text(stringResource(R.string.playlists)) }
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
                            icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                            label = { Text(stringResource(R.string.settings)) }
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
            enterTransition = { slideInHorizontally(initialOffsetX = { it }) },
            exitTransition = { slideOutHorizontally(targetOffsetX = { -it }) },
            popEnterTransition = { slideInHorizontally(initialOffsetX = { -it }) },
            popExitTransition = { slideOutHorizontally(targetOffsetX = { it }) }
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
                    onQueueClick = { navController.navigate(Queue) }
                )
            }

            composable<Queue> {
                // Queue screen
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