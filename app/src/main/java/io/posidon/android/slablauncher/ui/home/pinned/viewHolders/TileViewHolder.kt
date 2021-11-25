package io.posidon.android.slablauncher.ui.home.pinned.viewHolders

import android.app.Activity
import android.view.View
import android.widget.ImageView
import androidx.cardview.widget.CardView
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.isVisible
import androidx.palette.graphics.Palette
import androidx.recyclerview.widget.RecyclerView
import io.posidon.android.slablauncher.R
import io.posidon.android.slablauncher.data.items.App
import io.posidon.android.slablauncher.data.items.LauncherItem
import io.posidon.android.slablauncher.providers.color.theme.ColorTheme
import io.posidon.android.slablauncher.ui.home.pinned.TileArea
import io.posidon.android.slablauncher.ui.home.pinned.acrylicBlur
import io.posidon.android.slablauncher.ui.popup.appItem.ItemLongPress
import io.posidon.android.slablauncher.util.view.SeeThroughView
import io.posidon.android.slablauncher.util.view.tile.TileContentView
import posidon.android.conveniencelib.Colors
import posidon.android.conveniencelib.getNavigationBarHeight
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

class TileViewHolder(
    val card: CardView
) : RecyclerView.ViewHolder(card) {

    val contentView = itemView.findViewById<TileContentView>(R.id.tile_content)!!.apply {
        widthToHeight = TileArea.WIDTH_TO_HEIGHT
    }

    val imageView = itemView.findViewById<ImageView>(R.id.background_image)!!

    val blurBG = itemView.findViewById<SeeThroughView>(R.id.blur_bg)!!.apply {
        viewTreeObserver.addOnPreDrawListener {
            invalidate()
            true
        }
    }

    fun bind(
        item: LauncherItem,
        activity: Activity,
        onDragStart: (View) -> Unit,
    ) {
        val banner = item.getBanner()
        contentView.label = item.label
        contentView.icon = if (banner?.hideIcon == true) null else item.icon
        contentView.extraTitle = banner?.title
        contentView.extraText = banner?.text

        val backgroundColor = ColorTheme.tintAppDrawerItem(item.getColor())
        val title = ColorTheme.titleColorForBG(itemView.context, backgroundColor)
        val text = ColorTheme.textColorForBG(itemView.context, backgroundColor)
        val label = ColorTheme.adjustColorForContrast(backgroundColor, backgroundColor)

        contentView.labelColor = label
        contentView.titleColor = title
        contentView.textColor = text

        blurBG.drawable = acrylicBlur?.smoothBlurDrawable

        card.setCardBackgroundColor(backgroundColor)

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
            val labelColor = ColorTheme.adjustColorForContrast(actuallyBackgroundColor, actuallyBackgroundColor)

            card.setCardBackgroundColor(newBackgroundColor)
            contentView.labelColor = labelColor
            contentView.titleColor = titleColor
            contentView.textColor = textColor
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