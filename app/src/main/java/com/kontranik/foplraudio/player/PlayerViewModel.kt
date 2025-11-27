package com.kontranik.foplraudio.player

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.webkit.MimeTypeMap
import android.widget.Toast
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
import androidx.media3.common.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaController
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
    private val _mediaPlaces = MutableStateFlow<List<MediaPlace>>(emptyList())
    val mediaPlaces = _mediaPlaces.asStateFlow()

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
        _mediaPlaces.value = storageManager.loadMediaPlaces()

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
            storageManager.saveShuffleMode(shuffleModeEnabled)
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            _playerStatus.value = _playerStatus.value.copy(repeatMode = repeatMode)
            storageManager.saveRepeatMode(repeatMode = repeatMode)
        }
    }

    private fun updateControllerState() {
        mediaController?.let { controller ->
            _playerStatus.value = _playerStatus.value.copy(
                isPlaying = controller.isPlaying,
//                shuffleMode = controller.shuffleModeEnabled,
//                repeatMode = controller.repeatMode,
                duration = controller.duration.coerceAtLeast(0L)
            )
            controller.shuffleModeEnabled = _playerStatus.value.shuffleMode
            controller.repeatMode = _playerStatus.value.repeatMode
            directExoPlayer.value?.pauseAtEndOfMediaItems = _playerStatus.value.pauseAtEndOfMediaItems

            updateCurrentTrackInfo(controller.currentMediaItem)
            updatePlaylistState()
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
                MediaItem.Builder()
                    .setUri(fileItem.uri)
                    .setMediaId(fileItem.uri.toString())
                    .setMediaMetadata(getMetadata(fileItem.uri, fileItem.name))
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
                if (replace) mediaController?.setMediaItems(mediaItems, 0, 0L)
                else  mediaController?.addMediaItems(mediaItems)
                mediaController?.prepare()
                mediaController?.play()
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
            val artworkBytes = retriever.embeddedPicture
            artworkBytes?.let {
                metadataBuilder.setArtworkData(it, MediaMetadata.PICTURE_TYPE_FRONT_COVER)
            }
        } catch (e: Exception) {
            Log.e("PlayerViewModel", "Error getting metadata for $uri", e)
            metadataBuilder.setTitle(fallBackName)
        } finally {
            retriever.release()
        }
        return metadataBuilder.build()
    }

    private fun getAllAudioFilesRecursive(dir: DocumentFile): List<DocumentFile> {
        val audioFiles = mutableListOf<DocumentFile>()
        val files = dir.listFiles()
        for (file in files) {
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

    fun togglePauseAtEndOfMediaItems() {
        directExoPlayer.value?.let {
            val pauseAtEndOfMediaItems = it.pauseAtEndOfMediaItems.not()
            it.pauseAtEndOfMediaItems = pauseAtEndOfMediaItems
            _playerStatus.value = _playerStatus.value.copy(
                pauseAtEndOfMediaItems = pauseAtEndOfMediaItems
            )
            storageManager.savePauseAtEndOfMediaItems(pauseAtEndOfMediaItems)
        }
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

    private fun restoreLastState() {
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
            directExoPlayer.value?.shuffleModeEnabled = it.shuffleMode
            directExoPlayer.value?.repeatMode = it.repeatMode
            directExoPlayer.value?.pauseAtEndOfMediaItems = it.pauseAtEndOfMediaItems
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