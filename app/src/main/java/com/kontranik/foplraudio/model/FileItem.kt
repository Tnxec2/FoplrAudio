package com.kontranik.foplraudio.model

import android.net.Uri
import androidx.core.net.toUri

data class FileItem(val name: String, val uri: Uri, val isDirectory: Boolean, val parentUri: Uri)
data class FileItemDTO(val name: String, val uri: String, val isDirectory: Boolean, val parentUri: String)

fun FileItem.toDTO(): FileItemDTO {
    return FileItemDTO(name, uri.toString(), isDirectory, parentUri.toString())
}

fun FileItemDTO.toFileItem() : FileItem {
    return FileItem(name, uri.toUri(), isDirectory, parentUri.toUri())
}



