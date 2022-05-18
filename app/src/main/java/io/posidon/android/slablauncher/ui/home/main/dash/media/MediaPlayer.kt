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

    private val image = view.findViewById<BackdropImageView>(R.id.image)

    private val title = view.findViewById<TextView>(R.id.title)
    private val subtitle = view.findViewById<TextView>(R.id.subtitle)

    private val buttonPrev = view.findViewById<ImageView>(R.id.button_prev).apply {
        setOnClickListener {
            NotificationService.mediaItem?.previous?.invoke(it)
        }
    }
    private val buttonPlay = view.findViewById<ImageView>(R.id.button_play).apply {
        setOnClickListener {
            NotificationService.mediaItem?.togglePause?.invoke(this)
        }
    }
    private val buttonNext = view.findViewById<ImageView>(R.id.button_next).apply {
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
        val buttonBGColor = ColorTheme.tintWithColor(ColorTheme.buttonColorCallToAction, c)
        val buttonFGColor = ColorTheme.titleColorForBG(buttonBGColor)
        val titleColor = ColorTheme.tintWithColor(ColorTheme.cardTitle, c)
        val subtitleColor = ColorTheme.tintWithColor(ColorTheme.cardDescription, c)
        val titleTintList = ColorStateList.valueOf(titleColor)

        title.setTextColor(titleTintList)
        subtitle.setTextColor(subtitleColor)
        buttonPrev.imageTintList = titleTintList
        buttonNext.imageTintList = titleTintList

        buttonPlay.backgroundTintList = ColorStateList.valueOf(buttonBGColor)
        buttonPlay.imageTintList = ColorStateList.valueOf(buttonFGColor)
    }

    fun onWindowFocusChanged(hasFocus: Boolean) {
        if (hasFocus)
            NotificationService.mediaItem?.let { updatePlayPauseButton(it) }
    }

    private fun updatePlayPauseButton(mediaItem: MediaPlayerData) {
        buttonPlay.setImageResource(
            if (mediaItem.isPlaying()) R.drawable.ic_pause
            else R.drawable.ic_play
        )
    }

    private fun updateTrack() {
        val mediaItem = NotificationService.mediaItem
        if (mediaItem != null) {
            view.isVisible = true
            separator.isVisible = true
            updatePlayPauseButton(mediaItem)
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