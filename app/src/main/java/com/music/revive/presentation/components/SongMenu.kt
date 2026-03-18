package com.music.revive.presentation.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.music.revive.R

@Composable
fun SongMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    isFavorite: Boolean,
    onFavoriteClick: () -> Unit,
    onAddToPlaylistClick: () -> Unit,
    onAddToQueueClick: () -> Unit,
    onGoToAlbumClick: () -> Unit = {},
    onGoToArtistClick: () -> Unit = {},
    onShareClick: () -> Unit = {},
    onDeleteClick: () -> Unit = {}
) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismiss
    ) {
        DropdownMenuItem(
            text = { Text(if (isFavorite) stringResource(R.string.remove_from_favorites) else stringResource(R.string.add_to_favorites)) },
            onClick = {
                onFavoriteClick()
                onDismiss()
            },
            leadingIcon = {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = null
                )
            }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.add_to_playlist)) },
            onClick = {
                onAddToPlaylistClick()
                onDismiss()
            },
            leadingIcon = {
                Icon(imageVector = Icons.Default.PlaylistAdd, contentDescription = null)
            }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.add_to_queue)) },
            onClick = {
                onAddToQueueClick()
                onDismiss()
            },
            leadingIcon = {
                Icon(imageVector = Icons.Default.AddToQueue, contentDescription = null)
            }
        )
        HorizontalDivider()
        DropdownMenuItem(
            text = { Text(stringResource(R.string.go_to_album)) },
            onClick = {
                onGoToAlbumClick()
                onDismiss()
            },
            leadingIcon = {
                Icon(imageVector = Icons.Default.Album, contentDescription = null)
            }
        )
        DropdownMenuItem(
            text = { Text(stringResource(R.string.go_to_artist)) },
            onClick = {
                onGoToArtistClick()
                onDismiss()
            },
            leadingIcon = {
                Icon(imageVector = Icons.Default.Person, contentDescription = null)
            }
        )
        HorizontalDivider()
        DropdownMenuItem(
            text = { Text(stringResource(R.string.share)) },
            onClick = {
                onShareClick()
                onDismiss()
            },
            leadingIcon = {
                Icon(imageVector = Icons.Default.Share, contentDescription = null)
            }
        )
    }
}
