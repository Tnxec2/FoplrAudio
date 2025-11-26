package com.kontranik.foplraudio.player

import android.content.ComponentName
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.provider.MediaStore
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.*
import com.google.common.util.concurrent.ListenableFuture
import com.kontranik.foplraudio.data.StorageManager
import com.kontranik.foplraudio.model.FileItem
import com.kontranik.foplraudio.model.FolderBookmark
import com.kontranik.foplraudio.model.PlayerStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.core.net.toUri
import androidx.media3.common.MediaMetadata
import androidx.media3.common.util.Log
import com.kontranik.foplraudio.model.AudioTrack
import java.io.File


@RequiresApi(Build.VERSION_CODES.P)
@androidx.annotation.OptIn(UnstableApi::class)
class PlayerViewModel(private val context: Context) : ViewModel() {

    private val storageManager = StorageManager(context)

    // MediaController für MediaSession-Steuerung (empfohlene Methode)
    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var mediaController: MediaController? = null

    // Direkter Zugriff auf den ExoPlayer (über Binder)
    private val _directExoPlayer = MutableStateFlow<ExoPlayer?>(null)
    val directExoPlayer = _directExoPlayer.asStateFlow() // Kann nun von anderen Komponenten abonniert werden

    // DER KONSOLIDIERTE PLAYER-STATUS
    private val _playerStatus = MutableStateFlow(PlayerStatus())
    val playerStatus = _playerStatus.asStateFlow()

    // Status des Ordnerbrowsers
    private val _folders = MutableStateFlow<List<FolderBookmark>>(emptyList())
    val folders = _folders.asStateFlow()

    private val _currentFiles = MutableStateFlow<List<FileItem>>(emptyList())
    val currentFiles = _currentFiles.asStateFlow()

    private val _currentPathStack = MutableStateFlow<List<FileItem>>(emptyList())
    val currentPathStack = _currentPathStack.asStateFlow()

    // Service Connection für den lokalen Binder
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as PlaybackService.LocalBinder
            _directExoPlayer.value = binder.getPlayer()
            // Hier könnten Sie spezifische ExoPlayer-Methoden aufrufen,
            // die NICHT in MediaController verfügbar sind.
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            _directExoPlayer.value = null
        }
    }

    init {
        _folders.value = storageManager.loadFolders()

        connectToMediaController()
        bindToExoPlayerService()

        restoreLastState()

        viewModelScope.launch {
            while (true) {
                // Position nur aktualisieren, wenn ein Controller verbunden ist und spielt
                mediaController?.let { controller ->
                    if (controller.isPlaying) {
                        _playerStatus.value = _playerStatus.value.copy(
                            position = controller.currentPosition,
                            duration = controller.duration.coerceAtLeast(0L)
                        )
                    }
                }
                delay(500)
            }
        }
    }

    /**
     * Stellt die Verbindung zum PlaybackService her und initialisiert den MediaController.
     */
    @RequiresApi(Build.VERSION_CODES.P)
    private fun connectToMediaController() {
        val sessionToken = SessionToken(
            context,
            ComponentName(context, PlaybackService::class.java)
        )
        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()

        controllerFuture?.addListener({
            mediaController = controllerFuture?.get()
            mediaController?.addListener(controllerListener)
            // Initialen Status beim Verbinden abrufen
            updateControllerState()
        }, context.mainExecutor)
    }

    /**
     * Bindet den Service, um direkten Zugriff auf den ExoPlayer über den LocalBinder zu erhalten.
     */
    private fun bindToExoPlayerService() {
        val intent = Intent(context, PlaybackService::class.java).apply {
            action = PlaybackService.ACTION_BIND_EXOPLAYER
        }
        context.bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    private val controllerListener = object : Player.Listener {
        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _playerStatus.value = _playerStatus.value.copy(isPlaying = isPlaying)
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            updateCurrentTrackInfo(mediaController?.currentMediaItem)
            updatePlaylistState()

            // Lokale Logik für "Stop nach Titel"
            if (_playerStatus.value.stopAfterCurrent && reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
                mediaController?.pause()
                // Setze den lokalen Status zurück, nachdem die Aktion ausgeführt wurde
                _playerStatus.value = _playerStatus.value.copy(stopAfterCurrent = false)
            }
        }

        override fun onTimelineChanged(timeline: Timeline, reason: Int) {
            updatePlaylistState()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_READY) {
                _playerStatus.value = _playerStatus.value.copy(
                    duration = mediaController?.duration ?: 0L
                )
            }
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            _playerStatus.value = _playerStatus.value.copy(shuffleMode = shuffleModeEnabled)
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            _playerStatus.value = _playerStatus.value.copy(repeatMode = repeatMode)
        }
    }

    private fun updateControllerState() {
        mediaController?.let { controller ->
            _playerStatus.value = _playerStatus.value.copy(
                isPlaying = controller.isPlaying,
                shuffleMode = controller.shuffleModeEnabled,
                repeatMode = controller.repeatMode,
                duration = controller.duration.coerceAtLeast(0L)
            )
            updateCurrentTrackInfo(controller.currentMediaItem)
            updatePlaylistState()
        }
    }

    private fun updateCurrentTrackInfo(mediaItem: MediaItem?) {
        val title = mediaItem?.mediaMetadata?.title?.toString() ?: ""
        val artist = mediaItem?.mediaMetadata?.artist?.toString() ?: ""
        val artworkBytes = mediaItem?.mediaMetadata?.artworkData

        Log.d("PlayerViewModel", "Updating current track info: title=$title, artist=$artist, artworkBytes=${artworkBytes?.size}")

        _playerStatus.value = _playerStatus.value.copy(
            currentTrackTitle = title,
            currentTrackArtist = artist,
            currentArtworkBytes = artworkBytes,
            duration = mediaController?.duration?.coerceAtLeast(0L) ?: 0L,
            position = 0L,
            currentIndex = mediaController?.currentMediaItemIndex ?: -1
        )

        mediaItem?.mediaId?.let { uriStr ->
            storageManager.saveLastPlayedUri(uriStr)
        }
    }

    fun getTrackIconFallback(title: String): ImageVector {
        val lowerTitle = title.lowercase()
        return when {
            lowerTitle.contains("hörspiel") || lowerTitle.contains("hoerspiel") || lowerTitle.contains("audio drama") -> Icons.Default.Book
            lowerTitle.contains("podcast") || lowerTitle.contains("episode") -> Icons.Default.Mic
            else -> Icons.Default.MusicNote
        }
    }

    private fun updatePlaylistState() {
        mediaController?.let { controller ->
            val items = mutableListOf<MediaItem>()
            for (i in 0 until controller.mediaItemCount) {
                items.add(controller.getMediaItemAt(i))
            }
            _playerStatus.value = _playerStatus.value.copy(
                playlist = items,
                currentIndex = controller.currentMediaItemIndex
            )
        } ?: run {
            _playerStatus.value = _playerStatus.value.copy(
                playlist = emptyList(),
                currentIndex = -1
            )
        }
    }

    // --- Playlist Management ---

    fun playQueueItem(index: Int) {
        mediaController?.seekToDefaultPosition(index)
        mediaController?.play()
    }

    fun removeQueueItem(index: Int) {
        mediaController?.removeMediaItem(index)
    }

    // --- Folder Management (unchanged logic) ---

    fun addFolder(uri: Uri) {
        storageManager.persistUriPermission(uri)
        val docFile = DocumentFile.fromTreeUri(context, uri)
        val name = docFile?.name ?: uri.lastPathSegment ?: "Unbekannter Ordner"

        val newList = _folders.value.toMutableList().apply {
            add(FolderBookmark(uri.toString(), name))
        }
        _folders.value = newList
        storageManager.saveFolders(newList)
    }

    fun removeFolder(folder: FolderBookmark) {
        val currentList = _folders.value.toMutableList()
        if (currentList.remove(folder)) {
            _folders.value = currentList
            storageManager.saveFolders(currentList)
            storageManager.releaseUriPermission(folder.uriString.toUri())
        }
    }

    // --- Navigation & Playback ---

    fun openFolder(folderUri: Uri, folderName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val docFile = DocumentFile.fromTreeUri(context, folderUri)
            if (docFile != null && docFile.isDirectory) {
                val files = docFile.listFiles()
                    .filter { it.name?.startsWith(".")?.not() == true && (it.isDirectory  || isAudioFile(it.name ?: "")) }
                    .map {
                        FileItem(
                            name = it.name ?: "Unbekannt",
                            uri = it.uri,
                            isDirectory = it.isDirectory,
                            parentUri = folderUri
                        )
                    }
                    .sortedBy { !it.isDirectory }

                withContext(Dispatchers.Main) {
                    _currentFiles.value = files

                    val newItem = FileItem(folderName, folderUri, true, Uri.EMPTY)
                    if (_currentPathStack.value.isEmpty() || _currentPathStack.value.last().uri != folderUri) {
                        _currentPathStack.value = _currentPathStack.value.plus(newItem)
                    }

                    storageManager.saveLastOpenedFolder(folderUri, folderName)
                }
            }
        }
    }

    fun navigateBack() {
        val stack = _currentPathStack.value
        if (stack.isNotEmpty()) {
            val newStack = stack.dropLast(1)
            _currentPathStack.value = newStack
            if (newStack.isNotEmpty()) {
                openFolder(newStack.last().uri, newStack.last().name)
                _currentPathStack.value = newStack
            } else {
                _currentFiles.value = emptyList()
            }
        }
    }

    fun playFile(selectedFile: FileItem, allFiles: List<FileItem>) {
        val audioFiles = allFiles.filter { !it.isDirectory }
        viewModelScope.launch(Dispatchers.IO) {
            val mediaItems = audioFiles.map { fileItem ->
                val metadataBuilder = MediaMetadata.Builder()
                val retriever = MediaMetadataRetriever()
                try {
                    retriever.setDataSource(context, fileItem.uri)
                    metadataBuilder.setTitle(
                        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                            ?: fileItem.name
                    )
                    metadataBuilder.setArtist(
                        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                    )
                    metadataBuilder.setAlbumTitle(
                        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                    )
                    metadataBuilder.setAlbumArtist(
                        retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST)
                    )
                    metadataBuilder.setArtworkUri(fileItem.uri)
                    val artworkBytes = retriever.embeddedPicture
                    artworkBytes?.let {
                        metadataBuilder.setArtworkData(
                            it,
                            MediaMetadata.PICTURE_TYPE_FRONT_COVER
                        )
                    }
                } catch (e: Exception) {
                    Log.e("PlayerViewModel", "Error getting metadata for ${fileItem.uri}", e)
                    metadataBuilder.setTitle(fileItem.name)
                } finally {
                    retriever.release()
                }
                MediaItem.Builder()
                    .setUri(fileItem.uri)
                    .setMediaId(fileItem.uri.toString())
                    .setMediaMetadata(metadataBuilder.build())
                    .build()
            }

            withContext(Dispatchers.Main) {
                val startIndex = audioFiles.indexOfFirst { it.uri == selectedFile.uri }
                if (startIndex != -1) {
                    mediaController?.setMediaItems(mediaItems, startIndex, 0L)
                    mediaController?.prepare()
                    mediaController?.play()
                }
            }
        }
    }

    fun playFolderContents(folderUri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val docFile = DocumentFile.fromTreeUri(context, folderUri)
            val files = docFile?.listFiles()?.filter { isAudioFile(it.name ?: "") } ?: emptyList()
            loadMediaItems(files)
        }
    }

    fun playFolderRecursive(folderUri: Uri) {
        viewModelScope.launch(Dispatchers.IO) {
            val docFile = DocumentFile.fromTreeUri(context, folderUri)
            val files = if (docFile != null) getAllAudioFilesRecursive(docFile) else emptyList()
            loadMediaItems(files)
        }
    }

    private suspend fun loadMediaItems(files: List<DocumentFile>) {
        val mediaItems = files.map { file ->
            val retriever = MediaMetadataRetriever()
            val metadataBuilder = MediaMetadata.Builder()
            try {
                retriever.setDataSource(context, file.uri)
                metadataBuilder.setTitle(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: file.name)
                metadataBuilder.setArtist(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST))
                metadataBuilder.setAlbumTitle(
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                )
                metadataBuilder.setAlbumArtist(
                    retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST)
                )
                metadataBuilder.setArtworkUri(file.uri)
                val artworkBytes = retriever.embeddedPicture
                artworkBytes?.let {
                    metadataBuilder.setArtworkData(it, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
                }
            } catch (e: Exception) {
                Log.e("PlayerViewModel", "Error getting metadata for ${file.uri}", e)
                metadataBuilder.setTitle(file.name)
            } finally {
                retriever.release()
            }
            MediaItem.Builder()
                .setUri(file.uri)
                .setMediaId(file.uri.toString())
                .setMediaMetadata(metadataBuilder.build())
                .build()
        }

        withContext(Dispatchers.Main) {
            if (mediaItems.isNotEmpty()) {
                mediaController?.setMediaItems(mediaItems, 0, 0L)
                mediaController?.prepare()
                mediaController?.play()
            } else {
                Toast.makeText(context, "Keine Audiodateien gefunden", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getAllAudioFilesRecursive(dir: DocumentFile): List<DocumentFile> {
        val audioFiles = mutableListOf<DocumentFile>()
        val files = dir.listFiles()
        for (file in files) {
            if (file.isDirectory) {
                audioFiles.addAll(getAllAudioFilesRecursive(file))
            } else {
                if (isAudioFile(file.name ?: "")) {
                    audioFiles.add(file)
                }
            }
        }
        return audioFiles
    }

    fun loadTracksForFolder(path: String): MutableList<AudioTrack> {
        val trackList = mutableListOf<AudioTrack>()

        val projection = arrayOf(
            MediaStore.Audio.Media._ID, MediaStore.Audio.Media.TITLE, MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DATA, MediaStore.Audio.Media.DURATION, MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ARTIST
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DATA} LIKE ?"
        val selectionArgs = arrayOf("${path}/%")

        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, selection, selectionArgs, MediaStore.Audio.Media.TITLE
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
            val albumArtistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ARTIST)

            val pathCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
            val durCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)

            while (cursor.moveToNext()) {
                val path = cursor.getString(pathCol)
                if (File(path).parent == path) {
                    val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, cursor.getLong(idCol))
                    trackList.add(
                        AudioTrack(
                            id = cursor.getLong(idCol),
                            title = cursor.getString(titleCol) ?: "Unbekannt",
                            artist = cursor.getString(artistCol) ?: "Unbekannt",
                            album = cursor.getString(albumCol) ?: "Unbekannt",
                            albumArtist = cursor.getString(albumArtistCol) ?: "Unbekannt",
                            path = path,
                            duration = cursor.getLong(durCol),
                            uri = uri,
                        )
                    )
                }
            }
        }
        return trackList
    }

    // --- Player Controls (über Controller) ---
    // HINWEIS: Diese Methoden verwenden weiterhin den MediaController.

    fun togglePlayPause() {
        mediaController?.let {
            if (it.isPlaying) it.pause() else it.play()
        }
    }

    fun skipNext() = mediaController?.seekToNext()
    fun skipPrev() = mediaController?.seekToPrevious()
    fun seekTo(pos: Long) = mediaController?.seekTo(pos)

    fun toggleShuffle() {
        mediaController?.shuffleModeEnabled = !(mediaController?.shuffleModeEnabled ?: false)
    }

    fun toggleRepeat() {
        mediaController?.let { controller ->
            val modes = listOf(Player.REPEAT_MODE_OFF, Player.REPEAT_MODE_ALL, Player.REPEAT_MODE_ONE)
            val current = controller.repeatMode
            val next = modes[(modes.indexOf(current) + 1) % modes.size]
            controller.repeatMode = next
        }
    }

    fun toggleStopAfterCurrent() {
        _playerStatus.value = _playerStatus.value.copy(
            stopAfterCurrent = !_playerStatus.value.stopAfterCurrent
        )
    }

    // --- Helpers ---
    private fun isAudioFile(name: String): Boolean {
        val lower = name.lowercase()
        return lower.endsWith(".mp3") || lower.endsWith(".wav") || lower.endsWith(".ogg") || lower.endsWith(".m4a") || lower.endsWith(".flac")
    }

    private fun restoreLastState() {
        val lastState = storageManager.getLastOpenedFolder()
        lastState?.let { (uri, name) ->
            openFolder(uri, name)
        }
    }

    override fun onCleared() {
        // Trenne die Verbindung zum Controller
        mediaController?.removeListener(controllerListener)
        controllerFuture?.let { MediaController.releaseFuture(it) }

        // Unbind des Services
        context.unbindService(serviceConnection)

        super.onCleared()
    }
}

class PlayerViewModelFactory(private val context: Context) : androidx.lifecycle.ViewModelProvider.Factory {
    @RequiresApi(Build.VERSION_CODES.P)
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PlayerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return PlayerViewModel(context) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}