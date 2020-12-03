package github.sachin2dehury.musicplayer.exoplayer

import android.support.v4.media.MediaMetadataCompat
import github.sachin2dehury.musicplayer.data.entities.Song

fun MediaMetadataCompat.toSong(): Song? {
    return description?.let { song ->
        Song(
            song.mediaId ?: "",
            song.title.toString(),
            song.subtitle.toString(),
            song.mediaUri.toString(),
            song.iconUri.toString(),
        )
    }
}