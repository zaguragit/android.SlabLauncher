package io.posidon.android.slablauncher.ui.home.main.tile.viewHolders

import android.app.Activity
import android.content.pm.LauncherApps
import android.content.res.ColorStateList
import android.graphics.*
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.graphics.applyCanvas
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.luminance
import androidx.core.graphics.toXfermode
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.posidon.android.slablauncher.R
import io.posidon.android.slablauncher.data.items.App
import io.posidon.android.slablauncher.data.items.LauncherItem
import io.posidon.android.slablauncher.providers.color.theme.ColorTheme
import io.posidon.android.slablauncher.ui.popup.appItem.ItemLongPress
import io.posidon.android.slablauncher.util.storage.DoMonochromeIconsSetting.doMonochrome
import io.posidon.android.slablauncher.util.storage.Settings
import io.posidon.android.conveniencelib.getNavigationBarHeight
import io.posidon.android.conveniencelib.units.toPixels
import io.posidon.android.conveniencelib.vibrate
import io.posidon.android.launcherutil.isUserRunning
import io.posidon.android.launcherutil.loader.IconData
import io.posidon.android.slablauncher.data.notification.MediaPlayerData
import io.posidon.android.slablauncher.providers.item.GraphicsLoader
import io.posidon.android.slablauncher.providers.notification.NotificationService
import io.posidon.android.slablauncher.ui.home.main.HomeArea.Companion.ITEM_HEIGHT
import io.posidon.android.slablauncher.ui.home.main.acrylicBlur
import io.posidon.android.slablauncher.ui.home.main.tile.ShortcutAdapter
import io.posidon.android.slablauncher.ui.popup.appItem.LongPressShortcutAdapter
import io.posidon.android.slablauncher.ui.view.SeeThroughView
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

class MediaTileViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), TileViewHolder {

    private val card = itemView.findViewById<CardView>(R.id.card)!!
    private val backgroundView = itemView.findViewById<ImageView>(R.id.background_image)!!
    private val icon = itemView.findViewById<ImageView>(R.id.icon)!!
    private val title = itemView.findViewById<TextView>(R.id.title)!!
    private val subtitle = itemView.findViewById<TextView>(R.id.subtitle)!!

    private val blurBG = itemView.findViewById<SeeThroughView>(R.id.blur_bg)!!.apply {
        viewTreeObserver.addOnPreDrawListener {
            invalidate()
            true
        }
    }

    private val buttonPrev = itemView.findViewById<ImageView>(R.id.button_prev)!!.apply {
        setOnClickListener {
            it.context.vibrate(1)
            NotificationService.mediaItem?.previous?.invoke(it)
        }
    }
    private val buttonPlay = itemView.findViewById<ImageView>(R.id.button_play)!!.apply {
        setOnClickListener {
            it.context.vibrate(1)
            NotificationService.mediaItem?.togglePause?.invoke(this)
        }
    }
    private val buttonNext = itemView.findViewById<ImageView>(R.id.button_next)!!.apply {
        setOnClickListener {
            it.context.vibrate(1)
            NotificationService.mediaItem?.next?.invoke(it)
        }
    }

    init {
        itemView.updateLayoutParams {
            height = ITEM_HEIGHT.toPixels(itemView)
        }
        icon.updateLayoutParams {
            width = ITEM_HEIGHT.toPixels(itemView)
        }
    }

    override fun bind(
        item: LauncherItem,
        activity: Activity,
        settings: Settings,
        graphicsLoader: GraphicsLoader,
        onDragStart: (View) -> Unit,
    ) {
        blurBG.drawable = acrylicBlur?.insaneBlurDrawable
        icon.setImageDrawable(null)
        backgroundView.setImageDrawable(null)
        card.setCardBackgroundColor(ColorTheme.cardBG)

        graphicsLoader.load(itemView.context, item) {
            icon.setImageDrawable(it.icon)
        }

        NotificationService.mediaItem?.let(::updateTint)
        NotificationService.setOnMediaUpdate(::updateTrack)
        updateTrack()

        itemView.setOnClickListener {
            item.open(it.context.applicationContext, it)
        }
        itemView.setOnLongClickListener { v ->
            if (item is App) {
                val color = graphicsLoader.load(itemView.context, item).extra.color
                val backgroundColor = ColorTheme.tintPopup(color)
                ItemLongPress.onItemLongPress(
                    v,
                    backgroundColor,
                    ColorTheme.titleColorForBG(backgroundColor),
                    item,
                    activity.getNavigationBarHeight(),
                    graphicsLoader,
                )
            }
            else ItemLongPress.onItemLongPress(v, item)
            onDragStart(v)
            true
        }
    }

    override fun recycle() {
        icon.setImageDrawable(null)
        backgroundView.setImageDrawable(null)
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

        card.setCardBackgroundColor(cardBGColor)

        buttonPlay.backgroundTintList = ColorStateList.valueOf(buttonBGColor)
        buttonPlay.imageTintList = ColorStateList.valueOf(buttonFGColor)
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
        val mediaItem = NotificationService.mediaItem ?: return
        updatePlayPauseButton(mediaItem)
        title.text = mediaItem.title
        subtitle.text = mediaItem.subtitle
        updateTint(mediaItem)
        val c = mediaItem.image
        if (c == null)
            backgroundView.setImageDrawable(null)
        else
            backgroundView.setImageBitmap(generateMediaImage(c))
    }
}