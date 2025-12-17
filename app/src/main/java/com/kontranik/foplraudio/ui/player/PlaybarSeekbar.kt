package com.kontranik.foplraudio.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kontranik.foplraudio.model.PlayerStatus
import com.kontranik.foplraudio.ui.player.helpers.formatDuration

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaybarSeekbar(
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
                modifier = Modifier.Companion
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
                modifier = Modifier.Companion.height(4.dp)
            )
        },
        modifier = Modifier.Companion.padding(0.dp)
    )
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.Companion.fillMaxWidth()
    ) {
        Text(formatDuration(status.position), style = MaterialTheme.typography.labelSmall)
        Text(formatDuration(status.duration), style = MaterialTheme.typography.labelSmall)
    }
}