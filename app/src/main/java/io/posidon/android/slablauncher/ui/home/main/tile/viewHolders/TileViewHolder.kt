package io.posidon.android.slablauncher.ui.home.main.tile.viewHolders

import android.app.Activity
import android.content.res.ColorStateList
import android.graphics.BlendMode
import android.graphics.drawable.Drawable
import android.os.Build
import android.view.View
import android.widget.ImageView
import androidx.cardview.widget.CardView
import androidx.core.graphics.drawable.toBitmap
import androidx.core.view.isVisible
import androidx.palette.graphics.Palette
import androidx.recyclerview.widget.RecyclerView
import io.posidon.android.computable.compute
import io.posidon.android.computable.syncCompute
import io.posidon.android.slablauncher.R
import io.posidon.android.slablauncher.data.items.App
import io.posidon.android.slablauncher.data.items.ContactItem
import io.posidon.android.slablauncher.data.items.LauncherItem
import io.posidon.android.slablauncher.data.items.LauncherItem.Banner.Companion.ALPHA_MULTIPLIER
import io.posidon.android.slablauncher.data.items.getBanner
import io.posidon.android.slablauncher.providers.color.theme.ColorTheme
import io.posidon.android.slablauncher.ui.home.main.HomeArea
import io.posidon.android.slablauncher.ui.home.main.acrylicBlur
import io.posidon.android.slablauncher.ui.popup.appItem.ItemLongPress
import io.posidon.android.slablauncher.util.storage.DoMonochromeIconsSetting.doMonochromeTileBackground
import io.posidon.android.slablauncher.util.storage.Settings
import io.posidon.android.slablauncher.ui.view.SeeThroughView
import io.posidon.android.slablauncher.ui.view.tile.TileContentView
import posidon.android.conveniencelib.getNavigationBarHeight
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

class TileViewHolder(
    val card: CardView
) : RecyclerView.ViewHolder(card) {

    private val contentView = itemView.findViewById<TileContentView>(R.id.tile_content)!!.apply {
        widthToHeight = HomeArea.WIDTH_TO_HEIGHT
    }

    private val imageView = itemView.findViewById<ImageView>(R.id.background_image)!!

    private val blurBG = itemView.findViewById<SeeThroughView>(R.id.blur_bg)!!.apply {
        viewTreeObserver.addOnPreDrawListener {
            invalidate()
            true
        }
    }

    fun updateBackground(
        item: LauncherItem,
        background: Drawable?,
        settings: Settings,
        banner: LauncherItem.Banner
    ) {
        val itemColor = item.color.syncCompute()
        val backgroundColor = ColorTheme.tileColor(itemColor)
        contentView.post {
            card.setCardBackgroundColor(backgroundColor)
        }
        if (background == null) {
            imageView.post {
                imageView.isVisible = false
            }
        } else {
            val imageColor = run {
                val bitmap = background.toBitmap(24, 24)
                val palette = Palette.from(bitmap).generate()
                palette.getDominantColor(itemColor)
                    .also { bitmap.recycle() }
            }
            val newBackgroundColor = ColorTheme.tileColor(imageColor and 0xffffff)

            imageView.post {
                imageView.isVisible = true
                imageView.setImageDrawable(background)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    if (settings.doMonochromeTileBackground && item !is ContactItem) {
                        imageView.imageTintList = ColorStateList.valueOf(backgroundColor)
                        imageView.imageTintBlendMode = BlendMode.COLOR
                    } else imageView.imageTintList = null
                }
                imageView.alpha = banner.bgOpacity * ALPHA_MULTIPLIER

                card.setCardBackgroundColor(newBackgroundColor)
            }
        }
    }

    fun bind(
        item: LauncherItem,
        activity: Activity,
        settings: Settings,
        onDragStart: (View) -> Unit,
    ) {
        blurBG.drawable = acrylicBlur?.insaneBlurDrawable

        imageView.isVisible = false
        imageView.setImageDrawable(null)
        contentView.icon = null
        card.setCardBackgroundColor(ColorTheme.cardBG)

        val banner = item.getBanner()

        if (banner.background.isComputed()) {
            val background = banner.background.computed()
            updateBackground(item, background, settings, banner)
        } else banner.background.compute { background ->
            updateBackground(item, background, settings, banner)
        }
        if (banner.hideIcon)
            contentView.icon = null
        else {
            val ic = item.icon
            if (ic.isComputed())
                contentView.icon = ic.computed()
            else ic.compute {
                contentView.post {
                    contentView.icon = it
                }
            }
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
        blurBG.drawable = null
        imageView.setImageDrawable(null)
        contentView.icon = null
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