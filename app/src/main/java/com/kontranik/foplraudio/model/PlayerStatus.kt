package com.kontranik.foplraudio.model

import androidx.media3.common.MediaItem
import androidx.media3.common.Player

data class PlayerStatus(
    val currentTrackTitle: String = "Kein Titel",
    val currentTrackArtist: String? = null,
    val currentArtworkBytes: ByteArray? = null,
    val isPlaying: Boolean = false,
    val shuffleMode: Boolean = false,
    val repeatMode: Int = Player.REPEAT_MODE_OFF,
    val pauseAtEndOfMediaItems: Boolean = false,
    val duration: Long = 0L,
    val position: Long = 0L,
    val playlist: List<MediaItem> = emptyList(),
    val currentIndex: Int = -1,
    val loading: Boolean = false
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PlayerStatus

        if (isPlaying != other.isPlaying) return false
        if (loading != other.loading) return false
        if (shuffleMode != other.shuffleMode) return false
        if (repeatMode != other.repeatMode) return false
        if (pauseAtEndOfMediaItems != other.pauseAtEndOfMediaItems) return false
        if (duration != other.duration) return false
        if (position != other.position) return false
        if (currentIndex != other.currentIndex) return false
        if (currentTrackTitle != other.currentTrackTitle) return false
        if (!currentArtworkBytes.contentEquals(other.currentArtworkBytes)) return false
        if (playlist != other.playlist) return false

        return true
    }

    override fun hashCode(): Int {
        var result = isPlaying.hashCode()
        result = 31 * result + shuffleMode.hashCode()
        result = 31 * result + loading.hashCode()
        result = 31 * result + repeatMode
        result = 31 * result + pauseAtEndOfMediaItems.hashCode()
        result = 31 * result + duration.hashCode()
        result = 31 * result + position.hashCode()
        result = 31 * result + currentIndex
        result = 31 * result + currentTrackTitle.hashCode()
        result = 31 * result + (currentArtworkBytes?.contentHashCode() ?: 0)
        result = 31 * result + playlist.hashCode()
        return result
    }
}