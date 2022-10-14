package io.posidon.android.slablauncher.ui.home.main.tile.viewHolders

import android.app.Activity
import android.content.pm.LauncherApps
import android.view.View
import androidx.cardview.widget.CardView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.posidon.android.conveniencelib.getNavigationBarHeight
import io.posidon.android.launcherutil.loader.IconData
import io.posidon.android.slablauncher.R
import io.posidon.android.slablauncher.data.items.App
import io.posidon.android.slablauncher.data.items.LauncherItem
import io.posidon.android.slablauncher.providers.color.theme.ColorTheme
import io.posidon.android.slablauncher.providers.item.GraphicsLoader
import io.posidon.android.slablauncher.ui.home.main.acrylicBlur
import io.posidon.android.slablauncher.ui.home.main.tile.ShortcutAdapter
import io.posidon.android.slablauncher.ui.popup.appItem.ItemLongPress
import io.posidon.android.slablauncher.ui.view.SeeThroughView
import io.posidon.android.slablauncher.ui.view.recycler.RecyclerViewLongPressHelper
import io.posidon.android.slablauncher.util.storage.DoMonochromeIconsSetting.doMonochrome
import io.posidon.android.slablauncher.util.storage.Settings

class ShortcutTileViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView), TileViewHolder {

    private val card = itemView.findViewById<CardView>(R.id.card)!!

    private val shortcutsRecycler = itemView.findViewById<RecyclerView>(R.id.shortcuts)!!.apply {
        layoutManager = GridLayoutManager(context, 2, RecyclerView.VERTICAL, false)
    }

    private val blurBG = itemView.findViewById<SeeThroughView>(R.id.blur_bg)!!.apply {
        viewTreeObserver.addOnPreDrawListener {
            invalidate()
            true
        }
    }

    private fun updateGraphics(
        iconData: IconData<GraphicsLoader.Extra>,
        settings: Settings,
    ) {
        val itemColor = when {
            settings.doMonochrome -> ColorTheme.cardBG
            else -> iconData.extra.color.let(ColorTheme::tileColor)
        }
        itemView.post {
            card.setCardBackgroundColor(itemColor)
        }
    }

    override fun bind(
        item: LauncherItem,
        activity: Activity,
        settings: Settings,
        graphicsLoader: GraphicsLoader,
        onDragStart: (View) -> Unit,
    ) {
        itemView.isVisible = true
        blurBG.drawable = acrylicBlur?.insaneBlurDrawable
        card.setCardBackgroundColor(ColorTheme.cardBG)

        graphicsLoader.load(itemView.context, item) {
            updateGraphics(it, settings)
        }

        val shortcuts = listOf(item) + (item as? App)?.let {
            val l = activity.getSystemService(LauncherApps::class.java)
            val s = it.getStaticShortcuts(l)
            if (s.size > 3) s.subList(0, 3)
            else if (s.size == 3) s
            else (s + it.getDynamicShortcuts(l)).let { it.subList(0, it.size.coerceAtMost(3)) }
        }.orEmpty()

        val shortcutsAdapter = ShortcutAdapter(shortcuts, graphicsLoader, settings)
        shortcutsRecycler.adapter = shortcutsAdapter

        RecyclerViewLongPressHelper.setOnLongPressListener(shortcutsRecycler) { v ->
            onLongPress(v, item, activity, graphicsLoader, onDragStart)
        }

        itemView.setOnLongClickListener { v ->
            onLongPress(v, item, activity, graphicsLoader, onDragStart)
            true
        }
    }

    fun onLongPress(
        v: View,
        item: LauncherItem,
        activity: Activity,
        graphicsLoader: GraphicsLoader,
        onDragStart: (View) -> Unit,
    ) {
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
        } else ItemLongPress.onItemLongPress(v, item)
        onDragStart(v)
    }

    override fun recycle() {}
}