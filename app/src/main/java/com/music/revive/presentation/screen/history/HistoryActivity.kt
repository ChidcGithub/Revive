package com.music.revive.presentation.screen.history

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.ClearAll
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.rememberImagePainter
import com.music.revive.data.local.HistoryItem
import com.music.revive.data.local.HistoryPreferences
import com.music.revive.presentation.theme.ReviveTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class HistoryActivity : ComponentActivity() {
    
    @Inject lateinit var historyPreferences: HistoryPreferences
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        setContent {
            ReviveTheme {
                HistoryScreen(
                    viewModel = HistoryViewModel(historyPreferences),
                    onNavigateBack = { finish() }
                )
            }
        }
    }
    
    companion object {
        fun newIntent(context: Context): android.content.Intent {
            return android.content.Intent(context, HistoryActivity::class.java)
        }
    }
}

class HistoryViewModel(
    private val historyPreferences: HistoryPreferences
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()
    
    init {
        loadHistory()
    }
    
    private fun loadHistory() {
        viewModelScope.launch {
            historyPreferences.historyItems.collect { items ->
                _uiState.value = _uiState.value.copy(
                    historyItems = items,
                    isLoading = false
                )
            }
        }
    }
    
    fun clearAllHistory() {
        viewModelScope.launch {
            historyPreferences.clearAllHistory()
        }
    }
    
    fun removeFromHistory(songId: Long) {
        viewModelScope.launch {
            historyPreferences.removeFromHistory(songId)
        }
    }
    
    fun setMaxHistorySize(size: Int) {
        viewModelScope.launch {
            historyPreferences.setMaxHistorySize(size)
        }
    }
}

data class HistoryUiState(
    val historyItems: List<HistoryItem> = emptyList(),
    val isLoading: Boolean = true,
    val showClearConfirm: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text("听歌历史") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                actions = {
                    if (uiState.historyItems.isNotEmpty()) {
                        IconButton(onClick = { 
                            viewModel.clearAllHistory()
                        }) {
                            Icon(
                                imageVector = Icons.Rounded.ClearAll,
                                contentDescription = "清除全部",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (uiState.isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (uiState.historyItems.isEmpty()) {
                // Empty state
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "暂无听歌记录",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "开始听音乐后，这里会显示你的听歌历史",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                        maxLines = 2
                    )
                }
            } else {
                // History list
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(
                        items = uiState.historyItems,
                        key = { it.songId }
                    ) { item ->
                        HistoryListItem(
                            item = item,
                            onRemove = { viewModel.removeFromHistory(item.songId) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Footer with count
                        Text(
                            text = "共 ${uiState.historyItems.size} 首歌曲",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HistoryListItem(
    item: HistoryItem,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Album art or placeholder using Coil
            if (!item.albumArtUri.isNullOrEmpty()) {
                val painter = rememberImagePainter(
                    data = item.albumArtUri,
                    builder = {
                        crossfade(true)
                        size(128)
                    }
                )
                
                androidx.compose.foundation.Image(
                    painter = painter,
                    contentDescription = null,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(MaterialTheme.shapes.small)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(MaterialTheme.shapes.small)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.size(24.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            
            // Song info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.artist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    
                    Text(
                        text = "•",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    
                    Text(
                        text = item.getPlayCountDisplay(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1
                    )
                }
                
                Text(
                    text = item.getLastPlayedRelativeTime(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    maxLines = 1
                )
            }
            
            // Remove button
            IconButton(
                onClick = onRemove,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Delete,
                    contentDescription = "移除",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
    }
}
