package github.sachin2dehury.musicplayer.ui

import android.os.Bundle
import android.support.v4.media.session.PlaybackStateCompat
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.navigation.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.RequestManager
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import github.sachin2dehury.musicplayer.R
import github.sachin2dehury.musicplayer.adapters.SwipeSongAdapter
import github.sachin2dehury.musicplayer.data.entities.Song
import github.sachin2dehury.musicplayer.exoplayer.isPlaying
import github.sachin2dehury.musicplayer.exoplayer.toSong
import github.sachin2dehury.musicplayer.others.Constants.UNKNOWN_ERROR
import github.sachin2dehury.musicplayer.others.Status
import github.sachin2dehury.musicplayer.ui.viewmodels.MainViewModel
import kotlinx.android.synthetic.main.activity_main.*
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private val mainViewModel: MainViewModel by viewModels()

    @Inject
    lateinit var swipeSongAdapter: SwipeSongAdapter

    @Inject
    lateinit var glide: RequestManager

    private var currentPlayingSong: Song? = null

    private var playbackState: PlaybackStateCompat? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        subscribeToObservers()

        viewPagerSongs.adapter = swipeSongAdapter

        viewPagerSongs.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                if (playbackState?.isPlaying == true) {
                    mainViewModel.playOrToggleSong(swipeSongAdapter.songs[position])
                } else {
                    currentPlayingSong = swipeSongAdapter.songs[position]
                }
            }
        })

        imageViewPlayPause.setOnClickListener {
            currentPlayingSong?.let { song ->
                mainViewModel.playOrToggleSong(song, true)
            }
        }

        swipeSongAdapter.setItemClickListener {
            navHostFragment.findNavController().navigate(
                R.id.globalActionToSongFragment
            )
        }

        navHostFragment.findNavController().addOnDestinationChangedListener { _, destination, _ ->
            when (destination.id) {
                R.id.songFragment -> hideBottomBar()
                else -> showBottomBar()
            }
        }
    }

    private fun hideBottomBar() {
        imageViewCurrentSong.isVisible = false
        viewPagerSongs.isVisible = false
        imageViewPlayPause.isVisible = false
    }

    private fun showBottomBar() {
        imageViewCurrentSong.isVisible = true
        viewPagerSongs.isVisible = true
        imageViewPlayPause.isVisible = true
    }

    private fun switchViewPagerToCurrentSong(song: Song) {
        val newItemIndex = swipeSongAdapter.songs.indexOf(song)
        if (newItemIndex != -1) {
            viewPagerSongs.currentItem = newItemIndex
            currentPlayingSong = song
        }
    }

    private fun subscribeToObservers() {
        mainViewModel.mediaItems.observe(this) {
            it?.let { result ->
                when (result.status) {
                    Status.SUCCESS -> {
                        result.data?.let { songs ->
                            swipeSongAdapter.songs = songs
                            if (songs.isNotEmpty()) {
                                glide.load((currentPlayingSong ?: songs.first()).imageUrl)
                                    .into(imageViewCurrentSong)
                            }
                            switchViewPagerToCurrentSong(currentPlayingSong ?: return@observe)
                        }
                    }
                    else -> Unit
                }
            }
        }

        mainViewModel.currentSongPlaying.observe(this) { song ->
            if (song == null) return@observe

            currentPlayingSong = song.toSong()
            glide.load(currentPlayingSong?.imageUrl).into(imageViewCurrentSong)
            switchViewPagerToCurrentSong(currentPlayingSong ?: return@observe)
        }

        mainViewModel.playbackState.observe(this) {
            playbackState = it
            imageViewPlayPause.setImageResource(
                if (playbackState?.isPlaying == true) R.drawable.ic_baseline_pause_24 else R.drawable.ic_baseline_play_arrow_24
            )
        }

        mainViewModel.isConnected.observe(this) {
            it?.getContentIfNotHandled()?.let { result ->
                when (result.status) {
                    Status.ERROR -> Snackbar.make(
                        rootLayout,
                        result.message ?: UNKNOWN_ERROR,
                        Snackbar.LENGTH_LONG
                    ).show()
                    else -> Unit
                }
            }
        }

        mainViewModel.networkError.observe(this) {
            it?.getContentIfNotHandled()?.let { result ->
                when (result.status) {
                    Status.ERROR -> Snackbar.make(
                        rootLayout,
                        result.message ?: UNKNOWN_ERROR,
                        Snackbar.LENGTH_LONG
                    ).show()
                    else -> Unit
                }
            }
        }
    }
}