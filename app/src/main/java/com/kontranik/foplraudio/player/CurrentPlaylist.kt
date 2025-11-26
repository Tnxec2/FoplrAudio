package com.kontranik.foplraudio.player

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.media3.common.MediaItem
import com.kontranik.foplraudio.R
import com.kontranik.foplraudio.model.PlayerStatus

@Composable
fun CurrentPlaylist(
    status: PlayerStatus,
    onPlayItem: (Int) -> Unit,
    onRemoveItem: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(status.currentIndex) {
        if (status.currentIndex in 0 until status.playlist.size) {
            listState.animateScrollToItem(status.currentIndex)
        }
    }
    
    Column(
        modifier = modifier
    ) {
        if (status.playlist.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(stringResource(R.string.playlist_is_empty))
            }
        } else {
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f, fill = false)
            ) {
                itemsIndexed(status.playlist) { index, item ->
                    val isCurrent = index == status.currentIndex
                    val backgroundColor =
                        if (isCurrent) MaterialTheme.colorScheme.primaryContainer else Color.Transparent

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(backgroundColor)
                            .clickable { onPlayItem(index) }
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (isCurrent) {
                            Icon(
                                Icons.Default.PlayArrow,
                                contentDescription = stringResource( R.string.playing),
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                        }
                        val artworkModifier = Modifier
                            .size(48.dp)
                            .padding(end = 8.dp)
                            .background(Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                        val bitmap = item.mediaMetadata.artworkData?.let {BitmapFactory.decodeByteArray(it, 0, it.size) }?.asImageBitmap()
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap,
                                contentDescription = stringResource(R.string.cover_art),
                                modifier = artworkModifier,
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                Icons.Default.MusicNote,
                                contentDescription = stringResource(R.string.music_note),
                                modifier = artworkModifier.padding(8.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
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
                                Icons.Default.Close,
                                contentDescription = stringResource(R.string.remove),
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

@Preview(showBackground = true)
@Composable
private fun CurrentPlaylistPreview() {
    val mediaItem1 = MediaItem.Builder()
        .setMediaMetadata(
            androidx.media3.common.MediaMetadata.Builder()
                .setTitle("The First Song Title")
                .setArtist("Artist One")
                .build()
        )
        .build()

    val mediaItem2 = MediaItem.Builder()
        .setMediaMetadata(
            androidx.media3.common.MediaMetadata.Builder()
                .setTitle("A Much Longer Song Title That Will Definitely Overflow The Available Space")
                .setArtist("Artist Two")
                .build()
        )
        .build()

    val mediaItem3 = MediaItem.Builder()
        .setMediaMetadata(
            androidx.media3.common.MediaMetadata.Builder()
                .setTitle("The Third Song")
                .setArtist("Artist Three")
                .build()
        )
        .build()

    val playlist = listOf(mediaItem1, mediaItem2, mediaItem3)
    val status = PlayerStatus(
        playlist = playlist,
        currentIndex = 1,
        isPlaying = true
    )

    MaterialTheme {
        CurrentPlaylist(
            status = status,
            onPlayItem = {},
            onRemoveItem = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun CurrentPlaylistEmptyPreview() {
    MaterialTheme {
        CurrentPlaylist(
            status = PlayerStatus(playlist = emptyList()),
            onPlayItem = {},
            onRemoveItem = {}
        )
    }
}