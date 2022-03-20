package io.posidon.android.slablauncher.ui.home.main.dash.media

import android.content.res.ColorStateList
import android.graphics.*
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.graphics.applyCanvas
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.toXfermode
import androidx.core.view.isVisible
import io.posidon.android.slablauncher.R
import io.posidon.android.slablauncher.data.notification.MediaPlayerData
import io.posidon.android.slablauncher.providers.color.theme.ColorTheme
import io.posidon.android.slablauncher.providers.notification.NotificationService
import io.posidon.android.slablauncher.ui.view.BackdropImageView

class MediaPlayer(val view: ViewGroup, val separator: View) {

    val image = view.findViewById<BackdropImageView>(R.id.image)

    val title = view.findViewById<TextView>(R.id.title)
    val subtitle = view.findViewById<TextView>(R.id.subtitle)

    val buttonPrev = view.findViewById<ImageView>(R.id.button_prev).apply {
        setOnClickListener {
            NotificationService.mediaItem?.previous?.invoke(it)
        }
    }
    val buttonPlay = view.findViewById<ImageView>(R.id.button_play).apply {
        setOnClickListener {
            NotificationService.mediaItem?.togglePause?.invoke(this)
        }
    }
    val buttonNext = view.findViewById<ImageView>(R.id.button_next).apply {
        setOnClickListener {
            NotificationService.mediaItem?.next?.invoke(it)
        }
    }

    init {
        NotificationService.setOnMediaUpdate(::updateTrack)
        updateTrack()
    }

    fun updateColorTheme() {
        NotificationService.mediaItem?.let(::updateTint)
    }

    private fun updateTint(mediaItem: MediaPlayerData) {
        val c = ColorTheme.adjustColorForContrast(ColorTheme.cardBG, mediaItem.color)
        val titleColor = ColorTheme.tintWithColor(ColorTheme.cardTitle, c)
        val subtitleColor = ColorTheme.tintWithColor(ColorTheme.cardDescription, c)
        val titleTintList = ColorStateList.valueOf(titleColor)

        title.setTextColor(titleTintList)
        subtitle.setTextColor(subtitleColor)
        buttonPrev.imageTintList = titleTintList
        buttonPlay.imageTintList = titleTintList
        buttonNext.imageTintList = titleTintList
    }

    private fun updateTrack() {
        val mediaItem = NotificationService.mediaItem
        if (mediaItem != null) {
            view.isVisible = true
            separator.isVisible = true
            buttonPlay.setImageResource(
                if (mediaItem.isPlaying.invoke()) R.drawable.ic_play
                else R.drawable.ic_pause
            )
            title.text = mediaItem.title
            subtitle.text = mediaItem.subtitle

            updateTint(mediaItem)

            val c = mediaItem.image
            if (c == null)
                image.setImageDrawable(null)
            else {
                val paint = Paint().apply {
                    shader = LinearGradient(
                        c.intrinsicWidth.toFloat() / 2f,
                        0f,
                        c.intrinsicWidth.toFloat(),
                        0f,
                        intArrayOf(
                            0,
                            0x33000000.toInt(),
                            0x88000000.toInt(),
                            0xdd000000.toInt(),
                            0xff000000.toInt()
                        ),
                        floatArrayOf(
                            0f, .25f, .5f, .75f, 1f
                        ),
                        Shader.TileMode.CLAMP
                    )
                    xfermode = PorterDuff.Mode.DST_IN.toXfermode()
                }
                val paint2 = Paint().apply {
                    shader = LinearGradient(
                        0f,
                        0f,
                        c.intrinsicWidth.toFloat() * 1.5f,
                        0f,
                        mediaItem.color and 0xffffff,
                        mediaItem.color,
                        Shader.TileMode.CLAMP
                    )
                    alpha = 200
                    xfermode = PorterDuff.Mode.DST_OVER.toXfermode()
                }
                val b = c.toBitmap()
                val w = b.width * 1.5f
                val bitmap = Bitmap.createBitmap(w.toInt(), b.height, b.config).applyCanvas {
                    val x = (width - c.intrinsicWidth).toFloat()
                    drawBitmap(b, x, 0f, paint2)
                    drawRect(x, 0f, c.intrinsicWidth.toFloat(), height.toFloat(), paint)
                    drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint2)
                }
                image.setImageBitmap(bitmap)
            }
        } else {
            view.isVisible = false
            separator.isVisible = false
        }
    }
}