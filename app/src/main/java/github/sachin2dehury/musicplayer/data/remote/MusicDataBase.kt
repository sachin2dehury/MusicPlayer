package github.sachin2dehury.musicplayer.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import github.sachin2dehury.musicplayer.data.entities.Song
import github.sachin2dehury.musicplayer.others.Constants.SONG_COLLECTION
import kotlinx.coroutines.tasks.await

class MusicDataBase {

    private val fireStore = FirebaseFirestore.getInstance()
    private val songCollection = fireStore.collection(SONG_COLLECTION)

    suspend fun getAllSongs(): List<Song> {
        return try {
            songCollection.get().await().toObjects(Song::class.java)
        } catch (e: Exception) {
            emptyList()
        }
    }
}