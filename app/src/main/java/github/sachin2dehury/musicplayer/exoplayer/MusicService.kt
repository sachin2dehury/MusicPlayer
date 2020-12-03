package github.sachin2dehury.musicplayer.exoplayer

import android.app.PendingIntent
import android.content.Intent
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import androidx.media.MediaBrowserServiceCompat
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.ext.mediasession.TimelineQueueNavigator
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import dagger.hilt.android.AndroidEntryPoint
import github.sachin2dehury.musicplayer.exoplayer.callbacks.MusicPlaybackPreparer
import github.sachin2dehury.musicplayer.exoplayer.callbacks.MusicPlayerEventListener
import github.sachin2dehury.musicplayer.exoplayer.callbacks.MusicPlayerNotificationListener
import github.sachin2dehury.musicplayer.others.Constants.MEDIA_ROOT_ID
import github.sachin2dehury.musicplayer.others.Constants.NETWORK_ERROR
import github.sachin2dehury.musicplayer.others.Constants.SERVICE_TAG
import kotlinx.coroutines.*
import javax.inject.Inject

@AndroidEntryPoint
class MusicService : MediaBrowserServiceCompat() {

    @Inject
    lateinit var dataSourceFactory: DefaultDataSourceFactory

    @Inject
    lateinit var exoPlayer: SimpleExoPlayer

    @Inject
    lateinit var fireBaseMusicSource: FireBaseMusicSource

    private lateinit var musicNotificationManager: MusicNotificationManager

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var mediaSessionConnector: MediaSessionConnector

    var isForegroundService = false

    private var currentSong: MediaMetadataCompat? = null

    private var isPlayerInitialized = false

    private lateinit var musicPlayerEventListener: MusicPlayerEventListener

    companion object {
        var currentSongDuration = 0L
            private set
    }

    override fun onCreate() {
        super.onCreate()

        serviceScope.launch {
            fireBaseMusicSource.fetchMediaData()
        }

        val activityIntent = packageManager?.getLaunchIntentForPackage(packageName)?.let {
            PendingIntent.getActivity(this, 0, it, 0)
        }

        mediaSession = MediaSessionCompat(this, SERVICE_TAG).apply {
            setSessionActivity(activityIntent)
            isActive = true
        }

        sessionToken = mediaSession.sessionToken

        musicNotificationManager = MusicNotificationManager(
            this, mediaSession.sessionToken, MusicPlayerNotificationListener(this)
        ) {
            currentSongDuration = exoPlayer.duration
        }

        val musicPlaybackPreparer = MusicPlaybackPreparer(fireBaseMusicSource) {
            currentSong = it
            preparePlayer(
                fireBaseMusicSource.songs, it, true
            )
        }

        mediaSessionConnector = MediaSessionConnector(mediaSession)
        mediaSessionConnector.apply {
            setPlaybackPreparer(musicPlaybackPreparer)
            setQueueNavigator(MusicQueueNavigator())
            setPlayer(exoPlayer)
        }

        musicPlayerEventListener = MusicPlayerEventListener(this)
        exoPlayer.addListener(musicPlayerEventListener)
        musicNotificationManager.showNotification(exoPlayer)
    }

    private fun preparePlayer(
        songs: List<MediaMetadataCompat>,
        itemToPlay: MediaMetadataCompat?,
        playNow: Boolean
    ) {
        val currentSongIndex = if (currentSong == null) 0 else songs.indexOf(itemToPlay)
        exoPlayer.prepare(fireBaseMusicSource.asMediaSource(dataSourceFactory))
        exoPlayer.seekTo(currentSongIndex, 0L)
        exoPlayer.playWhenReady = playNow
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        exoPlayer.stop()
    }

    override fun onGetRoot(
        clientPackageName: String,
        clientUid: Int,
        rootHints: Bundle?
    ): BrowserRoot? {
        return BrowserRoot(MEDIA_ROOT_ID, null)
    }

    override fun onLoadChildren(
        parentId: String,
        result: Result<MutableList<MediaBrowserCompat.MediaItem>>
    ) {
        when (parentId) {
            MEDIA_ROOT_ID -> {
                val resultSent = fireBaseMusicSource.whenReady { isInitialized ->
                    if (isInitialized) {
                        result.sendResult(fireBaseMusicSource.asMediaItems())
                        if (!isPlayerInitialized && fireBaseMusicSource.songs.isNotEmpty()) {
                            preparePlayer(
                                fireBaseMusicSource.songs,
                                fireBaseMusicSource.songs.first(),
                                false
                            )
                            isPlayerInitialized = true
                        }
                    } else {
                        mediaSession.sendSessionEvent(NETWORK_ERROR, null)
                        result.sendResult(null)
                    }
                }
                if (!resultSent) {
                    result.detach()
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()

        serviceScope.cancel()

        exoPlayer.apply {
            removeListener(musicPlayerEventListener)
            release()
        }
    }

    private inner class MusicQueueNavigator : TimelineQueueNavigator(mediaSession) {
        override fun getMediaDescription(player: Player, windowIndex: Int): MediaDescriptionCompat {
            return fireBaseMusicSource.songs[windowIndex].description
        }

    }
}