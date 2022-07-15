package io.posidon.android.slablauncher.ui.home.main.dash.media

import android.content.res.ColorStateList
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.graphics.applyCanvas
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.toXfermode
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import io.posidon.android.slablauncher.R
import io.posidon.android.slablauncher.data.notification.MediaPlayerData
import io.posidon.android.slablauncher.providers.color.theme.ColorTheme
import io.posidon.android.slablauncher.providers.notification.NotificationService
import io.posidon.android.slablauncher.ui.home.main.acrylicBlur
import io.posidon.android.slablauncher.ui.view.BackdropImageView
import io.posidon.android.slablauncher.ui.view.SeeThroughView

class MediaPlayer(val view: CardView, val updateLayout: () -> Unit) {

    private val image = view.findViewById<BackdropImageView>(R.id.image)!!

    private val blurBG = view.findViewById<SeeThroughView>(R.id.blur_bg)!!.apply {
        viewTreeObserver.addOnPreDrawListener {
            invalidate()
            true
        }
    }

    private val container = view.findViewById<View>(R.id.container)!!

    private val title = view.findViewById<TextView>(R.id.title)!!
    private val subtitle = view.findViewById<TextView>(R.id.subtitle)!!

    private val buttonPrev = view.findViewById<ImageView>(R.id.button_prev)!!.apply {
        setOnClickListener {
            NotificationService.mediaItem?.previous?.invoke(it)
        }
    }
    private val buttonPlay = view.findViewById<ImageView>(R.id.button_play)!!.apply {
        setOnClickListener {
            NotificationService.mediaItem?.togglePause?.invoke(this)
        }
    }
    private val buttonNext = view.findViewById<ImageView>(R.id.button_next)!!.apply {
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
        val buttonBGColor = ColorTheme.tintWithColor(ColorTheme.buttonColorCallToAction, mediaItem.color)
        val buttonFGColor = ColorTheme.titleColorForBG(buttonBGColor)
        val cardBGColor = ColorTheme.tintWithColor(ColorTheme.cardBG, mediaItem.color)
        val titleColor = ColorTheme.tintWithColor(ColorTheme.cardTitle, mediaItem.color)
        val subtitleColor = ColorTheme.tintWithColor(ColorTheme.cardDescription, mediaItem.color)
        val titleTintList = ColorStateList.valueOf(titleColor)
        val hintTintList = ColorStateList.valueOf(titleColor and 0xffffff or 0x33000000)

        title.setTextColor(titleTintList)
        subtitle.setTextColor(subtitleColor)
        buttonPrev.imageTintList = titleTintList
        buttonNext.imageTintList = titleTintList
        buttonPrev.backgroundTintList = hintTintList
        buttonNext.backgroundTintList = hintTintList

        view.setCardBackgroundColor(cardBGColor)
        container.foregroundTintList = ColorStateList.valueOf(ColorTheme.separator)

        buttonPlay.backgroundTintList = ColorStateList.valueOf(buttonBGColor)
        buttonPlay.imageTintList = ColorStateList.valueOf(buttonFGColor)
    }

    fun updateBlur() {
        blurBG.drawable = acrylicBlur?.smoothBlurDrawable
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

    private fun generateMediaImage(c: Drawable): Bitmap {
        val paint = Paint().apply {
            shader = LinearGradient(
                0f,
                0f,
                c.intrinsicWidth.toFloat(),
                0f,
                intArrayOf(
                    0xff000000.toInt(),
                    0xcc000000.toInt(),
                    0x55000000,
                    0,
                ),
                floatArrayOf(
                    0f, .6f, .8f, 1f
                ),
                Shader.TileMode.CLAMP
            )
            xfermode = PorterDuff.Mode.DST_IN.toXfermode()
        }
        val b = c.toBitmap()
        return Bitmap.createBitmap((b.width * 1.2f).toInt(), b.height, b.config).applyCanvas {
            drawBitmap(b, 0f, 0f, Paint())
            drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        }
    }

    private fun updateTrack() {
        val mediaItem = NotificationService.mediaItem
        val shouldShow = mediaItem != null
        val shouldUpdate = shouldShow != view.isVisible
        if (shouldUpdate) {
            view.isVisible = shouldShow
            view.doOnLayout {
                updateLayout()
            }
        }
        if (mediaItem != null) {
            updatePlayPauseButton(mediaItem)
            title.text = mediaItem.title
            subtitle.text = mediaItem.subtitle

            updateTint(mediaItem)

            val c = mediaItem.image
            if (c == null)
                image.setImageDrawable(null)
            else
                image.setImageBitmap(generateMediaImage(c))
        }
    }
}