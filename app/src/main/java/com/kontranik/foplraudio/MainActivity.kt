package com.kontranik.foplraudio

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.kontranik.foplraudio.player.AudioPlayerApp
import com.kontranik.foplraudio.ui.theme.FoplrAudioTheme


class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            FoplrAudioTheme {
                AudioPlayerApp()
            }
        }
    }

}