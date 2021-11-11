package io.posidon.android.slablauncher.ui.today.viewHolders.media

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.*
import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.graphics.applyCanvas
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.toXfermode
import androidx.recyclerview.widget.RecyclerView
import io.posidon.android.slablauncher.R
import io.posidon.android.slablauncher.data.notification.MediaPlayerData
import io.posidon.android.slablauncher.providers.color.theme.ColorTheme
import io.posidon.android.slablauncher.ui.home.MainActivity
import io.posidon.android.slablauncher.ui.home.acrylicBlur
import io.posidon.android.slablauncher.ui.home.pinned.viewHolders.hideIfNullOr
import io.posidon.android.slablauncher.util.view.SeeThroughView
import posidon.android.conveniencelib.Colors

@SuppressLint("ClickableViewAccessibility")
class MediaPlayerViewHolder(
    itemView: View
) : RecyclerView.ViewHolder(itemView) {

    val card = itemView.findViewById<CardView>(R.id.card)!!

    val cover = itemView.findViewById<ImageView>(R.id.image)!!

    val previous = itemView.findViewById<ImageView>(R.id.button_previous)!!
    val play = itemView.findViewById<ImageView>(R.id.button_play)!!
    val next = itemView.findViewById<ImageView>(R.id.button_next)!!

    val title = itemView.findViewById<TextView>(R.id.title)!!
    val subtitle = itemView.findViewById<TextView>(R.id.subtitle)!!

    val blurBG = itemView.findViewById<SeeThroughView>(R.id.blur_bg)!!.apply {
        viewTreeObserver.addOnPreDrawListener {
            invalidate()
            true
        }
    }

    init {
        previous.setOnTouchListener(::onButtonTouch)
        next.setOnTouchListener(::onButtonTouch)
        play.setOnTouchListener(::onButtonTouch)
    }

    fun onBind(
        mediaData: MediaPlayerData,
        activity: MainActivity,
    ) {
        blurBG.drawable = acrylicBlur?.smoothBlurDrawable
        blurBG.offset = 1f
        activity.setOnPageScrollListener(MediaPlayerViewHolder::class.simpleName!!) { blurBG.offset = it }

        card.setCardBackgroundColor(ColorTheme.cardBG)

        title.text = mediaData.title
        title.setTextColor(ColorTheme.cardTitle)
        subtitle.hideIfNullOr(mediaData.subtitle) {
            text = it
            setTextColor(ColorTheme.cardDescription)
        }

        val c = mediaData.image
        if (c == null)
            cover.setImageDrawable(null)
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
                    0,
                    mediaData.color,
                    Shader.TileMode.CLAMP
                )
                alpha = 100
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
            cover.setImageBitmap(bitmap)
        }

        cover.setOnClickListener(mediaData.onTap)
        title.setOnClickListener(mediaData.onTap)
        subtitle.setOnClickListener(mediaData.onTap)

        val titleColor = Colors.blend(ColorTheme.adjustColorForContrast(ColorTheme.uiBG, mediaData.color), ColorTheme.uiTitle, .3f)
        val titleTintList = ColorStateList.valueOf(titleColor)

        previous.imageTintList = titleTintList
        play.imageTintList = titleTintList
        next.imageTintList = titleTintList

        play.setImageResource(if (mediaData.isPlaying()) R.drawable.ic_pause else R.drawable.ic_play)

        previous.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY_RELEASE)
            }
            mediaData.previous(it)
        }
        next.setOnClickListener {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY_RELEASE)
            }
            mediaData.next(it)
        }
        play.setOnClickListener {
            it as ImageView
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                it.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY_RELEASE)
            }
            mediaData.togglePause(it)
        }
    }

    private fun onButtonTouch(v: View, event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN)
            v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        return false
    }
}