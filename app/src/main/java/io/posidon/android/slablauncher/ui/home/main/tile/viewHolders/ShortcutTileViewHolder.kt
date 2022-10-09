package io.posidon.android.slablauncher.ui.home.main.tile.viewHolders

import android.app.Activity
import android.content.pm.LauncherApps
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
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
import io.posidon.android.launcherutil.isUserRunning
import io.posidon.android.launcherutil.loader.IconData
import io.posidon.android.slablauncher.providers.item.GraphicsLoader
import io.posidon.android.slablauncher.ui.home.main.HomeArea.Companion.ITEM_HEIGHT
import io.posidon.android.slablauncher.ui.home.main.acrylicBlur
import io.posidon.android.slablauncher.ui.home.main.tile.ShortcutAdapter
import io.posidon.android.slablauncher.ui.view.SeeThroughView
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract

class ShortcutTileViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), TileViewHolder {

    private val card = itemView.findViewById<CardView>(R.id.card)!!
    private val icon = itemView.findViewById<ImageView>(R.id.icon)!!
    private val label = itemView.findViewById<TextView>(R.id.label)!!
    private val shortcutsRecycler = itemView.findViewById<RecyclerView>(R.id.shortcuts)!!.apply {
        layoutManager = LinearLayoutManager(context, RecyclerView.HORIZONTAL, false)
    }

    private val blurBG = itemView.findViewById<SeeThroughView>(R.id.blur_bg)!!.apply {
        viewTreeObserver.addOnPreDrawListener {
            invalidate()
            true
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

    private fun updateGraphics(
        item: LauncherItem,
        iconData: IconData<GraphicsLoader.Extra>,
        settings: Settings,
    ) {
        val itemColor = when {
            settings.doMonochrome -> ColorTheme.cardBG
            else -> iconData.extra.color.let(ColorTheme::tileColor)
        }
        label.setTextColor(ColorTheme.titleColorForBG(itemColor))
        icon.post {
            card.setCardBackgroundColor(itemColor)
            icon.setImageDrawable(iconData.icon)
            icon.alpha = 1f

            if (settings.doMonochrome) {
                icon.colorFilter = ColorMatrixColorFilter(ColorMatrix().apply {
                    setSaturation(0f)
                })
                if (item is App && !itemView.context.isUserRunning(item.userHandle)) {
                    icon.alpha = 0.7f
                    card.setCardBackgroundColor(0)
                }
            } else icon.colorFilter = null
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
        card.setCardBackgroundColor(ColorTheme.cardBG)

        label.text = item.label

        graphicsLoader.load(itemView.context, item) {
            updateGraphics(item, it, settings)
        }

        val shortcuts = (item as? App)?.let {
            val l = activity.getSystemService(LauncherApps::class.java)
            val s = it.getStaticShortcuts(l)
            if (s.size > 3) s.subList(0, 3)
            else if (s.size == 3) s
            else (s + it.getDynamicShortcuts(l)).let { it.subList(0, it.size.coerceAtMost(3)) }
        }?.ifEmpty { null }

        if (shortcuts == null) {
            shortcutsRecycler.adapter = null
            shortcutsRecycler.isVisible = false
        } else {
            shortcutsRecycler.isVisible = true
            val shortcutsAdapter = ShortcutAdapter(shortcuts, graphicsLoader)
            shortcutsRecycler.adapter = shortcutsAdapter
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

    override fun recycle() {
        icon.setImageDrawable(null)
    }
}