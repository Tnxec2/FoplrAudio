package com.kontranik.foplraudio.player

import android.content.Intent
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionCommands
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture


class PlaybackService : MediaSessionService() {
    private var mediaSession: MediaSession? = null
    private var player: ExoPlayer? = null

    private inner class CustomMediaSessionCallback : MediaSession.Callback {
        override fun onConnect(
            session: MediaSession,
            controller: MediaSession.ControllerInfo
        ): MediaSession.ConnectionResult {
            // Standard-Befehle holen
            val standardCommands = MediaSession.ConnectionResult.DEFAULT_SESSION_COMMANDS

            // Fügen Sie Ihre benutzerdefinierten Befehle hinzu
            val customCommands = SessionCommands.Builder()
                .addSessionCommands(standardCommands.commands)
                .add(CustomCommands.COMMAND_TOGGLE_PAUSE_AT_END)
                .add(CustomCommands.COMMAND_GET_PAUSE_AT_END_STATUS)
                .build()

            return MediaSession.ConnectionResult.AcceptedResultBuilder(session)
                .setAvailableSessionCommands(customCommands)
                .build()
        }

        override fun onCustomCommand(
            session: MediaSession,
            controller: MediaSession.ControllerInfo,
            customCommand: SessionCommand,
            args: Bundle
        ): ListenableFuture<SessionResult> {

            when (customCommand.customAction) {
                CustomCommands.ACTION_TOGGLE_PAUSE_AT_END -> {
                    val pauseAtEnd = args.getBoolean(CustomCommands.PARAM_PAUSE_AT_END, false)
                    (session.player as? ExoPlayer)?.pauseAtEndOfMediaItems = pauseAtEnd
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS))
                }
                CustomCommands.ACTION_GET_PAUSE_AT_END_STATUS -> {
                    val currentStatus = player?.pauseAtEndOfMediaItems ?: false
                    val resultBundle = Bundle().apply {
                        putBoolean(CustomCommands.RESULT_PAUSE_AT_END_STATUS, currentStatus)
                    }
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS, resultBundle))
                }
            }

            // Wenn der Befehl nicht bekannt ist
            return Futures.immediateFuture(SessionResult(SessionResult.RESULT_ERROR_NOT_SUPPORTED))
        }
    }

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        player = ExoPlayer.Builder(this).build()
        mediaSession = MediaSession.Builder(this, player!!)
            .setCallback(CustomMediaSessionCallback())
            .build()

        setMediaNotificationProvider(DefaultMediaNotificationProvider(this))
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.isPlaying) {
            stopSelf()
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}