package github.sachin2dehury.musicplayer.ui.fragments

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import github.sachin2dehury.musicplayer.R
import github.sachin2dehury.musicplayer.adapters.SongAdapter
import github.sachin2dehury.musicplayer.ui.viewmodels.MainViewModel
import javax.inject.Inject

class HomeFragment : Fragment(R.layout.fragment_home) {

    lateinit var mainViewModel: MainViewModel

    @Inject
    lateinit var songAdapter: SongAdapter

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mainViewModel = ViewModelProvider(requireActivity()).get(MainViewModel::class.java)

    }
}