package com.kontranik.foplraudio.player


import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kontranik.foplraudio.R

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.P)
@Composable
fun AudioPlayerApp() {
    val context = LocalContext.current
    val viewModel: PlayerViewModel = viewModel(factory = PlayerViewModelFactory(context))

    val folders by viewModel.mediaPlaces.collectAsState()
    val currentPathStack by viewModel.currentPathStack.collectAsState()
    val currentFiles by viewModel.currentFiles.collectAsState()

    val folderPickerLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            uri?.let { viewModel.addFolder(it) }
        }

    var showPlaylist by remember { mutableStateOf(false) }
    val status by viewModel.playerStatus.collectAsState()
    val isInitializing by viewModel.isInitializing.collectAsState()

    val title = if (currentPathStack.isEmpty()) {
                stringResource(R.string.music_places)
            } else {
                if (showPlaylist)
                    stringResource(R.string.current_playlist)
                else
                    currentPathStack.last().name
            }

    Scaffold(
        bottomBar = {
            PlayerBar(viewModel)
        },
        modifier = Modifier.safeDrawingPadding()
    ) { innerPadding ->
        Column(modifier = Modifier
            .padding(innerPadding)
            .fillMaxSize()) {

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
                                viewModel.navigateBack()
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
                    if (currentPathStack.isEmpty()) {
                        IconButton(onClick = { folderPickerLauncher.launch(null) }) {
                            Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_place))
                        }
                    }
                    IconButton(onClick = { showPlaylist = !showPlaylist }) {
                        Icon(
                            if (!showPlaylist) Icons.AutoMirrored.Filled.List else Icons.Default.Close,
                            contentDescription = stringResource(R.string.current_playlist))
                    }
                }
            )

            if (showPlaylist) {
                CurrentPlaylist(
                    status = status,
                    onPlayItem = { viewModel.playQueueItem(it) },
                    onRemoveItem = { viewModel.removeQueueItem(it) }
                )
            } else if (currentPathStack.isEmpty()) {
                FolderListScreen(
                    folders = folders,
                    onFolderClick = { uriStr, name ->
                        viewModel.openFolder(uriStr.toUri(), name)
                    },
                    onRemoveFolder = { folder ->
                        viewModel.removeFolder(folder)
                    }
                )
            } else {
                FileBrowserScreen(
                    files = currentFiles,
                    onFileClick = { file ->
                        if (file.isDirectory) {
                            viewModel.openFolder(file.uri, file.name)
                        } else {
                            viewModel.playFile(file, currentFiles)
                        }
                    },
                    onContextPlayFolder = { folderUri ->
                        viewModel.playFolderRecursive(folderUri)
                    },
                    onContextAddToPlayFolder = { folderUri ->
                        viewModel.addFolderRecursive(folderUri)
                    },
                    onPlayAllRecursive = {
                        val currentFolder = currentPathStack.lastOrNull()
                        if (currentFolder != null) {
                            viewModel.playFolderRecursive(currentFolder.uri)
                        }
                    }
                )
            }
            
            if (isInitializing) {
                LoadingScreen()
            }
        }
    }

    androidx.activity.compose.BackHandler(enabled = currentPathStack.isNotEmpty()) {
        viewModel.navigateBack()
    }
}

@Composable
fun LoadingScreen() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text("Load saved state...")
        }
    }
}