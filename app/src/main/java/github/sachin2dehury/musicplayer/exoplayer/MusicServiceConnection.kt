package github.sachin2dehury.musicplayer.exoplayer

import android.content.ComponentName
import android.content.Context
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaControllerCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import github.sachin2dehury.musicplayer.others.Constants.NETWORK_ERROR
import github.sachin2dehury.musicplayer.others.Event
import github.sachin2dehury.musicplayer.others.Resource

class MusicServiceConnection(context: Context) {

    private val _isConnected = MutableLiveData<Event<Resource<Boolean>>>()
    val isConnected: LiveData<Event<Resource<Boolean>>> = _isConnected

    private val _networkError = MutableLiveData<Event<Resource<Boolean>>>()
    val networkError: LiveData<Event<Resource<Boolean>>> = _networkError

    private val _playbackState = MutableLiveData<PlaybackStateCompat?>()
    val playbackState: LiveData<PlaybackStateCompat?> = _playbackState

    private val _currentSongPlaying = MutableLiveData<MediaMetadataCompat?>()
    val currentSongPlaying: LiveData<MediaMetadataCompat?> = _currentSongPlaying

    lateinit var mediaController: MediaControllerCompat

    private val mediaBrowserConnectionCallback = MediaBrowserConnectionCallback(context)

    private val mediaBrowser = MediaBrowserCompat(
        context,
        ComponentName(context, MusicService::class.java),
        mediaBrowserConnectionCallback, null
    ).apply {
        connect()
    }

    val transportControls: MediaControllerCompat.TransportControls
        get() = mediaController.transportControls

    fun subscribe(parentId: String, callback: MediaBrowserCompat.SubscriptionCallback) {
        mediaBrowser.subscribe(parentId, callback)
    }

    fun unsubscribe(parentId: String, callback: MediaBrowserCompat.SubscriptionCallback) {
        mediaBrowser.unsubscribe(parentId, callback)
    }

    private inner class MediaBrowserConnectionCallback(
        private val context: Context
    ) : MediaBrowserCompat.ConnectionCallback() {

        override fun onConnected() {

//            Log.d("MusicServiceConnection", "CONNECTED")

            mediaController = MediaControllerCompat(context, mediaBrowser.sessionToken).apply {
                registerCallback(MediaControllerCallback())
            }
            _isConnected.postValue(Event(Resource.success(true)))
        }

        override fun onConnectionSuspended() {

//            Log.d("MusicServiceConnection", "SUSPENDED")

            _isConnected.postValue(
                Event(
                    Resource.error(
                        false, "The connection was suspended"
                    )
                )
            )
        }

        override fun onConnectionFailed() {

//            Log.d("MusicServiceConnection", "FAILED")

            _isConnected.postValue(
                Event(
                    Resource.error(
                        false, "Couldn't connect to media browser"
                    )
                )
            )
        }
    }

    private inner class MediaControllerCallback : MediaControllerCompat.Callback() {

        override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
            _playbackState.postValue(state)
        }

        override fun onMetadataChanged(metadata: MediaMetadataCompat?) {
            _currentSongPlaying.postValue(metadata)
        }

        override fun onSessionEvent(event: String?, extras: Bundle?) {
            super.onSessionEvent(event, extras)

            when (event) {
                NETWORK_ERROR -> _networkError.postValue(
                    Event(
                        Resource.error(
                            null,
                            "Couldn't connect to the server. Please check your internet connection."
                        )
                    )
                )
            }
        }

        override fun onSessionDestroyed() {
            mediaBrowserConnectionCallback.onConnectionSuspended()
        }
    }
}