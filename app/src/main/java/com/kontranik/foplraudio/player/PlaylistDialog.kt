package com.kontranik.foplraudio.player

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.kontranik.foplraudio.model.PlayerStatus


@Composable
fun PlaylistDialog(
    status: PlayerStatus,
    onDismiss: () -> Unit,
    onPlayItem: (Int) -> Unit,
    onRemoveItem: (Int) -> Unit
) {
    val listState = rememberLazyListState()

    LaunchedEffect(status.currentIndex) {
        listState.animateScrollToItem(status.currentIndex)
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 500.dp),
            shape = RoundedCornerShape(16.dp),
        ) {
            Column {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Aktuelle Wiedergabeliste",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(16.dp)
                    )
                    IconButton(
                        onClick = {onDismiss()}
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Schließen",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                HorizontalDivider()

                if (status.playlist.isEmpty()) {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("Die Liste ist leer")
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        itemsIndexed(status.playlist) { index, item ->
                            val isCurrent = index == status.currentIndex
                            val backgroundColor = if (isCurrent) MaterialTheme.colorScheme.primaryContainer else Color.Transparent

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(backgroundColor)
                                    .clickable { onPlayItem(index) }
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (isCurrent) {
                                    Icon(
                                        Icons.Default.PlayArrow,
                                        contentDescription = "Playing",
                                        modifier = Modifier.size(16.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                }

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.mediaMetadata.title.toString(),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                    )
                                    if (item.mediaMetadata.artist != null)
                                        Text(
                                            text = item.mediaMetadata.artist.toString(),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                }

                                IconButton(onClick = { onRemoveItem(index) }) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Entfernen",
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f))
                        }
                    }
                }
            }
        }
    }
}