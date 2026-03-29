package com.music.revive.presentation.navigation

/**
 * Mini bar and full-screen player must use the same key for shared element matching.
 */
object NowPlayingSharedKeys {
    fun albumArt(songId: Long) = "revive_now_playing_album_$songId"
}
