package com.quranplayer.tawakalplayer

import android.content.Intent
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

class PlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        Logger.log(this, "PlaybackService onCreate — service starting up")

        val player = ExoPlayer.Builder(this)
            .setHandleAudioBecomingNoisy(true)
            .build()

        player.repeatMode = Player.REPEAT_MODE_OFF

        player.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                Logger.log(this@PlaybackService, "isPlaying changed: $isPlaying")
            }

            override fun onPlaybackStateChanged(state: Int) {
                val stateName = when (state) {
                    Player.STATE_IDLE -> "IDLE"
                    Player.STATE_BUFFERING -> "BUFFERING"
                    Player.STATE_READY -> "READY"
                    Player.STATE_ENDED -> "ENDED"
                    else -> "UNKNOWN($state)"
                }
                Logger.log(this@PlaybackService, "Playback state changed: $stateName")
            }

            override fun onPlayerError(error: PlaybackException) {
                Logger.log(
                    this@PlaybackService,
                    "PLAYER ERROR — code=${error.errorCode} (${error.errorCodeName}), message=${error.message}"
                )
            }
        })

        mediaSession = MediaSession.Builder(this, player).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        Logger.log(this, "onTaskRemoved called — app swiped away from recents")
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        Logger.log(this, "PlaybackService onDestroy — service shutting down")
        mediaSession?.run {
            player.release()
            release()
            mediaSession = null
        }
        super.onDestroy()
    }
}