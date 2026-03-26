package com.music.revive.presentation.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
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
import com.music.revive.presentation.screen.playlist.FavoritesScreen
import com.music.revive.presentation.screen.playlist.PlaylistDetailScreen
import com.music.revive.presentation.screen.playlist.PlaylistScreen
import com.music.revive.presentation.screen.playlist.PlaylistViewModel
import com.music.revive.presentation.screen.playlist.RecentScreen
import com.music.revive.presentation.screen.search.SearchScreen
import com.music.revive.presentation.screen.search.SearchViewModel
import com.music.revive.presentation.screen.settings.SettingsScreen
import com.music.revive.presentation.screen.settings.SettingsViewModel
import com.music.revive.presentation.screen.song.SongDetailScreen
import com.music.revive.service.MusicPlayer
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

// Custom easing curves for smoother animations
private val SmoothEasing = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)
private val DecelerateEasing = CubicBezierEasing(0.0f, 0.0f, 0.2f, 1.0f)
private val AccelerateEasing = CubicBezierEasing(0.4f, 0.0f, 1.0f, 1.0f)
private val EmphasizedEasing = CubicBezierEasing(0.2f, 0.0f, 0.0f, 1.0f)

// Spring animation specs
private val DefaultSpringSpec = spring<Float>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessLow
)

private val BouncySpringSpec = spring<Float>(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness = Spring.StiffnessMedium
)

// Transition animations for different screen types

// Standard horizontal slide transition (for most screens)
private fun standardEnterTransition(): EnterTransition {
    return slideInHorizontally(
        initialOffsetX = { it },
        animationSpec = tween(350, easing = DecelerateEasing)
    ) + fadeIn(
        animationSpec = tween(200, easing = SmoothEasing)
    )
}

private fun standardExitTransition(): ExitTransition {
    return slideOutHorizontally(
        targetOffsetX = { -it / 3 },
        animationSpec = tween(350, easing = AccelerateEasing)
    ) + fadeOut(
        animationSpec = tween(200, easing = SmoothEasing)
    )
}

private fun standardPopEnterTransition(): EnterTransition {
    return slideInHorizontally(
        initialOffsetX = { -it / 3 },
        animationSpec = tween(350, easing = DecelerateEasing)
    ) + fadeIn(
        animationSpec = tween(200, easing = SmoothEasing)
    )
}

private fun standardPopExitTransition(): ExitTransition {
    return slideOutHorizontally(
        targetOffsetX = { it },
        animationSpec = tween(350, easing = AccelerateEasing)
    ) + fadeOut(
        animationSpec = tween(200, easing = SmoothEasing)
    )
}

// Player screen uses slide up from bottom with smooth animation
private fun playerEnterTransition(): EnterTransition {
    return slideInVertically(
        initialOffsetY = { it },
        animationSpec = tween(400, easing = DecelerateEasing)
    ) + fadeIn(
        animationSpec = tween(300, easing = SmoothEasing)
    )
}

private fun playerExitTransition(): ExitTransition {
    return slideOutVertically(
        targetOffsetY = { it },
        animationSpec = tween(350, easing = AccelerateEasing)
    ) + fadeOut(
        animationSpec = tween(200, easing = SmoothEasing)
    )
}

// Queue screen slides up
private fun queueEnterTransition(): EnterTransition {
    return slideInVertically(
        initialOffsetY = { it },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMedium
        )
    ) + fadeIn(
        animationSpec = tween(250, easing = SmoothEasing)
    )
}

private fun queueExitTransition(): ExitTransition {
    return slideOutVertically(
        targetOffsetY = { it },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        )
    ) + fadeOut(
        animationSpec = tween(150, easing = SmoothEasing)
    )
}

// Modal/bottom sheet style transitions for detail screens
private fun detailEnterTransition(): EnterTransition {
    return slideInHorizontally(
        initialOffsetX = { it / 2 },
        animationSpec = tween(400, easing = EmphasizedEasing)
    ) + fadeIn(
        animationSpec = tween(300, easing = SmoothEasing)
    ) + scaleIn(
        initialScale = 0.95f,
        animationSpec = tween(400, easing = EmphasizedEasing)
    )
}

private fun detailExitTransition(): ExitTransition {
    return slideOutHorizontally(
        targetOffsetX = { it / 2 },
        animationSpec = tween(350, easing = AccelerateEasing)
    ) + fadeOut(
        animationSpec = tween(200, easing = SmoothEasing)
    ) + scaleOut(
        targetScale = 0.95f,
        animationSpec = tween(350, easing = AccelerateEasing)
    )
}

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
        Songs::class.qualifiedName ?: "Songs",
        Playlists::class.qualifiedName ?: "Playlists",
        Settings::class.qualifiedName ?: "Settings"
    )

    // Hide bottom bar on full-screen pages like Player and Queue
    val isFullScreenPage = currentRoute.contains("Player") || 
            currentRoute.contains("Queue") ||
            currentRoute.contains("SongDetail")

    val isBottomBarVisible = (currentRoute.contains("Home") ||
            currentRoute.contains("Songs") ||
            currentRoute.contains("Playlists") ||
            currentRoute.contains("Settings")) && !isFullScreenPage

    // Show bottom player bar only when there's a song and not on full-screen pages
    val showBottomPlayerBar = playerState.currentSong != null && !isFullScreenPage

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            Column {
                // Bottom Player Bar with smooth entry animation
                AnimatedVisibility(
                    visible = showBottomPlayerBar,
                    enter = fadeIn(
                        animationSpec = tween(300, easing = SmoothEasing)
                    ) + slideInVertically(
                        initialOffsetY = { it },
                        animationSpec = tween(350, easing = DecelerateEasing)
                    ),
                    exit = fadeOut(
                        animationSpec = tween(200, easing = SmoothEasing)
                    ) + slideOutVertically(
                        targetOffsetY = { it },
                        animationSpec = tween(250, easing = AccelerateEasing)
                    )
                ) {
                    BottomPlayerBar(
                        playerState = playerState,
                        onPlayPauseClick = { musicPlayer.playPause() },
                        onNextClick = { musicPlayer.playNext() },
                        onPreviousClick = { musicPlayer.playPrevious() },
                        onBarClick = { navController.navigate(Player) }
                    )
                }

                // Navigation Bar with blur effect
                AnimatedVisibility(
                    visible = isBottomBarVisible,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .blur(4.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.9f)
                    ) {
                        Box(modifier = Modifier.fillMaxWidth())
                    }
                    NavigationBar(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerLow.copy(alpha = 0.85f),
                        tonalElevation = 2.dp
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
                            selected = currentRoute.contains("Songs"),
                            onClick = {
                                if (!currentRoute.contains("Songs")) {
                                    navController.navigate(Songs) {
                                        popUpTo(Home) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = if (currentRoute.contains("Songs")) Icons.Rounded.MusicNote else Icons.Outlined.MusicNote,
                                    contentDescription = null
                                )
                            },
                            label = {
                                Text(
                                    text = stringResource(R.string.songs),
                                    fontWeight = if (currentRoute.contains("Songs")) FontWeight.Medium else FontWeight.Normal
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
        // Determine if current page should use padding (non-fullscreen pages)
        val contentPadding = if (isFullScreenPage) PaddingValues(0.dp) else paddingValues
        
        NavHost(
            navController = navController,
            startDestination = Home,
            modifier = Modifier.padding(contentPadding),
            enterTransition = { standardEnterTransition() },
            exitTransition = { standardExitTransition() },
            popEnterTransition = { standardPopEnterTransition() },
            popExitTransition = { standardPopExitTransition() }
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
                    },
                    onNavigateToSongs = {
                        navController.navigate(Songs)
                    }
                )
            }

            composable<Songs> {
                com.music.revive.presentation.screen.song.SongsScreen(
                    songs = homeUiState.songs,
                    favoriteSongIds = homeUiState.favoriteSongIds,
                    playlists = homeUiState.playlists,
                    onSongClick = { song, songList ->
                        musicPlayer.playSong(song, songList)
                        navController.navigate(Player)
                    },
                    onFavoriteClick = { songId ->
                        homeViewModel.toggleFavorite(songId)
                    },
                    onSongDetailClick = { songId ->
                        navController.navigate(com.music.revive.presentation.navigation.SongDetail(songId))
                    },
                    onAddToPlaylist = { playlistId, songId ->
                        homeViewModel.addToPlaylist(playlistId, songId)
                    },
                    onCreatePlaylist = { name ->
                        homeViewModel.createPlaylist(name)
                    }
                )
            }

            composable<Playlists> {
                PlaylistScreen(
                    onPlaylistClick = { playlistId ->
                        navController.navigate(PlaylistDetail(playlistId))
                    },
                    onFavoritesClick = {
                        navController.navigate(Favorites)
                    },
                    onRecentClick = {
                        navController.navigate(Recent)
                    }
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

            composable<Player>(
                enterTransition = { playerEnterTransition() },
                exitTransition = { playerExitTransition() },
                popEnterTransition = { playerEnterTransition() },
                popExitTransition = { playerExitTransition() }
            ) {
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

            composable<Queue>(
                enterTransition = { queueEnterTransition() },
                exitTransition = { queueExitTransition() },
                popEnterTransition = { queueEnterTransition() },
                popExitTransition = { queueExitTransition() }
            ) {
                QueueScreen(
                    onNavigateBack = { navController.popBackStack() }
                )
            }

            composable<AlbumDetail>(
                enterTransition = { detailEnterTransition() },
                exitTransition = { standardExitTransition() },
                popEnterTransition = { standardPopEnterTransition() },
                popExitTransition = { detailExitTransition() }
            ) {
                val viewModel: AlbumDetailViewModel = hiltViewModel()
                AlbumDetailScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onSongClick = { song, songList ->
                        musicPlayer.playSong(song, songList)
                        navController.navigate(Player)
                    }
                )
            }

            composable<ArtistDetail>(
                enterTransition = { detailEnterTransition() },
                exitTransition = { standardExitTransition() },
                popEnterTransition = { standardPopEnterTransition() },
                popExitTransition = { detailExitTransition() }
            ) {
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
                val args = it.toRoute<PlaylistDetail>()
                PlaylistDetailScreen(
                    playlistId = args.playlistId,
                    onNavigateBack = { navController.popBackStack() },
                    onSongClick = { song, songList ->
                        musicPlayer.playSong(song, songList)
                        navController.navigate(Player)
                    }
                )
            }

            composable<Favorites> {
                FavoritesScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onSongClick = { song, songList ->
                        musicPlayer.playSong(song, songList)
                        navController.navigate(Player)
                    }
                )
            }

            composable<Recent> {
                RecentScreen(
                    onNavigateBack = { navController.popBackStack() },
                    onSongClick = { song, songList ->
                        musicPlayer.playSong(song, songList)
                        navController.navigate(Player)
                    }
                )
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
