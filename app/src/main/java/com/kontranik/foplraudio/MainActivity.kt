package com.kontranik.foplraudio

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.media3.common.util.UnstableApi
import com.kontranik.foplraudio.player.AudioPlayerApp
import com.kontranik.foplraudio.player.PlaybackService
import com.kontranik.foplraudio.ui.theme.FoplrAudioTheme

@OptIn(ExperimentalMaterial3Api::class)
@UnstableApi
class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.P)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STARTE DEN PLAYBACK SERVICE
        // Dies ist notwendig, damit der MediaController im ViewModel eine Verbindung herstellen kann.
        // Der Service wird durch den startService-Call gestartet und läuft dann
        startService(Intent(this, PlaybackService::class.java))

        setContent {
            FoplrAudioTheme {
                AudioPlayerApp()
            }
        }
    }

    override fun onDestroy() {
        // Stoppe den Dienst beim Zerstören der Activity.
        stopService(Intent(this, PlaybackService::class.java))
        super.onDestroy()
    }
}