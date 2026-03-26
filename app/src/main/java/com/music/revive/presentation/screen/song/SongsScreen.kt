package com.music.revive.presentation.screen.song

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.music.revive.R
import com.music.revive.domain.model.Song
import com.music.revive.presentation.components.CreatePlaylistDialog
import kotlinx.coroutines.delay
import kotlin.math.abs

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
    onCreatePlaylist: (String) -> Unit,
    onSearchClick: () -> Unit = {},
    onRefresh: () -> Unit = {}
) {
    var showCreatePlaylistDialog by remember { mutableStateOf(false) }
    var songToAddToPlaylist by remember { mutableStateOf<Long?>(null) }
    var expandedSongMenu by remember { mutableStateOf<Long?>(null) }
    var sortOrder by remember { mutableStateOf(SortOrder.TITLE) }
    var showSortMenu by remember { mutableStateOf(false) }
    
    // Search functionality
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    
    val listState = rememberLazyListState()
    
    // Sort and filter songs
    val sortedSongs = remember(songs, sortOrder, searchQuery, isSearching) {
        val filtered = if (isSearching && searchQuery.isNotEmpty()) {
            songs.filter { 
                it.title.contains(searchQuery, ignoreCase = true) ||
                it.artist.contains(searchQuery, ignoreCase = true) ||
                it.album.contains(searchQuery, ignoreCase = true)
            }
        } else {
            songs
        }
        
        when (sortOrder) {
            SortOrder.TITLE -> filtered.sortedBy { it.title.lowercase() }
            SortOrder.ARTIST -> filtered.sortedBy { it.artist.lowercase() }
            SortOrder.ALBUM -> filtered.sortedBy { it.album.lowercase() }
            SortOrder.DURATION -> filtered.sortedBy { it.duration }
            SortOrder.DATE_ADDED -> filtered.sortedByDescending { it.dateAdded }
        }
    }
    
    // Scroll to top listener
    LaunchedEffect(listState.firstVisibleItemIndex) {
        if (listState.firstVisibleItemIndex > 0) {
            // User scrolled
        }
    }

    Scaffold(
        topBar = {
            AnimatedVisibility(
                visible = !isSearching,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                DefaultTopBar(
                    songsCount = songs.size,
                    onSearchClick = { isSearching = true },
                    onRefresh = onRefresh,
                    onShuffleClick = {
                        if (songs.isNotEmpty()) {
                            onSongClick(songs.random(), songs)
                        }
                    },
                    onSortClick = { showSortMenu = true },
                    sortOrder = sortOrder
                )
            }
            
            AnimatedVisibility(
                visible = isSearching,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                SearchTopBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    onBackClick = { 
                        isSearching = false
                        searchQuery = ""
                    },
                    songsCount = sortedSongs.size
                )
            }
            
            // Sort dropdown menu
            DropdownMenu(
                expanded = showSortMenu,
                onDismissRequest = { showSortMenu = false }
            ) {
                Text(
                    text = stringResource(R.string.sort),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary
                )
                SortOrder.entries.forEach { order ->
                    DropdownMenuItem(
                        text = { Text(getSortOrderName(order)) },
                        onClick = {
                            sortOrder = order
                            showSortMenu = false
                        },
                        leadingIcon = {
                            if (sortOrder == order) {
                                Icon(Icons.Rounded.Check, contentDescription = null)
                            }
                        }
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (sortedSongs.isEmpty()) {
                AnimatedEmptyState(
                    isSearching = isSearching,
                    searchQuery = searchQuery
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    itemsIndexed(
                        items = sortedSongs,
                        key = { _, song -> song.id }
                    ) { index, song ->
                        val isFavorite = favoriteSongIds.contains(song.id)
                        
                        AnimatedSongListItem(
                            song = song,
                            isFavorite = isFavorite,
                            index = index,
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
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.go_to_album)) },
                                onClick = { expandedSongMenu = null },
                                leadingIcon = {
                                    Icon(Icons.Rounded.Album, contentDescription = null)
                                }
                            )
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.go_to_artist)) },
                                onClick = { expandedSongMenu = null },
                                leadingIcon = {
                                    Icon(Icons.Rounded.Person, contentDescription = null)
                                }
                            )
                            Divider()
                            DropdownMenuItem(
                                text = { Text(stringResource(R.string.remove)) },
                                onClick = { expandedSongMenu = null },
                                leadingIcon = {
                                    Icon(Icons.Rounded.Delete, contentDescription = null)
                                }
                            )
                        }
                    }
                    
                    // Bottom spacer
                    item {
                        Spacer(modifier = Modifier.height(100.dp))
                    }
                }
            }
            
            // Fast scroll indicator
            if (sortedSongs.size > 20) {
                FastScrollIndicator(
                    listState = listState,
                    totalCount = sortedSongs.size
                )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DefaultTopBar(
    songsCount: Int,
    onSearchClick: () -> Unit,
    onRefresh: () -> Unit,
    onShuffleClick: () -> Unit,
    onSortClick: () -> Unit,
    sortOrder: SortOrder
) {
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
                    text = "$songsCount ${stringResource(R.string.songs)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        actions = {
            IconButton(onClick = onSearchClick) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = stringResource(R.string.search)
                )
            }
            IconButton(onClick = onRefresh) {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = stringResource(R.string.refresh)
                )
            }
            IconButton(onClick = onShuffleClick) {
                Icon(
                    imageVector = Icons.Rounded.Shuffle,
                    contentDescription = stringResource(R.string.shuffle)
                )
            }
            IconButton(onClick = onSortClick) {
                Icon(
                    imageVector = Icons.Rounded.Sort,
                    contentDescription = stringResource(R.string.sort),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent
        )
    )
}

@Composable
private fun SearchTopBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onBackClick: () -> Unit,
    songsCount: Int
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Rounded.ArrowBack,
                    contentDescription = stringResource(R.string.back)
                )
            }
            TextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(stringResource(R.string.search_hint))
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(
                                imageVector = Icons.Rounded.Clear,
                                contentDescription = stringResource(R.string.clear)
                            )
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(28.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "$songsCount",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AnimatedSongListItem(
    song: Song,
    isFavorite: Boolean,
    index: Int,
    onPlayClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    onMenuClick: () -> Unit,
    onLongClick: () -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }
    var isPressed by remember { mutableStateOf(false) }
    
    val smoothEasing = CubicBezierEasing(0.4f, 0.0f, 0.2f, 1.0f)
    
    LaunchedEffect(Unit) {
        delay((index % 10) * 30L)
        isVisible = true
    }
    
    val itemAlpha by animateFloatAsState(
        targetValue = if (isVisible) 1f else 0f,
        animationSpec = tween(300, easing = smoothEasing),
        label = "itemAlpha"
    )
    
    val itemOffset by animateDpAsState(
        targetValue = if (isVisible) 0.dp else 32.dp,
        animationSpec = tween(300, easing = smoothEasing),
        label = "itemOffset"
    )
    
    val itemScale by animateFloatAsState(
        targetValue = if (isPressed) 0.98f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "itemScale"
    )

    ListItem(
        headlineContent = {
            Text(
                text = song.title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isFavorite) FontWeight.SemiBold else FontWeight.Normal
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
                    text = "•",
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
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                if (song.albumArtUri != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(song.albumArtUri)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.MusicNote,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
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
                        imageVector = if (isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
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
                onClick = {
                    isPressed = true
                    onPlayClick()
                },
                onLongClick = onLongClick
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .graphicsLayer {
                alpha = itemAlpha
                translationY = itemOffset.toPx()
                scaleX = itemScale
                scaleY = itemScale
            }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        isPressed = true
                        try {
                            awaitRelease()
                        } finally {
                            isPressed = false
                        }
                    }
                )
            }
    )
}

@Composable
private fun AnimatedEmptyState(
    isSearching: Boolean,
    searchQuery: String
) {
    var isVisible by remember { mutableStateOf(false) }
    
    LaunchedEffect(Unit) {
        isVisible = true
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AnimatedVisibility(
            visible = isVisible,
            enter = scaleIn(animationSpec = tween(500)) + fadeIn(animationSpec = tween(500))
        ) {
            Surface(
                modifier = Modifier.size(120.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHighest
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isSearching) Icons.Rounded.Search else Icons.Rounded.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.size(60.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = if (isSearching) {
                "未找到与 \"$searchQuery\" 相关的结果"
            } else {
                stringResource(R.string.no_songs_found)
            },
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (isSearching) {
                "尝试其他关键词"
            } else {
                "设备上未找到音乐文件"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun FastScrollIndicator(
    listState: androidx.compose.foundation.lazy.LazyListState,
    totalCount: Int
) {
    var visible by remember { mutableStateOf(false) }
    val scrollProgress by remember {
        derivedStateOf {
            val layoutInfo = listState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            val visibleItemsInfo = layoutInfo.visibleItemsInfo
            
            if (totalItems == 0) return@derivedStateOf 0f
            
            val firstVisibleItem = listState.firstVisibleItemIndex
            val scrollOffset = listState.firstVisibleItemScrollOffset
            
            (firstVisibleItem + scrollOffset.toFloat() / maxOf(1, layoutInfo.viewportEndOffset)) / totalItems.toFloat()
        }
    }
    
    LaunchedEffect(listState.isScrollInProgress) {
        if (listState.isScrollInProgress) {
            visible = true
        } else {
            delay(2000)
            visible = false
        }
    }
    
    Box(
        modifier = Modifier
            .fillMaxHeight()
            .padding(end = 4.dp)
            .width(24.dp),
        contentAlignment = Alignment.CenterEnd
    ) {
        AnimatedVisibility(
            visible = visible,
            enter = fadeIn(animationSpec = tween(150)),
            exit = fadeOut(animationSpec = tween(150))
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(4.dp),
                shape = RoundedCornerShape(2.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight(scrollProgress)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.tertiary
                                )
                            )
                        )
                        .clip(RoundedCornerShape(2.dp))
                )
            }
        }
    }
}

@Composable
private fun getSortOrderName(sortOrder: SortOrder): String {
    return when (sortOrder) {
        SortOrder.TITLE -> stringResource(R.string.sort_by_title)
        SortOrder.ARTIST -> stringResource(R.string.sort_by_artist)
        SortOrder.ALBUM -> stringResource(R.string.sort_by_album)
        SortOrder.DURATION -> stringResource(R.string.sort_by_duration)
        SortOrder.DATE_ADDED -> stringResource(R.string.sort_by_date_added)
    }
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
                items(playlists.size) { index ->
                    val playlist = playlists[index]
                    ListItem(
                        headlineContent = { Text(playlist.name) },
                        supportingContent = { 
                            Text("${playlist.songCount} ${stringResource(R.string.songs)}")
                        },
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