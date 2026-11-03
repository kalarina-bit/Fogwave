package cc.skysparkle.fogwave

import android.app.Application
import android.content.Context
import cc.skysparkle.fogwave.data.repository.RadioRepository
import cc.skysparkle.fogwave.player.AudioEffectsEngine
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.memory.MemoryCache

class FogwaveApp : Application(), ImageLoaderFactory {
    override fun onCreate() {
        super.onCreate()
        // Opening the preference files starts loading them on a background thread right away, so the
        // first screen and the playback service usually find them loaded instead of waiting on the disk.
        getSharedPreferences(RadioRepository.PREFS_NAME, Context.MODE_PRIVATE)
        getSharedPreferences(AudioEffectsEngine.PREFS_NAME, Context.MODE_PRIVATE)
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.20)
                    .build()
            }
            .crossfade(true)
            .build()
    }
}
