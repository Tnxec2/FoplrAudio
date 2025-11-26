package com.kontranik.foplraudio.player

import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.kontranik.foplraudio.R
import com.kontranik.foplraudio.model.AudioTrack
import com.kontranik.foplraudio.model.FileItem

@Composable
fun rememberMetadata(fileItem: FileItem): MutableState<AudioTrack?> {
    if (fileItem.isDirectory) return remember { mutableStateOf(null) }
    val retriever = MediaMetadataRetriever()
    val context = LocalContext.current
    val artwork = remember { mutableStateOf (
        try {
                retriever.setDataSource(context, fileItem.uri)

                val artworkBytes = retriever.embeddedPicture
                val image = artworkBytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }?.asImageBitmap()

                val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: fileItem.name
                val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                val albumArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST)

                AudioTrack(
                    title = title,
                    artist = artist,
                    album = album,
                    albumArtist = albumArtist,
                    path = fileItem.uri.toString(),
                    uri = fileItem.uri,
                    bitmap = image
                )
            } catch (e: Exception) {
                // Fehler beim Laden der Metadaten
                null
            } finally {
                try { retriever.release() } catch (_: Exception) {}
            }
    )}
    return artwork
}
