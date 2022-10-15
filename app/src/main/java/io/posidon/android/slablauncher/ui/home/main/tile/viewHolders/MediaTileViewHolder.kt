package io.posidon.android.slablauncher.ui.home.main.tile.viewHolders

import android.app.Activity
import android.content.res.ColorStateList
import android.view.View
import android.widget.ImageView
import androidx.cardview.widget.CardView
import androidx.core.graphics.ColorUtils
import androidx.recyclerview.widget.RecyclerView
import io.posidon.android.conveniencelib.getNavigationBarHeight
import io.posidon.android.conveniencelib.vibrate
import io.posidon.android.slablauncher.R
import io.posidon.android.slablauncher.data.items.App
import io.posidon.android.slablauncher.data.items.LauncherItem
import io.posidon.android.slablauncher.data.notification.MediaPlayerData
import io.posidon.android.slablauncher.providers.color.theme.ColorTheme
import io.posidon.android.slablauncher.providers.item.GraphicsLoader
import io.posidon.android.slablauncher.providers.notification.NotificationService
import io.posidon.android.slablauncher.ui.popup.appItem.ItemLongPress
import io.posidon.android.slablauncher.util.storage.Settings

class MediaTileViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), TileViewHolder {

    private val card = itemView.findViewById<CardView>(R.id.card)!!
    private val backgroundView = itemView.findViewById<ImageView>(R.id.background_image)!!
    val icon = itemView.findViewById<ImageView>(R.id.icon)!!

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

    override fun bind(
        item: LauncherItem,
        activity: Activity,
        settings: Settings,
        graphicsLoader: GraphicsLoader,
        onDragStart: (View) -> Unit,
    ) {
        icon.setImageDrawable(null)
        backgroundView.setImageDrawable(null)
        card.setCardBackgroundColor(ColorTheme.cardBG)

        graphicsLoader.load(itemView.context, item) {
            icon.setImageDrawable(it.icon)
        }

        NotificationService.mediaItem?.let(::updateTint)
        NotificationService.setOnMediaUpdate(::updateTrack)
        updateTrack()

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
        val buttonFGTintList = ColorStateList.valueOf(titleColor)
        val buttonBGTintList = ColorStateList.valueOf(ColorUtils.blendARGB(titleColor, mediaItem.color, 0.95f) and 0xffffff or 0x9d000000.toInt())

        buttonPrev.imageTintList = buttonFGTintList
        buttonNext.imageTintList = buttonFGTintList
        buttonPrev.backgroundTintList = buttonBGTintList
        buttonNext.backgroundTintList = buttonBGTintList

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

    private fun updateTrack() {
        val mediaItem = NotificationService.mediaItem ?: return
        updatePlayPauseButton(mediaItem)
        updateTint(mediaItem)
        val c = mediaItem.image
        if (c == null)
            backgroundView.setImageDrawable(null)
        else
            backgroundView.setImageDrawable(c)
    }
}