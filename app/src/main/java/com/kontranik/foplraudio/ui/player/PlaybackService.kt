package com.kontranik.foplraudio.ui.player

import android.Manifest
import com.kontranik.foplraudio.R
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.annotation.OptIn
import androidx.annotation.RequiresPermission
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.TaskStackBuilder
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import androidx.media3.session.SessionCommand
import androidx.media3.session.SessionCommands
import androidx.media3.session.SessionError
import androidx.media3.session.SessionResult
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import kotlin.jvm.java


@UnstableApi
class PlaybackService : MediaSessionService() {
    private var _mediaSession: MediaSession? = null
    private val mediaSession get() = _mediaSession!!

    companion object {
        private const val NOTIFICATION_ID = 123
        private const val CHANNEL_ID = "session_notification_channel_id"
        private val immutableFlag = PendingIntent.FLAG_IMMUTABLE
    }

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
                    val currentStatus = (session.player as? ExoPlayer)?.pauseAtEndOfMediaItems ?: false
                    val resultBundle = Bundle().apply {
                        putBoolean(CustomCommands.RESULT_PAUSE_AT_END_STATUS, currentStatus)
                    }
                    return Futures.immediateFuture(SessionResult(SessionResult.RESULT_SUCCESS, resultBundle))
                }
            }

            // Wenn der Befehl nicht bekannt ist
            return Futures.immediateFuture(SessionResult(SessionError.ERROR_NOT_SUPPORTED))
        }
    }

    @OptIn(UnstableApi::class)
    override fun onCreate() {
        super.onCreate()
        val player = ExoPlayer.Builder(this).build()
        _mediaSession = MediaSession.Builder(this, player)
            .also { builder ->
                getSingleTopActivity()?.let { builder.setSessionActivity(it) }
            }
            .setCallback(CustomMediaSessionCallback())
            .build()
        setListener(MediaSessionServiceListener())
        setMediaNotificationProvider(DefaultMediaNotificationProvider(this))
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return _mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession.player
        if (!player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        _mediaSession?.run {
            getBackStackedActivity()?.let { setSessionActivity(it) }
            player.release()
            release()
            _mediaSession = null
        }
        clearListener()
        super.onDestroy()
    }

    /**
     * This method creates a PendingIntent that starts the MainActivity.
     * The PendingIntent is created with the "FLAG_UPDATE_CURRENT" flag, which means that if the described PendingIntent already exists,
     * then keep it but replace its extra data with what is in this new Intent.
     * The PendingIntent also has the "FLAG_IMMUTABLE" flag if the Android version is 23 or above, which means that the created PendingIntent
     * is immutable and cannot be changed after it's created.
     *
     * @return A PendingIntent that starts the MainActivity. If the PendingIntent cannot be created for any reason, it returns null.
     */
    private fun getSingleTopActivity(): PendingIntent? {
        return PendingIntent.getActivity(
            this,
            0,
            Intent(this, com.kontranik.foplraudio.MainActivity::class.java),
            immutableFlag or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }


    /**
     * This method creates a PendingIntent that starts the MainActivity with a back stack.
     * The PendingIntent is created with the "FLAG_UPDATE_CURRENT" flag, which means that if the described PendingIntent already exists,
     * then keep it but replace its extra data with what is in this new Intent.
     * The PendingIntent also has the "FLAG_IMMUTABLE" flag if the Android version is 23 or above, which means that the created PendingIntent
     * is immutable and cannot be changed after it's created.
     * The back stack is created using TaskStackBuilder, which allows the user to navigate back to the MainActivity from the PendingIntent.
     *
     * @return A PendingIntent that starts the MainActivity with a back stack. If the PendingIntent cannot be created for any reason, it returns null.
     */
    private fun getBackStackedActivity(): PendingIntent? {
        return TaskStackBuilder.create(this).run {
            addNextIntent(Intent(this@PlaybackService, com.kontranik.foplraudio.MainActivity::class.java))
            getPendingIntent(0, immutableFlag or PendingIntent.FLAG_UPDATE_CURRENT)
        }
    }

    @OptIn(UnstableApi::class) // MediaSessionService.Listener
    private inner class MediaSessionServiceListener : Listener {

        /**
         * This method is only required to be implemented on Android 12 or above when an attempt is made
         * by a media controller to resume playback when the {@link MediaSessionService} is in the
         * background.
         */
        @RequiresPermission(Manifest.permission.POST_NOTIFICATIONS)
        override fun onForegroundServiceStartNotAllowedException() {
            if (
                Build.VERSION.SDK_INT >= 33 &&
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                // Notification permission is required but not granted
                return
            }
            val notificationManagerCompat = NotificationManagerCompat.from(this@PlaybackService)
            ensureNotificationChannel(notificationManagerCompat)
            val builder =
                NotificationCompat.Builder(this@PlaybackService, CHANNEL_ID)
                    .setSmallIcon(androidx.media3.session.R.drawable.media3_notification_small_icon)
                    .setContentTitle("notification title")
                    .setStyle(
                        NotificationCompat.BigTextStyle().bigText("notification text")
                    )
                    .setPriority(NotificationCompat.PRIORITY_DEFAULT)
                    .setAutoCancel(true)
                    .also { builder -> getBackStackedActivity()?.let { builder.setContentIntent(it) } }
            notificationManagerCompat.notify(NOTIFICATION_ID, builder.build())
        }
    }

    private fun ensureNotificationChannel(notificationManagerCompat: NotificationManagerCompat) {
        if (
            Build.VERSION.SDK_INT < 26 ||
            notificationManagerCompat.getNotificationChannel(CHANNEL_ID) != null
        ) {
            return
        }

        val channel =
            NotificationChannel(
                CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            )
        notificationManagerCompat.createNotificationChannel(channel)
    }
}