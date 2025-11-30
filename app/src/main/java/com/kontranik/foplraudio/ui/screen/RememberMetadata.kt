package com.kontranik.foplraudio.ui.screen

import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import com.kontranik.foplraudio.model.AudioTrack
import com.kontranik.foplraudio.model.FileItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun rememberMetadata(fileItem: FileItem): AudioTrack? {
    var audioTrack by remember(fileItem) { mutableStateOf<AudioTrack?>(null) }

    if (fileItem.isDirectory) {
        return null
    }

    val context = LocalContext.current

    LaunchedEffect(fileItem) {
        withContext(Dispatchers.IO) {
            val retriever = MediaMetadataRetriever()
            try {
                retriever.setDataSource(context, fileItem.uri)

                val artworkBytes = retriever.embeddedPicture
                val image = artworkBytes?.let {
                    BitmapFactory.decodeByteArray(it, 0, it.size)
                }?.asImageBitmap()

                val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE) ?: fileItem.name
                val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                val albumArtist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUMARTIST)

                audioTrack = AudioTrack(
                    title = title,
                    artist = artist,
                    album = album,
                    albumArtist = albumArtist,
                    path = fileItem.uri.toString(),
                    uri = fileItem.uri,
                    bitmap = image
                )
            } catch (e: Exception) {
                Log.e("rememberMetadata", "Failed to load metadata for ${fileItem.uri}", e)
                audioTrack = AudioTrack(
                    title = fileItem.name,
                    artist = null,
                    album = null,
                    albumArtist = null,
                    path = fileItem.uri.toString(),
                    uri = fileItem.uri,
                    bitmap = null
                )
            } finally {
                retriever.release()
            }
        }
    }

    return audioTrack
}
