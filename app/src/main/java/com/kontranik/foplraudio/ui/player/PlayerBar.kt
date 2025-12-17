package com.kontranik.foplraudio.ui.player

import android.graphics.BitmapFactory
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import com.kontranik.foplraudio.R
import com.kontranik.foplraudio.model.PlayerStatus
import com.kontranik.foplraudio.ui.theme.FoplrAudioTheme


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
            } catch (_: Exception) {
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
fun PlaybarCurrentTrackInfo(
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

val sampleStatus = PlayerStatus(
    currentTrackTitle = "Test title",
    currentTrackArtist = "Artist",
    currentArtworkBytes = null,
    duration = 120000,
    position = 60000,
    isPlaying = true,
    shuffleMode = true,
    repeatMode = Player.REPEAT_MODE_ONE,
    playlist = emptyList(),
    currentIndex = 0,
    pauseAtEndOfMediaItems = true

)
@Preview
@Composable
private fun PlayerBarContentSmallPreview() {

    FoplrAudioTheme(
        dynamicColor = false
    ) {
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
}

@Preview
@Composable
private fun PlayerBarContentBigPreview() {
    FoplrAudioTheme(
        dynamicColor = false
    ) {
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
}