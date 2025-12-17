package com.kontranik.foplraudio.ui.player

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kontranik.foplraudio.R

@Composable
fun PlaybarArtwork(
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
            contentScale = ContentScale.Companion.Inside
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