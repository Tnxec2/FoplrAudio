package com.kontranik.foplraudio

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import com.kontranik.foplraudio.player.AudioPlayerApp
import com.kontranik.foplraudio.ui.theme.FoplrAudioTheme


class MainActivity : ComponentActivity() {

    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            FoplrAudioTheme {
                AudioPlayerApp()
            }
        }
    }

}