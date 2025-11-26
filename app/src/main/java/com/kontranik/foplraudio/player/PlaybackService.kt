package com.kontranik.foplraudio.player

import android.content.Intent
import android.os.Binder
import android.os.IBinder
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

// --- Playback Service (Verwaltet ExoPlayer im Hintergrund) ---
/**
 * DIESER DIENST ERMÖGLICHT DIE STEUERUNG ÜBER DIE BENACHRICHTIGUNGSLEISTE UND DEN SPERRBILDSCHIRM.
 * Er hostet den ExoPlayer und die MediaSession.
 * Zusätzlich wird ein Binder bereitgestellt, um direkten Zugriff auf den ExoPlayer zu ermöglichen.
 */
@UnstableApi
class PlaybackService : MediaSessionService() {

    private lateinit var player: ExoPlayer
    private lateinit var mediaSession: MediaSession

    // Binder zur Ermöglichung des direkten Zugriffs auf den ExoPlayer
    inner class LocalBinder : Binder() {
        fun getPlayer(): ExoPlayer = player
        fun getService(): PlaybackService = this@PlaybackService
    }

    private val binder = LocalBinder()

    override fun onCreate() {
        super.onCreate()

        // 1. ExoPlayer initialisieren
        player = ExoPlayer.Builder(this).build()

        // 2. MediaSession erstellen und mit dem Player verbinden
        mediaSession = MediaSession.Builder(this, player).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession = mediaSession

    // Füge die onBind-Methode hinzu, um den LocalBinder zurückzugeben
    override fun onBind(intent: Intent?): IBinder? {
        // Zuerst den MediaSessionService-Teil aufrufen
        val sessionBinder = super.onBind(intent)

        // Wenn die Bindung für unseren lokalen Zugriff ist, den LocalBinder zurückgeben
        return if (intent?.action == ACTION_BIND_EXOPLAYER) {
            binder
        } else {
            sessionBinder
        }
    }

    companion object {
        // Eine eindeutige Aktion für die lokale Bindung definieren
        const val ACTION_BIND_EXOPLAYER = "com.example.audioplayer.BIND_EXOPLAYER"
    }

    override fun onDestroy() {
        mediaSession.run {
            player.release()
            release()
        }
        super.onDestroy()
    }
}