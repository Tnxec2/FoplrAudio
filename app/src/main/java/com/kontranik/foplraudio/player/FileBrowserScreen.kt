package com.kontranik.foplraudio.player

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistAdd
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.kontranik.foplraudio.R
import com.kontranik.foplraudio.model.FileItem

@Composable
fun FileBrowserScreen(
    files: List<FileItem>,
    onFileClick: (FileItem) -> Unit,
    onContextPlayFolder: (Uri) -> Unit,
    onContextAddToPlayFolder: (Uri) -> Unit,
    onPlayAllRecursive: () -> Unit
) {
    Column {
        if (files.find { it.isDirectory } != null) {
            Button(
                onClick = onPlayAllRecursive,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.play_all_recursive))
            }
        }

        LazyColumn(modifier = Modifier.weight(1f)) {
            items(files) { file ->
                var expanded by remember { mutableStateOf(false) }
                val metadata = rememberMetadata(file)

                ListItem(
                    headlineContent = {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                metadata.value?.title ?: file.name,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (!file.isDirectory) {
                                metadata.value?.artist?.let { artist ->
                                    Text(
                                        artist,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                if (file.name != (metadata.value?.title ?: file.name)) {
                                    Text(
                                        file.name,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                        }
                    },
                    leadingContent = {
                        metadata.value?.bitmap?.let {
                            Image(
                                bitmap = it,
                                contentDescription = stringResource(R.string.cover_art),
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                contentScale = androidx.compose.ui.layout.ContentScale.Crop
                            )
                        } ?:
                            if (file.isDirectory)
                                Icon(Icons.Default.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                            else
                                Icon(Icons.Default.AudioFile, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)

                    },
                    modifier = Modifier.clickable {
                        if (file.isDirectory) {
                            onFileClick(file)
                        } else {
                            onFileClick(file)
                        }
                    },
                    trailingContent = {
                        if (file.isDirectory) {
                            Box {
                                IconButton(onClick = { expanded = true }) {
                                    Icon(Icons.Default.MoreVert, contentDescription = stringResource(
                                        R.string.options
                                    ))
                                }
                                DropdownMenu(
                                    expanded = expanded,
                                    onDismissRequest = { expanded = false }
                                ) {
                                    DropdownMenuItem(
                                        leadingIcon = { Icon(Icons.Default.PlayArrow, contentDescription = null) },
                                        text = { Text(stringResource(R.string.play_folder)) },
                                        onClick = {
                                            expanded = false
                                            onContextPlayFolder(file.uri)
                                        }
                                    )
                                    DropdownMenuItem(
                                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = null) },
                                        text = { Text(stringResource(R.string.add_folder_to_playlist)) },
                                        onClick = {
                                            expanded = false
                                            onContextAddToPlayFolder(file.uri)
                                        }
                                    )
                                }
                            }
                        }
                    }
                )
                HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f))
            }
        }
    }
}