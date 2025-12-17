package com.kontranik.foplraudio.ui.player

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.annotation.RequiresApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
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
@OptIn(UnstableApi::class)
class PlayerViewModel(
    private val context: Context,
    private val storageManager: StorageManager
) : ViewModel() {

    var playerController: MediaController? = null
    private var controllerFuture: ListenableFuture<MediaController>? = null

    private val _playerStatus = MutableStateFlow(PlayerStatus())
    val playerStatus = _playerStatus.asStateFlow()

    private val _showPlayList = MutableStateFlow<Boolean>(false)
    val showPlayList = _showPlayList.asStateFlow()

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

        restoreLastState(context)
        initController(context)

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

    private fun initController(context: Context) {
        Log.d("NIK", "initController")
        val sessionToken =
            SessionToken(
                context,
                ComponentName(context, PlaybackService::class.java)
            )

        controllerFuture = MediaController.Builder(context, sessionToken).buildAsync()

        controllerFuture?.addListener({
            try {
                Log.d("NIK", "initController.ready")
                val controller = controllerFuture?.get()
                playerController = controller
                playerController?.addListener( controllerListener)
                initControllerState()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }, MoreExecutors.directExecutor())
    }

    private fun initControllerState() {

        Log.d("NIK", "initControllerState")
        playerController?.let { controller ->
            _playerStatus.value = _playerStatus.value.copy(
                isPlaying = controller.isPlaying,
                duration = controller.duration.coerceAtLeast(0L)
            )
            controller.shuffleModeEnabled = _playerStatus.value.shuffleMode
            controller.repeatMode = _playerStatus.value.repeatMode
            setPauseAtEndOfMediaItems(_playerStatus.value.pauseAtEndOfMediaItems)

            if (controller.mediaItemCount == 0) {
                viewModelScope.launch {
                    loadAndPrepareLastPlaylist(controller)
                }
            } else {
                updatePlaylistState(true)
            }
            updateCurrentTrackInfo(controller.currentMediaItem)
        }
    }

    private suspend fun loadAndPrepareLastPlaylist(controller: MediaController) {
        // Zeigen Sie optional einen Ladeindikator an
        // _playerStatus.update { it.copy(isLoading = true) }

        var currentIndex = 0
        val lastPlaylist = withContext(Dispatchers.IO) {
            // I/O-Operation im Hintergrund
            val pl = storageManager.loadLastPlaylist()
            currentIndex = pl.currentIndex
            return@withContext  pl.playlistUris.map { item ->
                MediaItem.Builder()
                    .setUri(item.uri)
                    .setMediaId(item.uri.toString())
                    .setMediaMetadata(getMetadata(context, item.uri, item.title))
                    .build()
            }
        }
        Log.d("NIK", "loadAndPrepareLastPlaylist. lastPlaylist size: ${lastPlaylist.size}")

        // Zurück zum Main-Thread, um den Controller zu aktualisieren
        if (lastPlaylist.isNotEmpty()) {
            controller.setMediaItems(
                lastPlaylist,currentIndex, 0L)
            controller.prepare()
        }

        // Deaktivieren Sie den Ladeindikator
        // _playerStatus.update { it.copy(isLoading = false) }
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
            position = playerController?.currentPosition?.coerceAtLeast(0L) ?: 0L,
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

    private fun updatePlaylistState(init: Boolean = false) {
        playerController?.let { controller ->
            val items = mutableListOf<MediaItem>()
            var isPlaylistSame = controller.mediaItemCount == _playerStatus.value.playlist.size

            for (i in 0 until controller.mediaItemCount) {
                val item = controller.getMediaItemAt(i)
                if (isPlaylistSame && _playerStatus.value.playlist[i].mediaId != item.mediaId) {
                    isPlaylistSame = false
                }
                items.add(item)
            }

            if (!isPlaylistSame) {
                _playerStatus.value = _playerStatus.value.copy(
                    playlist = items,
                    currentIndex = controller.currentMediaItemIndex
                )
                if (!init) storageManager.savePlaylist(items)
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

    fun addFolder(context: Context, uri: Uri) {
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

    fun openFolder(context: Context, folderUri: Uri, folderName: String, saveStack: Boolean = true) {

        Log.d("NIK", "openFolder")
        _currentFiles.value = emptyList()

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
                    .sortedWith(compareBy({ !it.isDirectory }, { it.name.lowercase() }))

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

    fun navigateBack(context: Context) {
        if (_showPlayList.value) {
            _showPlayList.value = false
            return
        }
        val stack = _currentPathStack.value
        if (stack.isNotEmpty()) {
            val newStack = stack.dropLast(1)
            _currentPathStack.value = newStack
            if (newStack.isNotEmpty()) {
                val previousFolder = newStack.last()
                openFolder(context, previousFolder.uri, previousFolder.name)
            }
        }
    }

    fun playFile(context: Context, selectedFile: FileItem, allFiles: List<FileItem>) {
        val audioFiles = allFiles.filter { !it.isDirectory }
        viewModelScope.launch(Dispatchers.IO) {
            val mediaItems = audioFiles.map { fileItem ->
                MediaItem.Builder()
                    .setUri(fileItem.uri)
                    .setMediaId(fileItem.uri.toString())
                    .setMediaMetadata(getMetadata(context, fileItem.uri, fileItem.name))
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

    fun playFolderRecursive(context: Context, folderUri: Uri) {
        playFolderRecursive(context, folderUri, true)
    }

    fun addFolderRecursive(context: Context,folderUri: Uri) {
        playFolderRecursive(context, folderUri, false)
    }

    private fun playFolderRecursive(context: Context, folderUri: Uri, replace: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            val docFile = DocumentFile.fromTreeUri(context, folderUri)
            val files = if (docFile != null) getAllAudioFilesRecursive(docFile) else emptyList()
            loadMediaItems(context, files, replace)
        }
    }

    private suspend fun loadMediaItems(context: Context, files: List<DocumentFile>, replace: Boolean = false) {
        val mediaItems = files.map { file ->
            MediaItem.Builder()
                .setUri(file.uri)
                .setMediaId(file.uri.toString())
                .setMediaMetadata(getMetadata(context, file.uri, file.name ?: context.getString(R.string.unknown)))
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

    private fun getMetadata(context: Context, uri: Uri, fallBackName: String) : MediaMetadata {
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

    fun togglePlaylistShow() {
        _showPlayList.value = !_showPlayList.value
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

    private fun isAudioFile(name: String, path: String): Boolean {
        val lower = name.lowercase()
        val mime = getMimeType(path)
        return mime?.startsWith("audio/") == true || lower.endsWith(".mp3") || lower.endsWith(".wav") || lower.endsWith(".ogg") || lower.endsWith(".m4a") || lower.endsWith(".flac")
    }

    private fun getMimeType(fileUrl: String?): String? {
        val extension = MimeTypeMap.getFileExtensionFromUrl(fileUrl)
        return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)
    }

    private fun restoreLastState(context: Context) {
        Log.d("NIK", "restoreLastState")
        _mediaPlaces.value = storageManager.loadMediaPlaces()

        val lastPathStack = storageManager.loadLastPathStack()
        _currentPathStack.value = lastPathStack
        if (lastPathStack.isNotEmpty()) {
            val lstState = lastPathStack.last()
            openFolder(context, lstState.uri, lstState.name, false)
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
        clearController()

        super.onCleared()
    }

    fun clearController() {
        playerController?.removeListener(controllerListener)
    }
}
