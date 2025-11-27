package com.kontranik.foplraudio.player

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionResult
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.kontranik.foplraudio.R
import com.kontranik.foplraudio.data.StorageManager
import com.kontranik.foplraudio.model.FileItem
import com.kontranik.foplraudio.model.MediaPlace
import com.kontranik.foplraudio.model.PlayerStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


@RequiresApi(Build.VERSION_CODES.P)
@androidx.annotation.OptIn(UnstableApi::class)
class PlayerViewModel(private val context: Context) : ViewModel() {

    private val storageManager = StorageManager(context)

    var playerController: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null

    private val _isInitializing = MutableStateFlow(true)
    val isInitializing = _isInitializing.asStateFlow()

    private val _playerStatus = MutableStateFlow(PlayerStatus())
    val playerStatus = _playerStatus.asStateFlow()

    private val _mediaPlaces = MutableStateFlow<List<MediaPlace>>(emptyList())
    val mediaPlaces = _mediaPlaces.asStateFlow()

    private val _currentFiles = MutableStateFlow<List<FileItem>>(emptyList())
    val currentFiles = _currentFiles.asStateFlow()

    private val _currentPathStack = MutableStateFlow<List<FileItem>>(emptyList())
    val currentPathStack = _currentPathStack.asStateFlow()


    private val controllerListener = object : Player.Listener {

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            _playerStatus.value = _playerStatus.value.copy(isPlaying = isPlaying)
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            updateCurrentTrackInfo(playerController?.currentMediaItem)
            updatePlaylistState()
        }

        override fun onTimelineChanged(timeline: Timeline, reason: Int) {
            updatePlaylistState()
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_READY) {
                _playerStatus.value = _playerStatus.value.copy(
                    duration = playerController?.duration ?: 0L
                )
            }
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            _playerStatus.value = _playerStatus.value.copy(shuffleMode = shuffleModeEnabled)
            storageManager.saveShuffleMode(shuffleModeEnabled)
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            _playerStatus.value = _playerStatus.value.copy(repeatMode = repeatMode)
            storageManager.saveRepeatMode(repeatMode = repeatMode)
        }
    }

    init {
        restoreLastState()

        initController()

        viewModelScope.launch {
            while (true) {
                playerController?.let { controller ->
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

    private fun initController() {
        val sessionToken =
            SessionToken(
                context,
                ComponentName(context, PlaybackService::class.java)
            )

        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()

        controllerFuture?.addListener({
            try {
                val controller = controllerFuture?.get()
                playerController = controller
                playerController?.addListener( controllerListener)
                initControllerState()
                _isInitializing.value = false
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, ContextCompat.getMainExecutor(context))
    }

    private fun initControllerState() {
        playerController?.let { controller ->
            _playerStatus.value = _playerStatus.value.copy(
                isPlaying = controller.isPlaying,
                duration = controller.duration.coerceAtLeast(0L)
            )
            controller.shuffleModeEnabled = _playerStatus.value.shuffleMode
            controller.repeatMode = _playerStatus.value.repeatMode
            setPauseAtEndOfMediaItems(_playerStatus.value.pauseAtEndOfMediaItems)

            val lastPlaylist = storageManager.loadLastPlaylist()
            if (lastPlaylist.playlist.isNotEmpty()) {
                controller.setMediaItems(lastPlaylist.playlist, lastPlaylist.currentIndex, 0L)
                controller.prepare()
            } else {
                updatePlaylistState()
            }
            updateCurrentTrackInfo(controller.currentMediaItem)
        }
    }

    private fun updateCurrentTrackInfo(mediaItem: MediaItem?) {
        val title = mediaItem?.mediaMetadata?.title?.toString() ?: ""
        val artist = mediaItem?.mediaMetadata?.artist?.toString() ?: ""
        val artworkBytes = mediaItem?.mediaMetadata?.artworkData

        _playerStatus.value = _playerStatus.value.copy(
            currentTrackTitle = title,
            currentTrackArtist = artist,
            currentArtworkBytes = artworkBytes,
            duration = playerController?.duration?.coerceAtLeast(0L) ?: 0L,
            position = 0L,
            currentIndex = playerController?.currentMediaItemIndex ?: -1
        )
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
        playerController?.let { controller ->
            val items = mutableListOf<MediaItem>()
            for (i in 0 until controller.mediaItemCount) {
                items.add(controller.getMediaItemAt(i))
            }
            var isPlaylistSame = items.size == _playerStatus.value.playlist.size

            if (isPlaylistSame) {
                for (item in items) {
                    if (_playerStatus.value.playlist.find { pl -> pl.mediaId == item.mediaId } == null) {
                        isPlaylistSame = false
                        break
                    }
                }
            }

            if (!isPlaylistSame) {
                _playerStatus.value = _playerStatus.value.copy(
                    playlist = items,
                    currentIndex = controller.currentMediaItemIndex
                )
                storageManager.savePlaylist(items)

            }
            storageManager.savePlaylistCurrentIndex(controller.currentMediaItemIndex)
        } ?: run {
            _playerStatus.value = _playerStatus.value.copy(
                playlist = emptyList(),
                currentIndex = -1
            )
            storageManager.clearPlaylist()
        }
    }

    // --- Playlist Management ---

    fun playQueueItem(index: Int) {
        playerController?.seekToDefaultPosition(index)
        playerController?.play()
    }

    fun removeQueueItem(index: Int) {
        playerController?.removeMediaItem(index)
    }

    // --- Folder Management (unchanged logic) ---

    fun addFolder(uri: Uri) {
        storageManager.persistUriPermission(uri)
        val docFile = DocumentFile.fromTreeUri(context, uri)
        val name = docFile?.name ?: uri.lastPathSegment ?: context.getString(R.string.unknown_folder)

        val newList = _mediaPlaces.value.toMutableList().apply {
            add(MediaPlace(uri.toString(), name))
        }
        _mediaPlaces.value = newList
        storageManager.saveMediaPlaces(newList)
    }

    fun removeFolder(folder: MediaPlace) {
        val currentList = _mediaPlaces.value.toMutableList()
        if (currentList.remove(folder)) {
            _mediaPlaces.value = currentList
            storageManager.saveMediaPlaces(currentList)
            storageManager.releaseUriPermission(folder.uriString.toUri())
        }
    }

    // --- Navigation & Playback ---

    fun openFolder(folderUri: Uri, folderName: String, saveStack: Boolean = true) {
        viewModelScope.launch(Dispatchers.IO) {
            val docFile = DocumentFile.fromTreeUri(context, folderUri)
            if (docFile != null && docFile.isDirectory) {
                val files = docFile.listFiles()
                    .filter { it.name?.startsWith(".")?.not() == true && (it.isDirectory  || isAudioFile(it.name ?: "", it.uri.toString())) }
                    .map {
                        FileItem(
                            name = it.name ?: context.getString(R.string.unknown),
                            uri = it.uri,
                            isDirectory = it.isDirectory,
                            parentUri = folderUri
                        )
                    }
                    .sortedBy { !it.isDirectory }

                withContext(Dispatchers.Main) {
                    _currentFiles.value = files

                    if (saveStack) {
                        if (_currentPathStack.value.isEmpty() || _currentPathStack.value.last().uri != folderUri) {
                            val newItem = FileItem(folderName, folderUri, true, Uri.EMPTY)
                            _currentPathStack.value = _currentPathStack.value.plus(newItem)
                        }

                        storageManager.saveCurrentPathStack(_currentPathStack.value)
                    }
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
                val previousFolder = newStack.last()
                openFolder(previousFolder.uri, previousFolder.name)
            }
        }
    }

    fun playFile(selectedFile: FileItem, allFiles: List<FileItem>) {
        val audioFiles = allFiles.filter { !it.isDirectory }
        viewModelScope.launch(Dispatchers.IO) {
            val mediaItems = audioFiles.map { fileItem ->
                MediaItem.Builder()
                    .setUri(fileItem.uri)
                    .setMediaId(fileItem.uri.toString())
                    .setMediaMetadata(getMetadata(fileItem.uri, fileItem.name))
                    .build()
            }

            withContext(Dispatchers.Main) {
                val startIndex = audioFiles.indexOfFirst { it.uri == selectedFile.uri }
                if (startIndex != -1) {
                    playerController?.setMediaItems(mediaItems, startIndex, 0L)
                    playerController?.prepare()
                    playerController?.play()
                }
            }
        }
    }

    fun playFolderRecursive(folderUri: Uri) {
        playFolderRecursive(folderUri, true)
    }

    fun addFolderRecursive(folderUri: Uri) {
        playFolderRecursive(folderUri, false)
    }

    private fun playFolderRecursive(folderUri: Uri, replace: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val docFile = DocumentFile.fromTreeUri(context, folderUri)
            val files = if (docFile != null) getAllAudioFilesRecursive(docFile) else emptyList()
            loadMediaItems(files, replace)
        }
    }

    private suspend fun loadMediaItems(files: List<DocumentFile>, replace: Boolean = false) {
        val mediaItems = files.map { file ->
            MediaItem.Builder()
                .setUri(file.uri)
                .setMediaId(file.uri.toString())
                .setMediaMetadata(getMetadata(file.uri, file.name ?: context.getString(R.string.unknown)))
                .build()
        }

        withContext(Dispatchers.Main) {
            if (mediaItems.isNotEmpty()) {
                if (replace) {
                    playerController?.setMediaItems(mediaItems, 0, 0L)
                    playerController?.prepare()
                    playerController?.play()
                } else {
                    playerController?.addMediaItems(mediaItems)
                }
            } else {
                Toast.makeText(context,
                    context.getString(R.string.no_audio_files_found), Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getMetadata(uri: Uri, fallBackName: String) : MediaMetadata {
        val retriever = MediaMetadataRetriever()
        val metadataBuilder = MediaMetadata.Builder()
        try {
            retriever.setDataSource(context, uri)
            metadataBuilder.setTitle(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: fallBackName)
            metadataBuilder.setArtist(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST))
            metadataBuilder.setAlbumTitle(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM))
            metadataBuilder.setAlbumArtist(retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST))
            metadataBuilder.setArtworkUri(uri)
            retriever.embeddedPicture?.let {
                metadataBuilder.setArtworkData(it, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
            }
        } catch (_: Exception) {
            metadataBuilder.setTitle(fallBackName)
        } finally {
            retriever.release()
        }
        return metadataBuilder.build()
    }

    private fun getAllAudioFilesRecursive(dir: DocumentFile): List<DocumentFile> {
        val audioFiles = mutableListOf<DocumentFile>()

        dir.listFiles().forEach { file ->
            if (file.isDirectory && file.name?.startsWith(".") != true) {
                audioFiles.addAll(getAllAudioFilesRecursive(file))
            } else {
                if (isAudioFile(file.name ?: "", file.uri.toString())) {
                    audioFiles.add(file)
                }
            }
        }
        return audioFiles
    }


    // --- Player Controls (über Controller) ---

    fun togglePlayPause() {
        playerController?.let {
            if (it.isPlaying) it.pause() else it.play()
        }
    }

    fun skipNext() = playerController?.seekToNext()
    fun skipPrev() = playerController?.seekToPrevious()
    fun seekTo(pos: Long) = playerController?.seekTo(pos)

    fun toggleShuffle() {
        playerController?.shuffleModeEnabled = !(playerController?.shuffleModeEnabled ?: false)
    }

    fun toggleRepeat() {
        playerController?.let { controller ->
            val modes = listOf(Player.REPEAT_MODE_OFF, Player.REPEAT_MODE_ALL, Player.REPEAT_MODE_ONE)
            val current = controller.repeatMode
            val next = modes[(modes.indexOf(current) + 1) % modes.size]
            controller.repeatMode = next
        }
    }

    private fun setPauseAtEndOfMediaItems(pauseAtEnd: Boolean) {
        playerController?.let {
            val args = Bundle().apply {
                putBoolean(CustomCommands.PARAM_PAUSE_AT_END, pauseAtEnd)
            }
            // Erstellen Sie den Befehl
            val command = SessionCommand(CustomCommands.ACTION_TOGGLE_PAUSE_AT_END, args)
            // Senden Sie den Befehl an den Service
            playerController?.sendCustomCommand(command, args)
        }
    }

    fun togglePauseAtEndOfMediaItems() {
        val newPauseAtEnd = _playerStatus.value.pauseAtEndOfMediaItems.not()
        setPauseAtEndOfMediaItems(newPauseAtEnd)
        _playerStatus.value = _playerStatus.value.copy(pauseAtEndOfMediaItems = newPauseAtEnd)
        storageManager.savePauseAtEndOfMediaItems(newPauseAtEnd)
    }

    // --- Helpers ---

    fun requestPauseAtEndStatus() {
        // Den Befehl ohne Argumente erstellen
        val command = CustomCommands.COMMAND_GET_PAUSE_AT_END_STATUS
        val commandFuture = playerController?.sendCustomCommand(command, Bundle.EMPTY)

        // Einen Listener hinzufügen, um auf das Ergebnis zu warten.
        // Wichtig: Der Listener wird auf einem Executor-Thread ausgeführt.
        // UI-Updates müssen zurück auf den Main-Thread.
        commandFuture?.addListener({
            try {
                // Ergebnis aus der Future holen
                val sessionResult = commandFuture.get()

                // Überprüfen, ob der Befehl erfolgreich war
                if (sessionResult.resultCode == SessionResult.RESULT_SUCCESS) {
                    // Das Ergebnis-Bundle auslesen
                    val resultBundle = sessionResult.extras
                    val status = resultBundle.getBoolean(CustomCommands.RESULT_PAUSE_AT_END_STATUS, false)

                    // Den Status im ViewModel aktualisieren (z.B. in einem StateFlow)
                    // Da dies in einem Hintergrund-Thread passiert, verwenden Sie .postValue() für LiveData
                    // oder stellen sicher, dass Ihr StateFlow thread-sicher aktualisiert wird.
                    // Für Compose State ist es am besten, auf den Main-Thread zu wechseln.

                    // Hier Beispielhaft mit einem StateFlow
                    _playerStatus.value = _playerStatus.value.copy(pauseAtEndOfMediaItems = status)

                    Log.d("PlayerViewModel", "Status empfangen: pauseAtEnd = $status")
                }
            } catch (e: Exception) {
                // Fehlerbehandlung (z.B. InterruptedException, ExecutionException)
                Log.e("PlayerViewModel", "Fehler beim Abrufen des Status", e)
            }
        }, ContextCompat.getMainExecutor(context)) // Führt den Listener auf dem Main-Thread aus
    }

    private fun isAudioFile(name: String, path: String): Boolean {
        val lower = name.lowercase()
        val mime = getMimeType(path)
        return mime?.startsWith("audio/") == true || lower.endsWith(".mp3") || lower.endsWith(".wav") || lower.endsWith(".ogg") || lower.endsWith(".m4a") || lower.endsWith(".flac")
    }

    private fun getMimeType(fileUrl: String?): String? {
        val extension = MimeTypeMap.getFileExtensionFromUrl(fileUrl)
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
    }

    private fun restoreLastState() {
        _mediaPlaces.value = storageManager.loadMediaPlaces()

        val lastPathStack = storageManager.loadLastPathStack()
        _currentPathStack.value = lastPathStack
        if (lastPathStack.isNotEmpty()) {
            val lstState = lastPathStack.last()
            openFolder(lstState.uri, lstState.name, false)
        }

        val lastPlayerState = storageManager.loadPlayerStatus()
        lastPlayerState.let {
            _playerStatus.value = _playerStatus.value.copy(
                shuffleMode = it.shuffleMode,
                repeatMode = it.repeatMode,
                pauseAtEndOfMediaItems = it.pauseAtEndOfMediaItems
            )
        }
    }

    override fun onCleared() {
        playerController?.removeListener(controllerListener)

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