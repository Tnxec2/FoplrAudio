package com.kontranik.foplraudio.ui.player

import android.graphics.BitmapFactory
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayDisabled
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SliderState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import com.kontranik.foplraudio.R
import com.kontranik.foplraudio.model.PlayerStatus
import com.kontranik.foplraudio.ui.player.helpers.formatDuration


@RequiresApi(Build.VERSION_CODES.P)
@Composable
fun PlayerBar(
    viewModel: PlayerViewModel,
    stretchArt: Boolean = false,
    toggleMenu: () -> Unit,
    clickPlayinfo: () -> Unit,
    showPlaylist: Boolean = false,
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

    if (!stretchArt) {
        PlayerBarContentSmall(
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
            toggleMenu = toggleMenu,
            showMenu = showPlaylist,
            clickPlayinfo = clickPlayinfo,
        )
    } else {
        PlayerBarContentBig (
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
            toggleMenu = toggleMenu,
            showMenu = showPlaylist,
            clickPlayinfo = clickPlayinfo,
        )
    }
}

@Composable
private fun PlayerBarContentSmall(
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
    toggleMenu: () -> Unit,
    clickPlayinfo: () -> Unit,
    showMenu: Boolean,
) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp)
    ) {
        PlaybarSeekbar(status, seekTo)

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .clickable( onClick = { clickPlayinfo() }),
            verticalAlignment = Alignment.CenterVertically
        ) {
            PlaybarArtwork(
                imageBitmap = imageBitmap,
                fallBackIcon = fallBackIcon,
                modifier = Modifier
                    .size(68.dp)
            )
            Spacer(Modifier.width(8.dp))
            PlaybarCurrentTrackInfo(status)
        }

        PlaybarButtons(
            skipPrev,
            togglePlayPause,
            status,
            skipNext,
            togglePauseAtEndOfMediaItems,
            toggleShuffle,
            toggleRepeat,
            toggleMenu = toggleMenu,
            showMenu = showMenu
        )
    }
}


@Composable
private fun PlayerBarContentBig(
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
    toggleMenu: () -> Unit,
    showMenu: Boolean,
    clickPlayinfo: () -> Unit,

    ) {

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(16.dp)

    ) {
        PlaybarSeekbar(status, seekTo)

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .weight(1f)
                .clickable( onClick = { clickPlayinfo() }),
        ) {
            PlaybarArtwork(
                imageBitmap = imageBitmap,
                fallBackIcon = fallBackIcon,
                modifier = Modifier
                    .weight(1f)
            )

            Spacer(Modifier.height(8.dp))

            PlaybarCurrentTrackInfo(status, Alignment.CenterHorizontally)
        }
        PlaybarButtons(
            skipPrev,
            togglePlayPause,
            status,
            skipNext,
            togglePauseAtEndOfMediaItems,
            toggleShuffle,
            toggleRepeat,
            showMenu,
            toggleMenu,

        )
    }
}

@Composable
private fun PlaybarArtwork(
    imageBitmap: ImageBitmap?,
    fallBackIcon: ImageVector,
    modifier: Modifier
) {
    val artworkModifier = modifier
        .fillMaxWidth()

    if (imageBitmap != null) {
        Image(
            bitmap = imageBitmap,
            contentDescription = stringResource(R.string.cover_art),
            modifier = artworkModifier,
            contentScale = ContentScale.Inside
        )
    } else {
        Icon(
            fallBackIcon,
            contentDescription = stringResource(R.string.music_note),
            modifier = artworkModifier.padding(8.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}


@Composable
private fun PlaybarCurrentTrackInfo(
    status: PlayerStatus,
    alignment: Alignment.Horizontal = Alignment.Start
) {
    Column(
        horizontalAlignment = alignment
    ) {
        Text(
            text = status.currentTrackTitle.ifEmpty { stringResource(R.string.no_title) },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier
                .padding(bottom = 8.dp)
        )
        if (status.currentTrackArtist != null) {
            Text(
                text = status.currentTrackArtist,
                maxLines = 1,
                overflow = TextOverflow.Clip,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlaybarSeekbar(
    status: PlayerStatus,
    seekTo: (Long) -> Unit
) {
    val interactionSource: MutableInteractionSource = remember { MutableInteractionSource() }
    val colors = SliderDefaults.colors()


    Slider(
        value = status.position.toFloat(),
        onValueChange = { seekTo(it.toLong()) },
        valueRange = 0f..status.duration.toFloat(),
        // modifier = Modifier.height(20.dp)
        thumb = {
            Box(
                modifier = Modifier
                    .padding(0.dp)
                    .size(16.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
            )
        },
        track = { sliderState ->
            SliderDefaults.Track(
                colors = colors,
                thumbTrackGapSize = 0.dp,
                enabled = true,
                sliderState = sliderState,
                modifier = Modifier.height(4.dp)
            )
        },
        modifier = Modifier.padding(0.dp)
    )
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(formatDuration(status.position), style = MaterialTheme.typography.labelSmall)
        Text(formatDuration(status.duration), style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun PlaybarButtons(
    skipPrev: () -> Unit,
    togglePlayPause: () -> Unit,
    status: PlayerStatus,
    skipNext: () -> Unit,
    togglePauseAtEndOfMediaItems: () -> Unit,
    toggleShuffle: () -> Unit,
    toggleRepeat: () -> Unit,
    showMenu: Boolean,
    toggleMenu: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {

        IconButton(onClick = { toggleMenu() }) {
            Icon(
                if (!showMenu) Icons.AutoMirrored.Filled.List else Icons.Default.Close,
                contentDescription = stringResource(R.string.toggle_menu))
        }

        IconButton(onClick = { skipPrev() }) {
            Icon(Icons.Default.SkipPrevious, contentDescription = stringResource(R.string.previous))
        }

        IconButton(
            onClick = { togglePlayPause() },
            modifier = Modifier
                .size(48.dp)
                .background(MaterialTheme.colorScheme.primary, shape = CircleShape)
        ) {
            Icon(
                if (status.isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                contentDescription = stringResource(R.string.play_pause),
                tint = MaterialTheme.colorScheme.onPrimary
            )
        }

        IconButton(onClick = { skipNext() }) {
            Icon(Icons.Default.SkipNext, contentDescription = stringResource(R.string.next))
        }

        VerticalDivider(Modifier.height(18.dp))

        IconButton(onClick = { togglePauseAtEndOfMediaItems() }) {
            Icon(
                Icons.Default.PlayDisabled,
                contentDescription = stringResource(R.string.pause_after_current_item),
                tint = if (status.pauseAtEndOfMediaItems) MaterialTheme.colorScheme.error else LocalContentColor.current
            )
        }

        IconButton(onClick = { toggleShuffle() }) {
            Icon(
                Icons.Default.Shuffle,
                contentDescription = stringResource(R.string.shuffle),
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
                contentDescription = stringResource(R.string.repeat),
                tint = if (status.repeatMode != Player.REPEAT_MODE_OFF) MaterialTheme.colorScheme.primary else LocalContentColor.current
            )
        }
    }
}

val sampleStatus = PlayerStatus(
    currentTrackTitle = "Test title",
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

)
@Preview
@Composable
private fun PlayerBarContentSmallPreview() {

    Surface() {
        PlayerBarContentSmall(
            status = sampleStatus,
            imageBitmap = null,
            fallBackIcon = Icons.Default.MusicNote,
            seekTo = {},
            togglePauseAtEndOfMediaItems = {},
            skipPrev = {},
            skipNext = {},
            toggleShuffle = {},
            toggleRepeat = {},
            togglePlayPause = {},
            toggleMenu = {},
            showMenu = false,
            clickPlayinfo = {}
        )
    }

}

@Preview
@Composable
private fun PlayerBarContentBigPreview() {

    Surface(Modifier.height(400.dp).width(500.dp)) {
        PlayerBarContentBig(
            status = sampleStatus,
            imageBitmap = null,
            fallBackIcon = Icons.Default.MusicNote,
            seekTo = {},
            togglePauseAtEndOfMediaItems = {},
            skipPrev = {},
            skipNext = {},
            toggleShuffle = {},
            toggleRepeat = {},
            togglePlayPause = {},
            toggleMenu = {},
            showMenu = false,
            clickPlayinfo = {}
        )
    }

}