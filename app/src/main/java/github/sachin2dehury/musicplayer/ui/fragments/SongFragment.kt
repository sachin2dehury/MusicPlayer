package github.sachin2dehury.musicplayer.ui.fragments

import android.os.Bundle
import android.support.v4.media.session.PlaybackStateCompat
import android.view.View
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.RequestManager
import dagger.hilt.android.AndroidEntryPoint
import github.sachin2dehury.musicplayer.R
import github.sachin2dehury.musicplayer.data.entities.Song
import github.sachin2dehury.musicplayer.exoplayer.isPlaying
import github.sachin2dehury.musicplayer.exoplayer.toSong
import github.sachin2dehury.musicplayer.others.Status
import github.sachin2dehury.musicplayer.ui.viewmodels.MainViewModel
import github.sachin2dehury.musicplayer.ui.viewmodels.SongViewModel
import kotlinx.android.synthetic.main.fragment_song.*
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@AndroidEntryPoint
class SongFragment : Fragment(R.layout.fragment_song) {

    @Inject
    lateinit var glide: RequestManager

    private lateinit var mainViewModel: MainViewModel

    private val songViewModel: SongViewModel by viewModels()

    private var currentPlayingSong: Song? = null

    private var playbackState: PlaybackStateCompat? = null

    private var shouldUpdateSeekBar = true

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mainViewModel = ViewModelProvider(requireActivity()).get(MainViewModel::class.java)

        subscribeToObserver()

        imageViewPlayPauseDetails.setOnClickListener {
            currentPlayingSong?.let { song ->
                mainViewModel.playOrToggleSong(song, true)
            }
        }

        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, time: Int, fromUser: Boolean) {
                if (fromUser) {
                    setCurrentPlayerTime(time.toLong())
                }
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                shouldUpdateSeekBar = false
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                seekBar?.let { bar ->
                    mainViewModel.seekTo(bar.progress.toLong())
                    shouldUpdateSeekBar = true
                }
            }
        })

        imageViewSkipPrevious.setOnClickListener {
            mainViewModel.skipToPreviousSong()
        }

        imageViewSkipNext.setOnClickListener {
            mainViewModel.skipToNextSong()
        }
    }

    private fun updateTitleAndSongName(song: Song) {
        val title = "${song.title} - ${song.subTitle}"
        textViewSongName.text = title
        glide.load(song.imageUrl).into(imageViewSong)
    }

    private fun subscribeToObserver() {
        mainViewModel.mediaItems.observe(viewLifecycleOwner) {
            it?.let { result ->
                when (result.status) {
                    Status.SUCCESS -> {
                        result.data?.let { songs ->
                            if (currentPlayingSong == null && songs.isNotEmpty()) {
                                currentPlayingSong = songs.first()
                                updateTitleAndSongName(songs.first())
                            }
                        }
                    }
                    else -> Unit
                }
            }
        }

        mainViewModel.currentSongPlaying.observe(viewLifecycleOwner) { song ->
            if (song == null) return@observe
            currentPlayingSong = song.toSong()
            updateTitleAndSongName(currentPlayingSong!!)
        }

        mainViewModel.playbackState.observe(viewLifecycleOwner) {
            playbackState = it
            imageViewPlayPauseDetails.setImageResource(
                if (playbackState?.isPlaying == true) R.drawable.ic_baseline_pause_24 else R.drawable.ic_baseline_play_arrow_24
            )
            seekBar.progress = it?.position?.toInt() ?: 0
        }

        songViewModel.currentPlayerPosition.observe(viewLifecycleOwner) { time ->
            if (shouldUpdateSeekBar) {
                seekBar.progress = time.toInt()
                setCurrentPlayerTime(time)
            }
        }

        songViewModel.currentSongDuration.observe(viewLifecycleOwner) { time ->
            seekBar.max = time.toInt()
            val dateFormat = SimpleDateFormat("mm:ss", Locale.getDefault())
            textViewSongDuration.text = dateFormat.format(time)
        }
    }

    private fun setCurrentPlayerTime(time: Long) {
        val dateFormat = SimpleDateFormat("mm:ss", Locale.getDefault())
        textViewCurrentTime.text = dateFormat.format(time)
    }
}