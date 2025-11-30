package com.kontranik.foplraudio.ui


import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kontranik.foplraudio.AppViewModelProvider
import com.kontranik.foplraudio.R
import com.kontranik.foplraudio.model.FileItem
import com.kontranik.foplraudio.model.MediaPlace
import com.kontranik.foplraudio.model.PlayerStatus
import com.kontranik.foplraudio.ui.player.PlayerBar
import com.kontranik.foplraudio.ui.player.PlayerViewModel
import com.kontranik.foplraudio.ui.screen.CurrentPlaylist
import com.kontranik.foplraudio.ui.screen.FileBrowserScreen
import com.kontranik.foplraudio.ui.screen.FolderListScreen

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.P)
@Composable
fun AudioPlayerApp() {
    val context = LocalContext.current
    val viewModel: PlayerViewModel = viewModel(factory = AppViewModelProvider.Factory)

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    val folders by viewModel.mediaPlaces.collectAsState()
    val currentPathStack by viewModel.currentPathStack.collectAsState()
    val currentFiles by viewModel.currentFiles.collectAsState()

    val folderPickerLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            uri?.let { viewModel.addFolder(context, it) }
        }

    val status by viewModel.playerStatus.collectAsState()

    val showPlaylist by viewModel.showPlayList.collectAsState()

    val title = if (showPlaylist) {
        stringResource(R.string.current_playlist)
    } else if (currentPathStack.isEmpty()) {
        stringResource(R.string.music_places)
    } else {
        currentPathStack.last().name
    }

    Scaffold(
        bottomBar = {
            if (!isLandscape) PlayerBar(
                viewModel,
                togglePlaylist = { viewModel.togglePlaylistShow() })
        },
        modifier = Modifier.safeDrawingPadding()
    ) { innerPadding ->
        Box(modifier = Modifier
            .padding(innerPadding)
            .fillMaxSize()) {
            if (isLandscape) {
                LandscapeLayout(
                    showPlaylist = showPlaylist,
                    togglePlaylist = { viewModel.togglePlaylistShow() },
                    title = title,
                    folders = folders,
                    currentPathStack = currentPathStack,
                    currentFiles = currentFiles,
                    status = status,
                    navigateBack = { viewModel.navigateBack(context) },
                    addFolder = { folderPickerLauncher.launch(null) },
                    playQueueItem = { viewModel.playQueueItem(it) },
                    removeQueueItem = { viewModel.removeQueueItem(it) },
                    openFolder = { uri, name ->
                        viewModel.openFolder(context, uri, name)
                    },
                    removeFolder = { folder ->
                        viewModel.removeFolder(folder)
                    },
                    playFile = { file, files ->
                        viewModel.playFile(context, file, files)
                    },
                    playFolderRecursive = {
                        viewModel.playFolderRecursive(context, it)
                    },
                    addFolderRecursive = {
                        viewModel.addFolderRecursive(context, it)
                    },
                    playerBar = {
                        PlayerBar(
                            viewModel,
                            stretchArt = true,
                            togglePlaylist = { viewModel.togglePlaylistShow() })
                    }
                )
            } else {
                PortraitLayout(
                    showPlaylist = showPlaylist,
                    togglePlaylist = { viewModel.togglePlaylistShow() },
                    title = title,
                    folders = folders,
                    currentPathStack = currentPathStack,
                    currentFiles = currentFiles,
                    status = status,
                    navigateBack = { viewModel.navigateBack(context) },
                    addFolder = { folderPickerLauncher.launch(null) },
                    playQueueItem = { viewModel.playQueueItem(it) },
                    removeQueueItem = { viewModel.removeQueueItem(it) },
                    openFolder = { uri, name ->
                        viewModel.openFolder(context, uri, name)
                    },
                    removeFolder = { folder ->
                        viewModel.removeFolder(folder)
                    },
                    playFile = { file, files ->
                        viewModel.playFile(context, file, files)
                    },
                    playFolderRecursive = {
                        viewModel.playFolderRecursive(context, it)
                    },
                    addFolderRecursive = {
                        viewModel.addFolderRecursive(context, it)
                    }
                )
            }
        }
    }

    BackHandler(enabled = currentPathStack.isNotEmpty()) {
        viewModel.navigateBack(context)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortraitLayout(
    showPlaylist: Boolean,
    togglePlaylist: () -> Unit,
    title: String,
    folders: List<MediaPlace>,
    currentPathStack: List<FileItem>,
    currentFiles: List<FileItem>,
    status: PlayerStatus,
    navigateBack: () -> Unit,
    addFolder: () -> Unit,
    playQueueItem: (Int) -> Unit,
    removeQueueItem: (Int) -> Unit,
    openFolder: (Uri, String) -> Unit,
    playFolderRecursive: (Uri) -> Unit,
    addFolderRecursive: (Uri) -> Unit,
    playFile: (FileItem, List<FileItem>) -> Unit,
    removeFolder: (MediaPlace) -> Unit,
    modifier: Modifier = Modifier
) {

    Column(
        modifier = modifier
    ) {
        TopAppBar(
            title = {
                Text(
                    text = title,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            },
            navigationIcon = {
                if (currentPathStack.isNotEmpty()) {
                    if (showPlaylist.not()) {
                        IconButton(onClick = {
                            navigateBack()
                        }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = stringResource(
                                    R.string.back
                                )
                            )
                        }
                    }
                }
            },
            actions = {
                if (!showPlaylist) {
                    if (currentPathStack.isEmpty()) {
                        IconButton(onClick = { addFolder() }) {
                            Icon(
                                Icons.Default.Add,
                                contentDescription = stringResource(R.string.add_place)
                            )
                        }
                    }
                }
                IconButton(onClick = { togglePlaylist() }) {
                    Icon(
                        if (!showPlaylist) Icons.AutoMirrored.Filled.List else Icons.Default.Close,
                        contentDescription = stringResource(R.string.current_playlist))
                }
            }
        )

        if (showPlaylist) {
            CurrentPlaylist(
                status = status,
                onPlayItem = { playQueueItem(it) },
                onRemoveItem = { removeQueueItem(it) }
            )
        } else if (currentPathStack.isEmpty()) {
            FolderListScreen(
                folders = folders,
                onFolderClick = { uriStr, name ->
                    openFolder(uriStr.toUri(), name)
                },
                onRemoveFolder = { folder ->
                    removeFolder(folder)
                }
            )
        } else {
            FileBrowserScreen(
                files = currentFiles,
                onFileClick = { file ->
                    if (file.isDirectory) {
                        openFolder(file.uri, file.name)
                    } else {
                        playFile(file, currentFiles)
                    }
                },
                onContextPlayFolder = { folderUri ->
                    playFolderRecursive(folderUri)
                },
                onContextAddToPlayFolder = { folderUri ->
                    addFolderRecursive(folderUri)
                },
                onPlayAllRecursive = {
                    val currentFolder = currentPathStack.lastOrNull()
                    if (currentFolder != null) {
                        playFolderRecursive(currentFolder.uri)
                    }
                }
            )
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LandscapeLayout(
    showPlaylist: Boolean,
    togglePlaylist: () -> Unit,
    title: String,
    folders: List<MediaPlace>,
    currentPathStack: List<FileItem>,
    currentFiles: List<FileItem>,
    status: PlayerStatus,
    navigateBack: () -> Unit,
    addFolder: () -> Unit,
    playQueueItem: (Int) -> Unit,
    removeQueueItem: (Int) -> Unit,
    openFolder: (Uri, String) -> Unit,
    playFolderRecursive: (Uri) -> Unit,
    addFolderRecursive: (Uri) -> Unit,
    playFile: (FileItem, List<FileItem>) -> Unit,
    removeFolder: (MediaPlace) -> Unit,
    modifier: Modifier = Modifier,
    playerBar: @Composable () -> Unit
) {

    Row(
        modifier = modifier.fillMaxSize()
    ) {
        Column(
            Modifier.weight(1f)
        ) {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                navigationIcon = {
                    if (currentPathStack.isNotEmpty()) {
                        if (showPlaylist.not()) {
                            IconButton(onClick = {
                                navigateBack()
                            }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = stringResource(
                                        R.string.back
                                    )
                                )
                            }
                        }
                    }
                },
                actions = {
                    if (!showPlaylist) {
                        if (currentPathStack.isEmpty()) {
                            IconButton(onClick = { addFolder() }) {
                                Icon(
                                    Icons.Default.Add,
                                    contentDescription = stringResource(R.string.add_place)
                                )
                            }
                        }
                    }
                    IconButton(onClick = { togglePlaylist() }) {
                        Icon(
                            if (!showPlaylist) Icons.AutoMirrored.Filled.List else Icons.Default.Close,
                            contentDescription = stringResource(R.string.current_playlist))
                    }
                }
            )
            if (showPlaylist) {
                CurrentPlaylist(
                    status = status,
                    onPlayItem = { playQueueItem(it) },
                    onRemoveItem = { removeQueueItem(it) },
                )
            } else if (currentPathStack.isEmpty()) {
                FolderListScreen(
                    folders = folders,
                    onFolderClick = { uriStr, name ->
                        openFolder(uriStr.toUri(), name)
                    },
                    onRemoveFolder = { folder ->
                        removeFolder(folder)
                    }
                )
            } else {
                FileBrowserScreen(
                    files = currentFiles,
                    onFileClick = { file ->
                        if (file.isDirectory) {
                            openFolder(file.uri, file.name)
                        } else {
                            playFile(file, currentFiles)
                        }
                    },
                    onContextPlayFolder = { folderUri ->
                        playFolderRecursive(folderUri)
                    },
                    onContextAddToPlayFolder = { folderUri ->
                        addFolderRecursive(folderUri)
                    },
                    onPlayAllRecursive = {
                        val currentFolder = currentPathStack.lastOrNull()
                        if (currentFolder != null) {
                            playFolderRecursive(currentFolder.uri)
                        }
                    }
                )
            }
        }
        Column(
            Modifier.weight(1f)
        ) {
            playerBar()
        }
    }
}
