package com.kontranik.foplraudio.player

import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import com.kontranik.foplraudio.model.FileItem

@Composable
fun rememberArtwork(fileItem: FileItem): MutableState<ImageBitmap?> {
    if (fileItem.isDirectory) return remember { mutableStateOf(null) }
    val retriever = MediaMetadataRetriever()
    val context = LocalContext.current
    val artwork = remember { mutableStateOf (
            try {
                retriever.setDataSource(context, fileItem.uri)

                val bytes = retriever.embeddedPicture
                bytes?.let { BitmapFactory.decodeByteArray(bytes, 0, it.size) }?.asImageBitmap()
            } catch (e: Exception) {
                // Fehler beim Laden der Metadaten
                null
            } finally {
                try { retriever.release() } catch (_: Exception) {}
            }
    )}
    return artwork
}
