package io.posidon.android.slablauncher.ui.home.pinned

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.posidon.android.slablauncher.R
import io.posidon.android.slablauncher.data.items.LauncherItem
import io.posidon.android.slablauncher.providers.color.pallete.ColorPalette
import io.posidon.android.slablauncher.providers.color.theme.ColorTheme
import io.posidon.android.slablauncher.providers.suggestions.SuggestionsManager
import io.posidon.android.slablauncher.ui.home.MainActivity
import io.posidon.android.slablauncher.ui.home.pinned.suggestion.SuggestionsAdapter
import io.posidon.android.slablauncher.ui.popup.home.HomeLongPressPopup
import io.posidon.android.slablauncher.util.view.recycler.RecyclerViewLongPressHelper
import posidon.android.conveniencelib.Device
import posidon.android.conveniencelib.getStatusBarHeight

@SuppressLint("ClickableViewAccessibility")
class AtAGlanceArea(val view: View, tileArea: TileArea, mainActivity: MainActivity) {

    companion object {
        const val SUGGESTION_COUNT = 4
    }

    val date = view.findViewById<TextView>(R.id.date)!!
    val suggestionsAdapter = SuggestionsAdapter(mainActivity, mainActivity.settings)
    val suggestionsRecycler = view.findViewById<RecyclerView>(R.id.suggestions_recycler)!!.apply {
        layoutManager = GridLayoutManager(view.context, SUGGESTION_COUNT, RecyclerView.VERTICAL, false)
        adapter = suggestionsAdapter
    }

    private val popupHeight get() = view.height - view.context.getStatusBarHeight() - suggestionsRecycler.height - view.resources.getDimension(R.dimen.item_card_margin).toInt() * 2
    private val popupWidth get() = view.width - view.resources.getDimension(R.dimen.item_card_margin).toInt() * 4

    private var popupX = 0f
    private var popupY = 0f
    init {
        view.setOnTouchListener { _, e ->
            when (e.action and MotionEvent.ACTION_MASK) {
                MotionEvent.ACTION_DOWN -> {
                    popupX = e.rawX
                    popupY = e.rawY
                }
            }
            false
        }
        view.setOnLongClickListener {
            val loc = IntArray(2)
            it.getLocationOnScreen(loc)
            val m = it.resources.getDimension(R.dimen.item_card_margin)
            HomeLongPressPopup.show(
                it,
                if (tileArea.scrollY == 0) Device.screenWidth(it.context) / 2f else popupX,
                if (tileArea.scrollY == 0) popupHeight / 2f + view.context.getStatusBarHeight() + m else popupY,
                mainActivity.settings,
                mainActivity::reloadColorPaletteSync,
                mainActivity::updateColorTheme,
                mainActivity::loadApps,
                mainActivity::reloadBlur,
                if (tileArea.scrollY == 0) popupWidth else ViewGroup.LayoutParams.WRAP_CONTENT,
                if (tileArea.scrollY == 0) popupHeight else HomeLongPressPopup.calculateHeight(it.context),
            )
            true
        }
        RecyclerViewLongPressHelper.setOnLongPressListener(suggestionsRecycler) { v, x, y ->
            val m = v.resources.getDimension(R.dimen.item_card_margin)
            HomeLongPressPopup.show(
                v,
                if (tileArea.scrollY == 0) Device.screenWidth(v.context) / 2f else x,
                if (tileArea.scrollY == 0) popupHeight / 2f + view.context.getStatusBarHeight() + m else y,
                mainActivity.settings,
                mainActivity::reloadColorPaletteSync,
                mainActivity::updateColorTheme,
                mainActivity::loadApps,
                mainActivity::reloadBlur,
                if (tileArea.scrollY == 0) popupWidth else ViewGroup.LayoutParams.WRAP_CONTENT,
                if (tileArea.scrollY == 0) popupHeight else HomeLongPressPopup.calculateHeight(v.context),
            )
        }
    }

    fun updateColorTheme() {
        date.setTextColor(ColorTheme.titleColorForBG(view.context, ColorPalette.wallColor))
    }

    fun updateSuggestions(pinnedItems: List<LauncherItem>) {
        suggestionsAdapter.updateItems((SuggestionsManager.getTimeBasedSuggestions() - pinnedItems.let {
            val s = TileArea.DOCK_ROWS * TileArea.COLUMNS
            if (it.size > s) it.subList(0, s)
            else it
        }.toSet()).let {
            if (it.size > SUGGESTION_COUNT) it.subList(0, SUGGESTION_COUNT)
            else it
        })
    }
}