package io.posidon.android.slablauncher.ui.home.pinned.viewHolders.atAGlance

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.TextView
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.posidon.android.slablauncher.R
import io.posidon.android.slablauncher.data.items.LauncherItem
import io.posidon.android.slablauncher.providers.color.pallete.ColorPalette
import io.posidon.android.slablauncher.providers.color.theme.ColorTheme
import io.posidon.android.slablauncher.providers.suggestions.SuggestionsManager
import io.posidon.android.slablauncher.ui.home.MainActivity
import io.posidon.android.slablauncher.ui.home.pinned.TileArea.Companion.COLUMNS
import io.posidon.android.slablauncher.ui.home.pinned.TileArea.Companion.DOCK_ROWS
import io.posidon.android.slablauncher.ui.home.pinned.TileArea.Companion.WIDTH_TO_HEIGHT
import io.posidon.android.slablauncher.ui.home.pinned.TileAreaFragment
import io.posidon.android.slablauncher.ui.home.pinned.viewHolders.atAGlance.suggestion.SuggestionsAdapter
import io.posidon.android.slablauncher.ui.popup.home.HomeLongPressPopup
import io.posidon.android.slablauncher.util.view.recycler.RecyclerViewLongPressHelper
import posidon.android.conveniencelib.Device
import posidon.android.conveniencelib.getNavigationBarHeight
import posidon.android.conveniencelib.getStatusBarHeight

@SuppressLint("ClickableViewAccessibility")
class AtAGlanceViewHolder(
    itemView: View,
    mainActivity: MainActivity,
    fragment: TileAreaFragment,
) : RecyclerView.ViewHolder(itemView) {

    companion object {
        const val SUGGESTION_COUNT = 4
    }

    val date = itemView.findViewById<TextView>(R.id.date)!!
    val suggestionsAdapter = SuggestionsAdapter(mainActivity, mainActivity.settings)
    val suggestionsRecycler = itemView.findViewById<RecyclerView>(R.id.suggestions_recycler)!!.apply {
        layoutManager = GridLayoutManager(context, SUGGESTION_COUNT, RecyclerView.VERTICAL, false)
        adapter = suggestionsAdapter
    }

    private val popupHeight get() = itemView.height - suggestionsRecycler.height - itemView.resources.getDimension(R.dimen.item_card_margin).toInt() * 2
    private val popupWidth get() = itemView.width - itemView.resources.getDimension(R.dimen.item_card_margin).toInt() * 2

    private var popupX = 0f
    private var popupY = 0f
    init {
        itemView.setOnTouchListener { _, e ->
            when (e.action and MotionEvent.ACTION_MASK) {
                MotionEvent.ACTION_DOWN -> {
                    popupX = e.rawX
                    popupY = e.rawY
                }
            }
            false
        }
        itemView.setOnLongClickListener {
            val loc = IntArray(2)
            it.getLocationOnScreen(loc)
            val m = it.resources.getDimension(R.dimen.item_card_margin).toInt()
            HomeLongPressPopup.show(
                it,
                if (fragment.tileArea.scrollY == 0) Device.screenWidth(it.context) / 2f else popupX,
                if (fragment.tileArea.scrollY == 0) it.height / 2f + m * 3 else popupY,
                mainActivity.getNavigationBarHeight(),
                mainActivity.settings,
                mainActivity::reloadColorPaletteSync,
                mainActivity::updateColorTheme,
                mainActivity::loadApps,
                mainActivity::reloadBlur,
                if (fragment.tileArea.scrollY == 0) popupWidth else WRAP_CONTENT,
                if (fragment.tileArea.scrollY == 0) popupHeight else HomeLongPressPopup.calculateHeight(it.context),
            )
            true
        }
        RecyclerViewLongPressHelper.setOnLongPressListener(suggestionsRecycler) { v, x, y ->
            HomeLongPressPopup.show(
                v,
                if (fragment.tileArea.scrollY == 0) Device.screenWidth(v.context) / 2f else x,
                y,
                mainActivity.getNavigationBarHeight(),
                mainActivity.settings,
                mainActivity::reloadColorPaletteSync,
                mainActivity::updateColorTheme,
                mainActivity::loadApps,
                mainActivity::reloadBlur,
                if (fragment.tileArea.scrollY == 0) popupWidth else WRAP_CONTENT,
                if (fragment.tileArea.scrollY == 0) popupHeight else HomeLongPressPopup.calculateHeight(v.context),
            )
        }
        itemView.updateLayoutParams {
            val tileMargin = itemView.context.resources.getDimension(R.dimen.item_card_margin)
            val tileWidth = (Device.screenWidth(itemView.context) - tileMargin * 2) / COLUMNS - tileMargin * 2
            val tileHeight = tileWidth / WIDTH_TO_HEIGHT
            val dockHeight = DOCK_ROWS * (tileHeight + tileMargin * 2)
            height = fragment.requireView().height - itemView.context.getStatusBarHeight() - (tileMargin * 2 + dockHeight.toInt()).toInt() + 1
        }
    }

    fun onBind(pinnedItems: List<LauncherItem>) {
        date.setTextColor(ColorTheme.titleColorForBG(itemView.context, ColorPalette.wallColor))
        suggestionsAdapter.updateItems((SuggestionsManager.getTimeBasedSuggestions() - pinnedItems.let {
            val s = DOCK_ROWS * COLUMNS
            if (it.size > s) it.subList(0, s)
            else it
        }.toSet()).let {
            if (it.size > SUGGESTION_COUNT) it.subList(0, SUGGESTION_COUNT)
            else it
        })
    }
}