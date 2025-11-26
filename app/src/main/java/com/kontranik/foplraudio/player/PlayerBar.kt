package com.kontranik.foplraudio.player

import android.graphics.BitmapFactory
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayDisabled
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import com.kontranik.foplraudio.model.PlayerStatus
import com.kontranik.foplraudio.player.helpers.formatDuration


@RequiresApi(Build.VERSION_CODES.P)
@Composable
fun PlayerBar(
    viewModel: PlayerViewModel
) {
    val status by viewModel.playerStatus.collectAsState()

    val imageBitmap = remember(status.currentArtworkBytes) {
        status.currentArtworkBytes?.let { bytes ->
            try {
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size)?.asImageBitmap()
            } catch (e: Exception) {
                null
            }
        }
    }

    PlayerBarContent(
        status,
        imageBitmap,
        seekTo = { viewModel.seekTo(it) },
        togglePauseAtEndOfMediaItems = { viewModel.togglePauseAtEndOfMediaItems() },
        skipPrev = { viewModel.skipPrev() },
        skipNext = { viewModel.skipNext() },
        togglePlayPause = { viewModel.togglePlayPause() },
        toggleShuffle = { viewModel.toggleShuffle() },
        toggleRepeat = { viewModel.toggleRepeat() },
        fallBackIcon = viewModel.getTrackIconFallback(status.currentTrackTitle),
    )
}

@Composable
private fun PlayerBarContent(
    status: PlayerStatus,
    imageBitmap: ImageBitmap?,
    fallBackIcon: ImageVector,
    seekTo: (Long) -> Unit,
    togglePauseAtEndOfMediaItems: () -> Unit,
    skipPrev: () -> Unit,
    skipNext: () -> Unit,
    toggleShuffle: () -> Unit,
    toggleRepeat: () -> Unit,
    togglePlayPause: () -> Unit,

    ) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(8.dp)
    ) {
        if (status.duration > 0) {

            Slider(
                value = status.position.toFloat(),
                onValueChange = { seekTo(it.toLong()) },
                valueRange = 0f..status.duration.toFloat(),
                modifier = Modifier.height(20.dp)
            )
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(formatDuration(status.position), style = MaterialTheme.typography.labelSmall)
                Text(formatDuration(status.duration), style = MaterialTheme.typography.labelSmall)
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val artworkModifier = Modifier
                .size(48.dp)
                .padding(end = 8.dp)
                .background(Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(4.dp))

            if (imageBitmap != null) {
                Image(
                    bitmap = imageBitmap,
                    contentDescription = "Cover Art",
                    modifier = artworkModifier,
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    fallBackIcon,
                    contentDescription = "Medienart",
                    modifier = artworkModifier.padding(8.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(Modifier.weight(1f)) {
                Text(
                    text = status.currentTrackTitle.ifEmpty { "Kein Titel" },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Text(
                    text = status.currentTrackArtist ?: "",
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {

            IconButton(onClick = { skipPrev() }) {
                Icon(Icons.Default.SkipPrevious, contentDescription = "Zurück")
            }

            IconButton(
                onClick = { togglePlayPause() },
                modifier = Modifier
                    .size(48.dp)
                    .background(MaterialTheme.colorScheme.primary, shape = CircleShape)
            ) {
                Icon(
                    if (status.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = "Play/Pause",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }

            IconButton(onClick = { skipNext() }) {
                Icon(Icons.Default.SkipNext, contentDescription = "Weiter")
            }

            VerticalDivider(Modifier.height(14.dp))

            IconButton(onClick = { togglePauseAtEndOfMediaItems() }) {
                Icon(
                    Icons.Default.PlayDisabled,
                    contentDescription = "Pause nach Titel",
                    tint = if (status.pauseAtEndOfMediaItems) MaterialTheme.colorScheme.error else LocalContentColor.current
                )
            }

            IconButton(onClick = { toggleShuffle() }) {
                Icon(
                    Icons.Default.Shuffle,
                    contentDescription = "Shuffle",
                    tint = if (status.shuffleMode) MaterialTheme.colorScheme.primary else LocalContentColor.current
                )
            }

            IconButton(onClick = { toggleRepeat() }) {
                val icon = when (status.repeatMode) {
                    Player.REPEAT_MODE_ONE -> Icons.Default.RepeatOne
                    else -> Icons.Default.Repeat
                }
                Icon(
                    icon,
                    contentDescription = "Repeat",
                    tint = if (status.repeatMode != Player.REPEAT_MODE_OFF) MaterialTheme.colorScheme.primary else LocalContentColor.current
                )
            }
        }
    }
}

@Preview
@Composable
private fun PlayerBarContentPreview() {

    Surface() {
        PlayerBarContent(
            status = PlayerStatus(
                currentTrackTitle = "Test Titel",
                currentTrackArtist = "Artist",
                currentArtworkBytes = null,
                duration = 120000,
                position = 60000,
                isPlaying = true,
                shuffleMode = false,
                repeatMode = Player.REPEAT_MODE_OFF,
                playlist = emptyList(),
                currentIndex = 0,
                pauseAtEndOfMediaItems = false

            ),
            imageBitmap = null,
            fallBackIcon = Icons.Default.MusicNote,
            seekTo = {},
            togglePauseAtEndOfMediaItems = {},
            skipPrev = {},
            skipNext = {},
            toggleShuffle = {},
            toggleRepeat = {},
            togglePlayPause = {}
        )
    }

}