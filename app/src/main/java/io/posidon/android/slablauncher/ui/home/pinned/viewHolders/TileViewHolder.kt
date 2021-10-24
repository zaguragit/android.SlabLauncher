package io.posidon.android.slablauncher.ui.home.pinned.viewHolders

import android.app.Activity
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.isVisible
import androidx.palette.graphics.Palette
import androidx.recyclerview.widget.RecyclerView
import io.posidon.android.slablauncher.R
import io.posidon.android.slablauncher.data.items.App
import io.posidon.android.slablauncher.data.items.LauncherItem
import io.posidon.android.slablauncher.providers.color.theme.ColorTheme
import io.posidon.android.slablauncher.providers.suggestions.SuggestionsManager
import io.posidon.android.slablauncher.ui.home.acrylicBlur
import io.posidon.android.slablauncher.ui.home.pinned.TileArea
import io.posidon.android.slablauncher.ui.popup.appItem.ItemLongPress
import io.posidon.android.slablauncher.ui.view.HorizontalAspectRatioLayout
import io.posidon.android.slablauncher.ui.view.SeeThroughView
import posidon.android.conveniencelib.Colors
import posidon.android.conveniencelib.getNavigationBarHeight

class TileViewHolder(
    val card: CardView
) : RecyclerView.ViewHolder(card) {

    val icon = itemView.findViewById<ImageView>(R.id.icon_image)!!
    val label = itemView.findViewById<TextView>(R.id.icon_text)!!

    val iconSmall = itemView.findViewById<ImageView>(R.id.icon_image_small)!!

    val spacer = itemView.findViewById<View>(R.id.spacer)!!

    val lineTitle = itemView.findViewById<TextView>(R.id.line_title)!!
    val lineDescription = itemView.findViewById<TextView>(R.id.line_description)!!

    val imageView = itemView.findViewById<ImageView>(R.id.background_image)!!

    val blurBG = itemView.findViewById<SeeThroughView>(R.id.blur_bg)!!.apply {
        viewTreeObserver.addOnPreDrawListener {
            invalidate()
            true
        }
    }

    val aspect = itemView.findViewById<HorizontalAspectRatioLayout>(R.id.aspect)!!.apply {
        widthToHeight = TileArea.WIDTH_TO_HEIGHT
    }

    fun bind(
        item: LauncherItem,
        activity: Activity,
        onDragStart: (View) -> Unit,
    ) {
        blurBG.drawable = acrylicBlur?.insaneBlurDrawable

        val backgroundColor = ColorTheme.tintAppDrawerItem(item.getColor())
        card.setCardBackgroundColor(backgroundColor)
        label.text = item.label
        label.setTextColor(ColorTheme.titleColorForBG(itemView.context, backgroundColor))
        lineTitle.setTextColor(ColorTheme.titleColorForBG(itemView.context, backgroundColor))
        lineDescription.setTextColor(ColorTheme.textColorForBG(itemView.context, backgroundColor))

        val banner = (item as? App)?.getBanner()
        if (banner?.text == null && banner?.title == null) {
            iconSmall.isVisible = false
            spacer.isVisible = true
            icon.isVisible = true
            icon.setImageDrawable(item.icon)
        } else {
            iconSmall.isVisible = true
            spacer.isVisible = false
            icon.isVisible = false
            iconSmall.setImageDrawable(item.icon)
        }
        applyIfNotNull(lineTitle, banner?.title, TextView::setText)
        applyIfNotNull(lineDescription, banner?.text, TextView::setText)
        if (banner?.background == null) {
            imageView.isVisible = false
        } else {
            imageView.isVisible = true
            imageView.setImageDrawable(banner.background)
            imageView.alpha = banner.bgOpacity
            val palette = Palette.from(banner.background.toBitmap(32, 32)).generate()
            val color = item.getColor()
            val imageColor = palette.getDominantColor(color)
            val newBackgroundColor = ColorTheme.tintAppDrawerItem(imageColor)
            val actuallyBackgroundColor = Colors.blend(imageColor, newBackgroundColor, imageView.alpha)
            val titleColor = ColorTheme.titleColorForBG(itemView.context, actuallyBackgroundColor)
            val textColor = ColorTheme.textColorForBG(itemView.context, actuallyBackgroundColor)

            card.setCardBackgroundColor(backgroundColor)
            label.setTextColor(titleColor)
            lineTitle.setTextColor(titleColor)
            lineDescription.setTextColor(textColor)
        }

        itemView.setOnClickListener {
            SuggestionsManager.onItemOpened(it.context, item)
            item.open(it.context.applicationContext, it)
        }
        itemView.setOnLongClickListener {
            ItemLongPress.onItemLongPress(
                it,
                backgroundColor,
                ColorTheme.titleColorForBG(itemView.context, backgroundColor),
                item,
                activity.getNavigationBarHeight(),
            )
            onDragStart(it)
            true
        }
    }
}

inline fun <T: View, R> applyIfNotNull(view: T, value: R, block: (T, R) -> Unit) {
    if (value == null) {
        view.isVisible = false
    } else {
        view.isVisible = true
        block(view, value)
    }
}