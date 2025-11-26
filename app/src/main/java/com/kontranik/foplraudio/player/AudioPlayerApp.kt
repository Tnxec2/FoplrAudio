package com.kontranik.foplraudio.player


import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.List
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.core.net.toUri

@OptIn(ExperimentalMaterial3Api::class)
@RequiresApi(Build.VERSION_CODES.P)
@Composable
fun AudioPlayerApp() {
    val context = LocalContext.current
    val viewModel: PlayerViewModel = viewModel(factory = PlayerViewModelFactory(context))

    val folders by viewModel.folders.collectAsState()
    val currentPathStack by viewModel.currentPathStack.collectAsState()
    val currentFiles by viewModel.currentFiles.collectAsState()

    val folderPickerLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
            uri?.let { viewModel.addFolder(it) }
        }

    var showPlaylist by remember { mutableStateOf(false) }
    val status by viewModel.playerStatus.collectAsState()

    val title = if (currentPathStack.isEmpty()) {
                "Musikordner"
            } else {
                if (showPlaylist)
                    "Wiedergabeliste"
                else
                    currentPathStack.last().name
            }

    Scaffold(
        bottomBar = {
            PlayerBar(viewModel)
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).fillMaxSize()) {

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
                        IconButton(onClick = {
                            viewModel.navigateBack()
                        }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Zurück")
                        }
                    }
                },
                actions = {
                    if (currentPathStack.isEmpty()) {
                        IconButton(onClick = { folderPickerLauncher.launch(null) }) {
                            Icon(Icons.Default.Add, contentDescription = "Ordner hinzufügen")
                        }
                    }
                    IconButton(onClick = { showPlaylist = !showPlaylist }) {
                        Icon(Icons.AutoMirrored.Filled.List, contentDescription = "Wiedergabeliste")
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
                        viewModel.playFolderContents(folderUri)
                    },
                    onPlayAllRecursive = {
                        val currentFolder = currentPathStack.lastOrNull()
                        if (currentFolder != null) {
                            viewModel.playFolderRecursive(currentFolder.uri)
                        }
                    }
                )
            }
        }
    }

    androidx.activity.compose.BackHandler(enabled = currentPathStack.isNotEmpty()) {
        viewModel.navigateBack()
    }
}