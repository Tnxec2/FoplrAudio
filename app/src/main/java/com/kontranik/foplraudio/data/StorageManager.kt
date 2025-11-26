package com.kontranik.foplraudio.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.kontranik.foplraudio.model.FolderBookmark
import androidx.core.net.toUri
import androidx.core.content.edit
import androidx.documentfile.provider.DocumentFile

class StorageManager(private val context: Context) {
    private val prefs = context.getSharedPreferences("audio_player_prefs", Context.MODE_PRIVATE)
    private val gson = Gson()

    companion object {
        const val FOLDER_BOOKMARKS = "folder_bookmarks"
        const val last_folder_uri = "last_folder_uri"
        const val last_folder_name = "last_folder_name"
        const val last_played_uri = "last_played_uri"
    }

    fun saveFolders(folders: List<FolderBookmark>) {
        val json = gson.toJson(folders)
        prefs.edit {
            putString(FOLDER_BOOKMARKS, json)
            apply()
        }
    }

    fun loadFolders(): List<FolderBookmark> {
        val json = prefs.getString(FOLDER_BOOKMARKS, null)
        return if (json != null) {
            val type = object : TypeToken<List<FolderBookmark>>() {}.type
            gson.fromJson(json, type)
        } else {
            emptyList()
        }
    }

    fun persistUriPermission(uri: Uri) {
        val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        try {
            context.contentResolver.takePersistableUriPermission(uri, takeFlags)
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    fun releaseUriPermission(uri: Uri) {
        val takeFlags: Int = Intent.FLAG_GRANT_READ_URI_PERMISSION or
                Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        try {
            context.contentResolver.releasePersistableUriPermission(uri, takeFlags)
        } catch (e: Exception) {
            // Ignorieren falls Berechtigung nicht mehr existiert
        }
    }

    fun saveLastPlayedUri(uriString: String) {
        prefs.edit {
            putString(last_played_uri, uriString)
            apply()
        }
    }

    fun saveLastOpenedFolder(uri: Uri, name: String) {
        Log.d("StorageManager", "Saving last opened folder: $uri, $name")
        prefs.edit {
            putString(last_folder_uri, uri.toString())
            putString(last_folder_name, name)
            apply()
        }
    }

    fun getLastOpenedFolder(): Pair<Uri, String>? {
        val lastFolderUriStr = prefs.getString(last_folder_uri, null)
        val lastFolderName = prefs.getString(last_folder_name, "Zuletzt geöffnet")

        if (lastFolderUriStr != null) {
            try {
                val uri = lastFolderUriStr.toUri()
                Log.d("StorageManager", "Loading last opened folder: $uri, $lastFolderName")
//                val hasPermission = context.contentResolver.persistedUriPermissions.any { it.uri == uri }
//                if (hasPermission) {
//                    return Pair(uri, lastFolderName!!)
//                }
                val document = DocumentFile.fromTreeUri(context, uri)
                if (document?.canRead() == true) {
                    return Pair(uri, lastFolderName!!)
                } else {
                    Log.w("StorageManager", "Keine Berechtigung für $uri. Letzter Ordner wird gelöscht.")
                    prefs.edit {
                        remove(last_folder_uri)
                        remove(last_folder_name)
                        apply()
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return null
    }
}