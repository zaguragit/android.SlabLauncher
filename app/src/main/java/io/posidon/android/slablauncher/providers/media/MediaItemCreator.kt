package io.posidon.android.slablauncher.providers.media

import android.content.Context
import android.graphics.drawable.BitmapDrawable
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.PlaybackState
import androidx.core.content.ContextCompat
import androidx.palette.graphics.Palette
import io.posidon.android.slablauncher.R
import io.posidon.android.slablauncher.data.notification.MediaPlayerData

object MediaItemCreator {

    fun create(context: Context, controller: MediaController, mediaMetadata: MediaMetadata): MediaPlayerData {

        val title = mediaMetadata.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
            ?: mediaMetadata.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: null
        val subtitle = mediaMetadata.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE)
            ?: mediaMetadata.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: mediaMetadata.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
            ?: mediaMetadata.getString(MediaMetadata.METADATA_KEY_DISPLAY_DESCRIPTION)
            ?: null

        val coverBmp = mediaMetadata.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
            ?: mediaMetadata.getBitmap(MediaMetadata.METADATA_KEY_ART)
            ?: mediaMetadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: null

        val albumBmp = mediaMetadata.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
            ?: null

        val cover = coverBmp?.let(::BitmapDrawable) ?: ContextCompat.getDrawable(context, R.drawable.ic_play)!!

        val color = coverBmp?.let {
            Palette.from(it).generate().run {
                getDominantColor(getVibrantColor(0xff000000.toInt()))
            }
        } ?: 0

        return MediaPlayerData(
            color = color,
            title = title.toString(),
            subtitle = subtitle,
            onTap = {
                controller.sessionActivity?.send()
            },
            previous = {
                controller.transportControls.skipToPrevious()
            },
            next = {
                controller.transportControls.skipToNext()
            },
            togglePause = {
                if (controller.playbackState?.state == PlaybackState.STATE_PLAYING) {
                    controller.transportControls.pause()
                    it.setImageResource(R.drawable.ic_play)
                } else {
                    controller.transportControls.play()
                    it.setImageResource(R.drawable.ic_pause)
                }
            },
            isPlaying = {
                controller.playbackState?.state == PlaybackState.STATE_PLAYING
            },
            image = cover,
            sourcePackageName = controller.packageName
        )
    }
}