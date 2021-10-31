package io.posidon.android.slablauncher.ui.home.pinned.viewHolders

import android.app.Activity
import android.view.Gravity
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
import io.posidon.android.slablauncher.ui.home.acrylicBlur
import io.posidon.android.slablauncher.ui.home.pinned.TileArea
import io.posidon.android.slablauncher.ui.popup.appItem.ItemLongPress
import io.posidon.android.slablauncher.ui.view.HorizontalAspectRatioLayout
import io.posidon.android.slablauncher.ui.view.SeeThroughView
import posidon.android.conveniencelib.Colors
import posidon.android.conveniencelib.getNavigationBarHeight
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

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
        val title = ColorTheme.titleColorForBG(itemView.context, backgroundColor)
        label.setTextColor(title)

        val banner = item.getBanner()
        if (banner?.text == null && banner?.title == null) {
            iconSmall.isVisible = false
            spacer.isVisible = true
            icon.isVisible = true
            icon.setImageDrawable(item.icon)
            lineTitle.isVisible = false
            lineDescription.isVisible = false
            label.gravity = Gravity.CENTER_HORIZONTAL
        } else {
            iconSmall.isVisible = true
            spacer.isVisible = false
            icon.isVisible = false
            iconSmall.setImageDrawable(item.icon)
            label.gravity = Gravity.START
            lineTitle.hideIfNullOr(banner.title) {
                text = it
                setTextColor(title)
            }
            lineDescription.hideIfNullOr(banner.text) {
                text = it
                setTextColor(ColorTheme.textColorForBG(itemView.context, backgroundColor))
            }
        }
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
        if (banner?.hideIcon == true) {
            icon.isVisible = false
            iconSmall.isVisible = false
        }

        itemView.setOnClickListener {
            item.open(it.context.applicationContext, it)
        }
        itemView.setOnLongClickListener {
            if (item is App) {
                ItemLongPress.onItemLongPress(
                    it,
                    backgroundColor,
                    ColorTheme.titleColorForBG(itemView.context, backgroundColor),
                    item,
                    activity.getNavigationBarHeight(),
                )
            }
            else ItemLongPress.onItemLongPress(it, item)
            onDragStart(it)
            true
        }
    }
}

@OptIn(ExperimentalContracts::class)
inline fun <T: View, R> T.hideIfNullOr(value: R?, block: T.(R) -> Unit) {
    contract { callsInPlace(block, InvocationKind.AT_MOST_ONCE) }
    if (value == null) {
        isVisible = false
    } else {
        isVisible = true
        block(value)
    }
}