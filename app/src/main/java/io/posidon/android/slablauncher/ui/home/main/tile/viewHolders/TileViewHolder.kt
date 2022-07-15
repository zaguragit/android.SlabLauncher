package io.posidon.android.slablauncher.ui.home.main.tile.viewHolders

import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.view.View
import android.widget.ImageView
import androidx.cardview.widget.CardView
import androidx.core.graphics.luminance
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import io.posidon.android.slablauncher.R
import io.posidon.android.slablauncher.data.items.App
import io.posidon.android.slablauncher.data.items.LauncherItem
import io.posidon.android.slablauncher.providers.color.theme.ColorTheme
import io.posidon.android.slablauncher.ui.home.main.HomeArea
import io.posidon.android.slablauncher.ui.popup.appItem.ItemLongPress
import io.posidon.android.slablauncher.ui.view.HorizontalAspectRatioLayout
import io.posidon.android.slablauncher.util.storage.DoMonochromeIconsSetting.doMonochrome
import io.posidon.android.slablauncher.util.storage.Settings
import io.posidon.android.conveniencelib.getNavigationBarHeight
import io.posidon.android.launcherutil.isUserRunning
import io.posidon.android.launcherutil.loader.IconData
import io.posidon.android.slablauncher.providers.item.GraphicsLoader
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

class TileViewHolder(
    val card: CardView
) : RecyclerView.ViewHolder(card) {

    private val aspect = itemView.findViewById<HorizontalAspectRatioLayout>(R.id.aspect)!!.apply {
        widthToHeight = HomeArea.WIDTH_TO_HEIGHT
    }

    private val imageView = itemView.findViewById<ImageView>(R.id.background_image)!!

    private fun updateBackground(
        item: LauncherItem,
        iconData: IconData<GraphicsLoader.Extra>,
        settings: Settings,
    ) {
        val itemColor = iconData.extra.color.let {
            when {
                settings.doMonochrome -> {
                    val a = (it.luminance * 255).toInt()
                    Color.argb(0, a, a, a)
                }
                else -> it
            }
        }
        val backgroundColor = ColorTheme.tileColor(itemColor)
        imageView.post {
            card.setCardBackgroundColor(backgroundColor)
            imageView.setImageDrawable(iconData.extra.tile)
            imageView.alpha = 1f
            card.cardElevation = itemView.context.resources.getDimension(R.dimen.item_card_elevation)

            if (settings.doMonochrome) {
                imageView.colorFilter = ColorMatrixColorFilter(ColorMatrix().apply {
                    setSaturation(0f)
                })
                if (item is App && !itemView.context.isUserRunning(item.userHandle)) {
                    imageView.alpha = 0.7f
                    card.cardElevation = 0f
                    card.setCardBackgroundColor(0)
                }
            } else imageView.colorFilter = null
        }
    }

    fun bind(
        item: LauncherItem,
        activity: Activity,
        settings: Settings,
        graphicsLoader: GraphicsLoader,
        onDragStart: (View) -> Unit,
    ) {
        imageView.setImageDrawable(null)
        card.setCardBackgroundColor(ColorTheme.cardBG)

        val borderTint = if (item is App) {
            val color = graphicsLoader.load(itemView.context, item).extra.color
            ColorTheme.tintWithColor(0xcdcdcd, color)
        } else 0xffffff
        imageView.foregroundTintList = ColorStateList.valueOf(borderTint or 0x55000000.toInt())

        graphicsLoader.load(itemView.context, item) {
            updateBackground(item, it, settings)
        }

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

    fun recycle() {
        imageView.setImageDrawable(null)
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