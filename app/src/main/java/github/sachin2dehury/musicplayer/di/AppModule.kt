package github.sachin2dehury.musicplayer.di

import android.content.Context
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ApplicationComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import github.sachin2dehury.musicplayer.R
import github.sachin2dehury.musicplayer.exoplayer.MusicServiceConnection
import javax.inject.Singleton

@Module
@InstallIn(ApplicationComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideGlideInstance(
        @ApplicationContext context: Context
    ) = Glide.with(context).setDefaultRequestOptions(
        RequestOptions().apply {
            placeholder(R.drawable.ic_baseline_play_arrow_24)
            error(R.drawable.ic_baseline_play_arrow_24)
            diskCacheStrategy(DiskCacheStrategy.DATA)
        }
    )

    @Provides
    @Singleton
    fun provideMusicServiceConnection(
        @ApplicationContext context: Context
    ) = MusicServiceConnection(context)

}