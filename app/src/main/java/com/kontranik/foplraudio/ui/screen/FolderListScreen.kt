package com.kontranik.foplraudio.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.FolderDelete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.kontranik.foplraudio.R
import com.kontranik.foplraudio.model.MediaPlace
import com.kontranik.foplraudio.ui.theme.FoplrAudioTheme

@Composable
fun FolderListScreen(
    folders: List<MediaPlace>,
    onFolderClick: (String, String) -> Unit,
    onRemoveFolder: (MediaPlace) -> Unit,
    onAddFolder: () -> Unit,
) {
    if (folders.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            OutlinedButton(
                onClick = {
                    onAddFolder()
                }
            ) {
                Text(stringResource(R.string.press_to_add_a_music_folder), style = MaterialTheme.typography.bodyLarge)
            }
        }
    } else {
        LazyColumn {
            items(folders) { folder ->
                FolderListItem(folder, onFolderClick, onRemoveFolder)
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun FolderListItem(
    folder: MediaPlace,
    onFolderClick: (String, String) -> Unit,
    onRemoveFolder: (MediaPlace) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    ListItem(
        headlineContent = { Text(folder.name) },
        leadingContent = {
            Icon(
                Icons.Default.Folder,
                contentDescription = null
            )
        },
        modifier = Modifier.clickable { onFolderClick(folder.uriString, folder.name) },
        trailingContent = {
            Box {
                IconButton(onClick = { expanded = true }) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = stringResource(R.string.options)
                    )
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    DropdownMenuItem(
                        leadingIcon = {
                            Icon(
                                Icons.Filled.FolderDelete,
                                contentDescription = stringResource(R.string.remove)
                            )
                        },
                        text = { Text(stringResource(R.string.remove)) },
                        onClick = {
                            expanded = false
                            onRemoveFolder(folder)
                        }
                    )
                }
            }
        }
    )
}


@Preview(showBackground = true)
@Composable
fun FolderListScreenWithFoldersPreview() {
    // Beispieldaten für die Vorschau
    val sampleFolders = listOf(
        MediaPlace(name = "My Music Collection", uriString = "uri/music"),
        MediaPlace(name = "Podcasts", uriString = "uri/podcasts"),
        MediaPlace(name = "Downloaded Tracks", uriString = "uri/downloads")
    )
    FoplrAudioTheme(darkTheme = false) { // Umschließen Sie die Vorschau mit Ihrem App-Theme
        Surface() {
            FolderListScreen(
                folders = sampleFolders,
                onFolderClick = { _, _ -> },
                onRemoveFolder = {},
                onAddFolder = {}
            )
        }
    }
}

// 2. Vorschau für den leeren Bildschirm
@Preview(showBackground = true)
@Composable
fun FolderListScreenEmptyPreview() {
    FoplrAudioTheme(dynamicColor = false) {
        Surface() {
            FolderListScreen(
                folders = emptyList(), // Leere Liste, um den "Hinzufügen"-Button zu zeigen
                onFolderClick = { _, _ -> },
                onRemoveFolder = {},
                onAddFolder = {}
            )
        }
    }
}

// 3. Vorschau für ein einzelnes Listenelement (optional, aber nützlich)
@Preview(showBackground = true)
@Composable
fun FolderListItemPreview() {
    val sampleFolder = MediaPlace(name = "Classical Music", uriString = "uri/classical")
    FoplrAudioTheme(dynamicColor = false) {
        Surface() {
            FolderListItem(
                folder = sampleFolder,
                onFolderClick = { _, _ -> },
                onRemoveFolder = {}
            )
        }
    }
}
