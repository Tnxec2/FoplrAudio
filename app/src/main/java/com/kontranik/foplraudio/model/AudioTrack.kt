package com.kontranik.foplraudio.model

import android.net.Uri
import androidx.compose.ui.graphics.ImageBitmap

data class AudioTrack(
    val id: Long? = null,
    val title: String = "",
    val artist: String? = null,
    val album: String? = null,
    val albumArtist: String? = null,
    val path: String,
    val duration: Long = 0L,
    val uri: Uri,
    val bitmap: ImageBitmap? = null
)