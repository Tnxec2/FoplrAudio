package com.kontranik.foplraudio.model

import android.net.Uri

data class FileItem(val name: String, val uri: Uri, val isDirectory: Boolean, val parentUri: Uri)
