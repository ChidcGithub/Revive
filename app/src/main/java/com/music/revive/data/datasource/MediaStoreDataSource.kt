package com.music.revive.data.datasource

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.provider.MediaStore.Audio.Media
import com.music.revive.domain.model.Album
import com.music.revive.domain.model.Artist
import com.music.revive.domain.model.Folder
import com.music.revive.domain.model.Song
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaStoreDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val projection = arrayOf(
        Media._ID,
        Media.TITLE,
        Media.ARTIST,
        Media.ALBUM,
        Media.ALBUM_ID,
        Media.ARTIST_ID,
        Media.DURATION,
        Media.DATA,
        Media.DATE_ADDED,
        Media.BITRATE,
        Media.SAMPLE_RATE,
        Media.SIZE
    )

    suspend fun getAllSongs(): List<Song> = withContext(Dispatchers.IO) {
        val songs = mutableListOf<Song>()
        val selection = "${Media.IS_MUSIC} != 0"
        val sortOrder = "${Media.TITLE} ASC"

        context.contentResolver.query(
            Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            sortOrder
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                songs.add(cursor.toSong())
            }
        }
        songs
    }

    suspend fun getSongsByAlbum(albumId: Long): List<Song> = withContext(Dispatchers.IO) {
        val songs = mutableListOf<Song>()
        val selection = "${Media.IS_MUSIC} != 0 AND ${Media.ALBUM_ID} = ?"
        val sortOrder = "${Media.TRACK} ASC"

        context.contentResolver.query(
            Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            arrayOf(albumId.toString()),
            sortOrder
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                songs.add(cursor.toSong())
            }
        }
        songs
    }

    suspend fun getSongsByArtist(artistId: Long): List<Song> = withContext(Dispatchers.IO) {
        val songs = mutableListOf<Song>()
        val selection = "${Media.IS_MUSIC} != 0 AND ${Media.ARTIST_ID} = ?"
        val sortOrder = "${Media.ALBUM} ASC, ${Media.TRACK} ASC"

        context.contentResolver.query(
            Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            arrayOf(artistId.toString()),
            sortOrder
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                songs.add(cursor.toSong())
            }
        }
        songs
    }

    suspend fun getSongsByFolder(folderPath: String): List<Song> = withContext(Dispatchers.IO) {
        val songs = mutableListOf<Song>()
        val selection = "${Media.IS_MUSIC} != 0 AND ${Media.DATA} LIKE ?"
        val sortOrder = "${Media.TITLE} ASC"

        context.contentResolver.query(
            Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            arrayOf("$folderPath%"),
            sortOrder
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                val song = cursor.toSong()
                if (song.path.startsWith(folderPath)) {
                    songs.add(song)
                }
            }
        }
        songs
    }

    suspend fun getAlbums(): List<Album> = withContext(Dispatchers.IO) {
        val albums = mutableListOf<Album>()
        val albumProjection = arrayOf(
            MediaStore.Audio.Albums._ID,
            MediaStore.Audio.Albums.ALBUM,
            MediaStore.Audio.Albums.ARTIST,
            MediaStore.Audio.Albums.NUMBER_OF_SONGS
        )

        context.contentResolver.query(
            MediaStore.Audio.Albums.EXTERNAL_CONTENT_URI,
            albumProjection,
            null,
            null,
            "${MediaStore.Audio.Albums.ALBUM} ASC"
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums._ID)
            val albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ALBUM)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.ARTIST)
            val songCountColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Albums.NUMBER_OF_SONGS)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idColumn)
                albums.add(
                    Album(
                        id = id,
                        name = cursor.getString(albumColumn) ?: "Unknown Album",
                        artist = cursor.getString(artistColumn) ?: "Unknown Artist",
                        albumArtUri = getAlbumArtUri(id).toString(),
                        numberOfSongs = cursor.getInt(songCountColumn)
                    )
                )
            }
        }
        albums
    }

    suspend fun getArtists(): List<Artist> = withContext(Dispatchers.IO) {
        val artists = mutableListOf<Artist>()
        val artistProjection = arrayOf(
            MediaStore.Audio.Artists._ID,
            MediaStore.Audio.Artists.ARTIST,
            MediaStore.Audio.Artists.NUMBER_OF_ALBUMS,
            MediaStore.Audio.Artists.NUMBER_OF_TRACKS
        )

        context.contentResolver.query(
            MediaStore.Audio.Artists.EXTERNAL_CONTENT_URI,
            artistProjection,
            null,
            null,
            "${MediaStore.Audio.Artists.ARTIST} ASC"
        )?.use { cursor ->
            val idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists._ID)
            val artistColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.ARTIST)
            val albumCountColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.NUMBER_OF_ALBUMS)
            val trackCountColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Artists.NUMBER_OF_TRACKS)

            while (cursor.moveToNext()) {
                artists.add(
                    Artist(
                        id = cursor.getLong(idColumn),
                        name = cursor.getString(artistColumn) ?: "Unknown Artist",
                        numberOfAlbums = cursor.getInt(albumCountColumn),
                        numberOfSongs = cursor.getInt(trackCountColumn)
                    )
                )
            }
        }
        artists
    }

    suspend fun getFolders(): List<Folder> = withContext(Dispatchers.IO) {
        val folderMap = mutableMapOf<String, Int>()
        val selection = "${Media.IS_MUSIC} != 0"

        context.contentResolver.query(
            Media.EXTERNAL_CONTENT_URI,
            arrayOf(Media.DATA),
            selection,
            null,
            null
        )?.use { cursor ->
            val dataColumn = cursor.getColumnIndexOrThrow(Media.DATA)
            while (cursor.moveToNext()) {
                val path = cursor.getString(dataColumn)
                val folder = File(path).parentFile?.path ?: continue
                folderMap[folder] = (folderMap[folder] ?: 0) + 1
            }
        }

        folderMap.map { (path, count) ->
            Folder(
                path = path,
                name = File(path).name,
                numberOfSongs = count
            )
        }.sortedByDescending { it.numberOfSongs }
    }

    suspend fun searchSongs(query: String): List<Song> = withContext(Dispatchers.IO) {
        val songs = mutableListOf<Song>()
        val selection = "${Media.IS_MUSIC} != 0 AND (${Media.TITLE} LIKE ? OR ${Media.ARTIST} LIKE ? OR ${Media.ALBUM} LIKE ?)"
        val searchQuery = "%$query%"
        val sortOrder = "${Media.TITLE} ASC"

        context.contentResolver.query(
            Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            arrayOf(searchQuery, searchQuery, searchQuery),
            sortOrder
        )?.use { cursor ->
            while (cursor.moveToNext()) {
                songs.add(cursor.toSong())
            }
        }
        songs
    }

    private fun Cursor.toSong(): Song {
        val id = getLong(getColumnIndexOrThrow(Media._ID))
        val albumId = getColumnIndex(Media.ALBUM_ID).takeIf { it >= 0 }?.let { getLong(it) }
        val artistId = getColumnIndex(Media.ARTIST_ID).takeIf { it >= 0 }?.let { getLong(it) }
        
        // Audio quality info
        val bitrate = getColumnIndex(Media.BITRATE).takeIf { it >= 0 }?.let { 
            getInt(it) / 1000  // Convert from bps to kbps
        } ?: 0
        val sampleRate = getColumnIndex(Media.SAMPLE_RATE).takeIf { it >= 0 }?.let { 
            getInt(it) 
        } ?: 0
        val fileSize = getColumnIndex(Media.SIZE).takeIf { it >= 0 }?.let { 
            getLong(it) 
        } ?: 0L

        return Song(
            id = id,
            title = getString(getColumnIndexOrThrow(Media.TITLE)) ?: "Unknown",
            artist = getString(getColumnIndexOrThrow(Media.ARTIST)) ?: "Unknown Artist",
            album = getString(getColumnIndexOrThrow(Media.ALBUM)) ?: "Unknown Album",
            albumId = albumId,
            artistId = artistId,
            duration = getLong(getColumnIndexOrThrow(Media.DURATION)),
            path = getString(getColumnIndexOrThrow(Media.DATA)),
            dateAdded = getLong(getColumnIndexOrThrow(Media.DATE_ADDED)) * 1000,
            albumArtUri = albumId?.let { getAlbumArtUri(it).toString() },
            bitrate = bitrate,
            sampleRate = sampleRate,
            fileSize = fileSize
        )
    }

    private fun getAlbumArtUri(albumId: Long): Uri {
        return ContentUris.withAppendedId(
            Uri.parse("content://media/external/audio/albumart"),
            albumId
        )
    }
}
