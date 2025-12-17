package com.kontranik.foplraudio.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kontranik.foplraudio.model.MediaPlace
import androidx.core.content.edit
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import com.kontranik.foplraudio.model.FileItem
import com.kontranik.foplraudio.model.FileItemDTO
import com.kontranik.foplraudio.model.PlayerStatus
import com.kontranik.foplraudio.model.toDTO
import com.kontranik.foplraudio.model.toFileItem
import kotlin.math.min

class StorageManager(private val context: Context) {
    private val prefs = context.getSharedPreferences("audio_player_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        const val MEDIA_PLACES_KEY = "media_places"
        const val CURRENT_PLAYLIST_KEY = "current_playlist"
        const val CURRENT_PLAYLIST_INDEX_KEY = "current_playlist_index"

        const val PAUSE_AT_END_OF_MEDIA_ITEMS_KEY = "pauseAtEndOfMediaItems"
        const val SHUFFLE_MODE_KEY = "shuffleMode"
        const val REPEAT_MODE_KEY = "repeatMode"
        const val CURRENT_PATH_STACK_KEY = "CURRENT_PATH_STACK"
    }

    fun saveMediaPlaces(mediaPlaces: List<MediaPlace>) {
        val json = gson.toJson(mediaPlaces)
        prefs.edit {
            putString(MEDIA_PLACES_KEY, json)
            apply()
        }
    }

    fun loadMediaPlaces(): List<MediaPlace> {
        val json = prefs.getString(MEDIA_PLACES_KEY, null)
        return if (json != null) {
            val type = object : TypeToken<List<MediaPlace>>() {}.type
            gson.fromJson(json, type)
        } else {
            emptyList()
        }
    }

    fun persistUriPermission(uri: Uri) {
        val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        try {
            context.contentResolver.takePersistableUriPermission(uri, takeFlags)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    fun releaseUriPermission(uri: Uri) {
        val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        try {
            context.contentResolver.releasePersistableUriPermission(uri, takeFlags)
        } catch (_: Exception) {
            // Ignorieren falls Berechtigung nicht mehr existiert
        }
    }


    fun loadLastPathStack() : List<FileItem> {
        val result: MutableList<FileItem> = mutableListOf()
        val json = prefs.getString(CURRENT_PATH_STACK_KEY, null)
        if (json != null) {
            try {
                val type = object : TypeToken<List<FileItemDTO>>() {}.type
                val stack: List<FileItemDTO> = gson.fromJson(json, type)
                stack.forEach { item ->
                    val fileItem = item.toFileItem()
                    val document = DocumentFile.fromTreeUri(context, fileItem.uri)
                    if (document?.canRead() == true) {
                        result.add(fileItem)
                    } else {
                        Log.w("StorageManager", "No permissions for ${fileItem.uri}. This entry will be removed.")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        if (result.isEmpty()) {
            prefs.edit {
                remove(CURRENT_PATH_STACK_KEY)
                apply()
            }
        }
        return result
    }

    fun saveCurrentPathStack(pathStack: List<FileItem>) {
        val json = gson.toJson(pathStack.map { fileItem -> fileItem.toDTO() })
        prefs.edit {
            putString(CURRENT_PATH_STACK_KEY, json)
            apply()
        }
    }


    fun loadPlayerStatus(): PlayerStatus {
        return PlayerStatus(
            pauseAtEndOfMediaItems = prefs.getBoolean(PAUSE_AT_END_OF_MEDIA_ITEMS_KEY, false),
            shuffleMode = prefs.getBoolean(SHUFFLE_MODE_KEY, false),
            repeatMode = prefs.getInt(REPEAT_MODE_KEY, Player.REPEAT_MODE_OFF)
        )
    }

    fun saveRepeatMode(repeatMode: Int) {
        prefs.edit {
            putInt(REPEAT_MODE_KEY, repeatMode)
            apply()
        }
    }

    fun saveShuffleMode(shuffleMode: Boolean) {
        prefs.edit {
            putBoolean(SHUFFLE_MODE_KEY, shuffleMode)
            apply()
        }
    }

    fun savePauseAtEndOfMediaItems(pauseAtEndOfMediaItems: Boolean) {
        prefs.edit {
            putBoolean(PAUSE_AT_END_OF_MEDIA_ITEMS_KEY, pauseAtEndOfMediaItems)
            apply()
        }
    }

    fun savePlaylist(
        items: List<MediaItem>
    ) {
        prefs.edit {
            val json = items.mapIndexed { index, item ->
                gson.toJson(
                    PlaylistItemDto(
                        index = index,
                        uri = item.mediaId,
                        title = item.mediaMetadata.title.toString()
                    )
                )
            }.toSet()
            putStringSet(CURRENT_PLAYLIST_KEY, json)
            apply()
        }
    }

    fun savePlaylistCurrentIndex(
        currentMediaItemIndex: Int
    ) {
        prefs.edit {
            putInt(CURRENT_PLAYLIST_INDEX_KEY, currentMediaItemIndex)
            apply()
        }
    }

    fun clearPlaylist() {
        prefs.edit {
            remove(CURRENT_PLAYLIST_KEY)
            remove(CURRENT_PLAYLIST_INDEX_KEY)
            apply()
        }
    }

    fun loadLastPlaylist(): PlaylistStatus {
        val result = mutableListOf<PlaylistItem>()
        val json = prefs.getStringSet(CURRENT_PLAYLIST_KEY, null)
        var lastPlaylistIndex = prefs.getInt(CURRENT_PLAYLIST_INDEX_KEY, -1)
        if (json != null) {
            try {
                json.forEach { json ->
                    val playlistItem: PlaylistItem = gson.fromJson(json, PlaylistItemDto::class.java).toPlaylistItem()
                    val document = DocumentFile.fromTreeUri(context, playlistItem.uri)
                    if (document?.canRead() == true) {
                        result.add(playlistItem)
                    } else {
                        Log.w("StorageManager", "No permissions for ${playlistItem.uri}. This entry will be removed.")
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        if (result.isEmpty()) {
            clearPlaylist()
            lastPlaylistIndex = -1
        }

        result.sortBy { it.index }

        lastPlaylistIndex = min(lastPlaylistIndex, result.size-1)

        return PlaylistStatus(
            result,
            lastPlaylistIndex)
    }
}

data class PlaylistItem(
    val index: Int,
    val uri: Uri,
    val title: String,
)
data class PlaylistItemDto(
    val index: Int,
    val uri: String,
    val title: String,
) {
    fun toPlaylistItem(): PlaylistItem = PlaylistItem(index, uri.toUri(), title)
}
data class PlaylistStatus(
    val playlistUris: List<PlaylistItem> = emptyList(),
    val currentIndex: Int = -1
)


