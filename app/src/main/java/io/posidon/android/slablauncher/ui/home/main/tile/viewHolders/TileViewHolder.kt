package io.posidon.android.slablauncher.ui.home.main.tile.viewHolders

import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.BlendMode
import android.graphics.Color
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.drawable.Drawable
import android.os.Build
import android.view.View
import android.widget.ImageView
import androidx.cardview.widget.CardView
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.luminance
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.palette.graphics.Palette
import androidx.recyclerview.widget.RecyclerView
import io.posidon.android.computable.compute
import io.posidon.android.computable.syncCompute
import io.posidon.android.slablauncher.R
import io.posidon.android.slablauncher.data.items.App
import io.posidon.android.slablauncher.data.items.LauncherItem
import io.posidon.android.slablauncher.data.items.isUserRunning
import io.posidon.android.slablauncher.providers.color.theme.ColorTheme
import io.posidon.android.slablauncher.ui.home.main.HomeArea
import io.posidon.android.slablauncher.ui.popup.appItem.ItemLongPress
import io.posidon.android.slablauncher.ui.view.HorizontalAspectRatioLayout
import io.posidon.android.slablauncher.util.storage.DoMonochromeIconsSetting.doMonochrome
import io.posidon.android.slablauncher.util.storage.Settings
import io.posidon.android.conveniencelib.getNavigationBarHeight
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

    fun updateBackground(
        item: LauncherItem,
        background: Drawable,
        settings: Settings,
    ) {
        val itemColor = item.color.syncCompute().let {
            when {
                settings.doMonochrome -> {
                    val a = (it.luminance * 255).toInt()
                    Color.rgb(a, a, a)
                }
                else -> it
            }
        }
        val backgroundColor = ColorTheme.tileColor(itemColor)
        imageView.post {
            card.setCardBackgroundColor(backgroundColor)
            imageView.setImageDrawable(background)
            imageView.alpha = 1f
            card.cardElevation = itemView.context.resources.getDimension(R.dimen.item_card_elevation)

            if (settings.doMonochrome) {
                imageView.colorFilter = ColorMatrixColorFilter(ColorMatrix().apply {
                    setSaturation(0f)
                })
                if (item is App && !item.isUserRunning(itemView.context)) {
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
        onDragStart: (View) -> Unit,
    ) {
        imageView.setImageDrawable(null)
        card.setCardBackgroundColor(ColorTheme.cardBG)

        val banner = item.tileImage

        if (banner.isComputed()) {
            val background = banner.computed()
            updateBackground(item, background, settings)
        } else banner.compute { background ->
            updateBackground(item, background, settings)
        }

        itemView.setOnClickListener {
            item.open(it.context.applicationContext, it)
        }
        itemView.setOnLongClickListener { v ->
            if (item is App) {
                item.color.compute {
                    val backgroundColor = ColorTheme.tintPopup(it)
                    ItemLongPress.onItemLongPress(
                        v,
                        backgroundColor,
                        ColorTheme.titleColorForBG(backgroundColor),
                        item,
                        activity.getNavigationBarHeight(),
                    )
                }
            }
            else ItemLongPress.onItemLongPress(v, item)
            onDragStart(v)
            true
        }
    }

    fun recycle(item: LauncherItem) {
        imageView.setImageDrawable(null)
        item.icon.offload()
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