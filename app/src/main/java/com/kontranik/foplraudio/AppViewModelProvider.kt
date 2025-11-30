package com.kontranik.foplraudio

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kontranik.foplraudio.ui.player.PlayerViewModel

/**
 * Provides Factory to create instance of ViewModel for the entire Inventory app
 */
object AppViewModelProvider {
    @RequiresApi(Build.VERSION_CODES.O)
    val Factory = viewModelFactory {

        initializer {
            PlayerViewModel(
                application().applicationContext,
                application().container.storageManager
            )
        }
    }
}


/**
 * Extension function to queries for [FoplrAudioApplication] object and returns an instance of
 * [FoplrAudioApplication].
 */
fun CreationExtras.application(): FoplrAudioApplication =
    (this[AndroidViewModelFactory.APPLICATION_KEY] as FoplrAudioApplication)
