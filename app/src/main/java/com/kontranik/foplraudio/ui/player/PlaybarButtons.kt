package com.kontranik.foplraudio.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ListAlt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LooksOne
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayDisabled
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.media3.common.Player
import com.kontranik.foplraudio.R
import com.kontranik.foplraudio.model.PlayerStatus
import com.kontranik.foplraudio.ui.theme.FoplrAudioTheme

@Composable
fun PlaybarButtons(
    skipPrev: () -> Unit = {},
    togglePlayPause: () -> Unit = {},
    status: PlayerStatus,
    skipNext: () -> Unit = {},
    togglePauseAtEndOfMediaItems: () -> Unit = {},
    toggleShuffle: () -> Unit = {},
    toggleRepeat: () -> Unit = {},
    showMenu: Boolean,
    toggleMenu: () -> Unit = {},
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val borderCircleModifier = Modifier
            .background(MaterialTheme.colorScheme.primary, shape = CircleShape)

        IconButton(onClick = { toggleMenu() }) {
            Icon(
                if (!showMenu) Icons.AutoMirrored.Filled.ListAlt else Icons.Default.Close,
                contentDescription = stringResource(R.string.toggle_menu)
            )
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

        IconButton(
            onClick = { togglePauseAtEndOfMediaItems() },
            modifier = if (status.pauseAtEndOfMediaItems) borderCircleModifier else Modifier
        ) {
            Icon(
                Icons.Default.PlayDisabled,
                contentDescription = stringResource(R.string.pause_after_current_item),
                tint = if (status.pauseAtEndOfMediaItems)
                    MaterialTheme.colorScheme.onPrimary
                else
                    LocalContentColor.current,
            )
        }

        IconButton(
            onClick = { toggleShuffle() },
            modifier = if (status.shuffleMode) borderCircleModifier else Modifier
        ) {
            Icon(
                Icons.Default.Shuffle,
                contentDescription = stringResource(R.string.shuffle),
                tint = if (status.shuffleMode)
                    MaterialTheme.colorScheme.onPrimary
                else
                    LocalContentColor.current,
            )
        }

        IconButton(
            onClick = { toggleRepeat() },
            modifier = if (status.repeatMode != Player.REPEAT_MODE_OFF) borderCircleModifier else Modifier
        ) {
            val icon = when (status.repeatMode) {
                Player.REPEAT_MODE_ONE -> Icons.Default.LooksOne
                else -> Icons.Default.Repeat
            }
            Icon(
                icon,
                contentDescription = stringResource(R.string.repeat),
                tint = if (status.repeatMode != Player.REPEAT_MODE_OFF)
                    MaterialTheme.colorScheme.onPrimary
                else
                    LocalContentColor.current,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PlaybarButtonsPreview1() {
    FoplrAudioTheme {
        Surface() {
            PlaybarButtons(
                status = PlayerStatus(
                    shuffleMode = true,
                    repeatMode = Player.REPEAT_MODE_ONE,
                    pauseAtEndOfMediaItems = true,
                    isPlaying = true,
                ),
                showMenu = false,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun PlaybarButtonsPreview2() {
    FoplrAudioTheme {
        Surface() {
            PlaybarButtons(
                status = PlayerStatus(
                    shuffleMode = false,
                    repeatMode = Player.REPEAT_MODE_ALL,
                    pauseAtEndOfMediaItems = false,
                    isPlaying = false,
                ),
                showMenu = false,
            )
        }
    }
}


@Preview(showBackground = true)
@Composable
fun PlaybarButtonsPreview3() {
    FoplrAudioTheme {
        Surface() {
            PlaybarButtons(
                status = PlayerStatus(
                    shuffleMode = false,
                    repeatMode = Player.REPEAT_MODE_OFF,
                    pauseAtEndOfMediaItems = false,
                    isPlaying = false,
                ),
                showMenu = true,
            )
        }
    }
}