package com.kontranik.foplraudio

import android.content.Context
import com.kontranik.foplraudio.data.StorageManager

/**
 * App container for Dependency injection.
 */
interface AppContainer {
    val storageManager: StorageManager
}


class AppDataContainer(private val context: Context) : AppContainer {

    override val storageManager: StorageManager by lazy {
        StorageManager(
            context
        )
    }
}
