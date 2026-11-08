package cc.skysparkle.fogwave.player

import android.content.ContentResolver
import android.content.Context
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.MimeTypes
import cc.skysparkle.fogwave.data.model.RadioStation

fun RadioStation.toMediaItem(context: Context): MediaItem {
    val artworkUri = "${ContentResolver.SCHEME_ANDROID_RESOURCE}://${context.packageName}/$iconResId".toUri()
    return MediaItem.Builder()
        .setUri(streamUrl)
        // HLS playlists are picked up by the HLS module; everything else stays a progressive stream.
        .apply { if (streamUrl.substringBefore('?').endsWith(".m3u8")) setMimeType(MimeTypes.APPLICATION_M3U8) }
        .setMediaId(id)
        .setMediaMetadata(
            MediaMetadata.Builder()
                .setTitle(name)
                .setArtist(context.getString(descriptionRes))
                .setStation(name)
                .setArtworkUri(artworkUri)
                .setIsBrowsable(false)
                .setIsPlayable(true)
                .setMediaType(MediaMetadata.MEDIA_TYPE_RADIO_STATION)
                .build()
        )
        .build()
}
