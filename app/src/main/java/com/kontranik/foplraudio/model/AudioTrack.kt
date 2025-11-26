package com.kontranik.foplraudio.model

import android.net.Uri

data class AudioTrack(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val albumArtist: String,
    val path: String,
    val duration: Long,
    val uri: Uri
)