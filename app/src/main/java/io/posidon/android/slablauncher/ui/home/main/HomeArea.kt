package io.posidon.android.slablauncher.ui.home.main

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.Context
import android.view.DragEvent
import android.view.View
import android.view.ViewGroup
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.posidon.android.conveniencelib.Device
import io.posidon.android.slablauncher.LauncherContext
import io.posidon.android.slablauncher.R
import io.posidon.android.slablauncher.data.items.LauncherItem
import io.posidon.android.slablauncher.providers.suggestions.SuggestionsManager
import io.posidon.android.slablauncher.ui.home.MainActivity
import io.posidon.android.slablauncher.ui.home.main.dash.DashArea
import io.posidon.android.slablauncher.ui.home.main.suggestion.SuggestionsAdapter
import io.posidon.android.slablauncher.ui.home.main.tile.PinnedTilesAdapter
import io.posidon.android.slablauncher.ui.popup.appItem.ItemLongPress
import io.posidon.android.slablauncher.ui.popup.home.HomeLongPressPopup
import io.posidon.android.slablauncher.ui.view.recycler.RecyclerViewLongPressHelper
import io.posidon.android.slablauncher.util.storage.ColumnCount.dockColumnCount
import io.posidon.android.slablauncher.util.storage.DoAlignMediaPlayerToTop.alignMediaPlayerToTop
import io.posidon.android.slablauncher.util.storage.DoSuggestionStripSetting.doSuggestionStrip
import io.posidon.android.slablauncher.util.storage.DockRowCount.dockRowCount
import io.posidon.android.slablauncher.util.storage.Settings
import io.posidon.android.slablauncher.util.storage.SuggestionColumnCount.suggestionColumnCount
import io.posidon.ksugar.delegates.observable
import kotlin.math.abs
import kotlin.math.min
import kotlin.properties.Delegates

class HomeArea(
    val view: NestedScrollView,
    private val fragment: HomeAreaFragment,
    private val launcherContext: LauncherContext
) {

    companion object {
        const val WIDTH_TO_HEIGHT = 6f / 5f
        const val SUGGESTION_WIDTH_TO_HEIGHT = 5f / 3f

        fun calculateColumns(context: Context, settings: Settings): Int =
            Device.screenWidth(context) / (
                min(
                    Device.screenWidth(context),
                    Device.screenHeight(context)
                ) / settings.dockColumnCount.coerceAtLeast(1)
            )
    }


    inline val scrollY: Int
        get() = view.scrollY

    val dash = DashArea(view.findViewById<ViewGroup>(R.id.dash), this, fragment.requireActivity() as MainActivity)

    val suggestionsAdapter = SuggestionsAdapter(fragment.requireActivity() as MainActivity, launcherContext.settings, (fragment.requireActivity() as MainActivity).graphicsLoader)
    private var suggestionsRecycler = view.findViewById<RecyclerView>(R.id.suggestions_recycler)!!.apply {
        adapter = suggestionsAdapter
    }

    init {
        val activity = fragment.requireActivity() as MainActivity
        view.setOnScrollChangeListener { v, _, scrollY, _, _ ->
            activity.overlayOpacity = run {
                val tileMargin = v.resources.getDimension(R.dimen.item_card_margin)
                val tileWidth = (Device.screenWidth(v.context) - tileMargin * 2) / calculateColumns(view.context, launcherContext.settings) - tileMargin * 2
                val tileHeight = tileWidth / WIDTH_TO_HEIGHT
                val dockRowHeight = (tileHeight + tileMargin * 2)
                (scrollY / dockRowHeight).coerceAtMost(1f)
            }
            activity.updateBlurLevel()
        }
        view.setOnDragListener(::onDrag)
    }

    val pinnedAdapter = PinnedTilesAdapter(fragment.requireActivity() as MainActivity, launcherContext, fragment)
    @SuppressLint("ClickableViewAccessibility")
    val pinnedRecycler = view.findViewById<RecyclerView>(R.id.pinned_recycler).apply {
        adapter = pinnedAdapter
        background.alpha = 0
        val activity = fragment.requireActivity() as MainActivity

        RecyclerViewLongPressHelper.setOnLongPressListener(this) { v, x, y ->
            HomeLongPressPopup.show(
                v, x, y,
                launcherContext.settings,
                activity::reloadColorPaletteSync,
                activity::updateColorTheme,
                activity::invalidateItemGraphics,
                activity::reloadBlur,
                activity::updateLayout,
                activity::updateGreeting,
            )
        }
    }

    fun showDropTarget(i: Int, state: ItemLongPress.State?) {
        if (i != -1) pinnedRecycler.isVisible = true
        state?.view ?: pinnedAdapter.showDropTarget(i)
    }

    fun getPinnedItemIndex(x: Float, y: Float): Int {
        var y = y + scrollY - dash.view.height
        if (y < 0) return -1
        val x = x / pinnedRecycler.width * calculateColumns(view.context, launcherContext.settings)
        y = ((y - pinnedRecycler.paddingTop) / pinnedRecycler.width * calculateColumns(view.context, launcherContext.settings)) * WIDTH_TO_HEIGHT
        val i = y.toInt() * calculateColumns(view.context, launcherContext.settings) + x.toInt()
        return i.coerceAtMost(pinnedAdapter.tileCount)
    }

    fun onDrop(v: View, i: Int, clipData: ClipData) {
        pinnedAdapter.onDrop(v, i, clipData)
    }

    fun updatePinned() {
        pinnedAdapter.updateItems(launcherContext.appManager.pinnedItems)
        updateSuggestions(launcherContext.appManager.pinnedItems)
    }

    fun forceUpdatePinned() {
        pinnedAdapter.forceUpdateItems(launcherContext.appManager.pinnedItems)
        suggestionsAdapter.updateItems ()
    }

    var highlightDropArea by Delegates.observable(false) { new ->
        pinnedRecycler.background.alpha = if (new) 255 else 0
    }

    private var canAutoScroll = true
    fun onDrag(view: View, event: DragEvent): Boolean {
        when (event.action) {
            DragEvent.ACTION_DRAG_ENTERED,
            DragEvent.ACTION_DRAG_STARTED -> {
                val state = event.localState as? ItemLongPress.State?
                val v = state?.view
                v?.visibility = View.INVISIBLE
                val i = getPinnedItemIndex(event.x, event.y)
                if (v == null)
                    highlightDropArea = true
                showDropTarget(i, state)
            }
            DragEvent.ACTION_DRAG_LOCATION -> {
                val state = event.localState as? ItemLongPress.State?
                val v = state?.view
                val location = state?.location
                val i = getPinnedItemIndex(event.x, event.y)
                if (v != null && location != null) {
                    val x = abs(event.x - location[0] - v.measuredWidth / 2f)
                    val y = abs(event.y - location[1] - v.measuredHeight / 2f)
                    if (x > v.measuredWidth / 3.5f || y > v.measuredHeight / 3.5f) {
                        ItemLongPress.currentPopup?.dismiss()
                        val i = getPinnedItemIndex(location[0].toFloat(), location[1].toFloat())
                        if (i != -1) {
                            pinnedAdapter.onDragOut(v, i)
                        }
                        highlightDropArea = true
                        v.isVisible = true
                        state.view = null
                    }
                }
                showDropTarget(i, state)
                val rowHeight = pinnedRecycler.width /
                    (pinnedRecycler.layoutManager as GridLayoutManager).spanCount /
                    WIDTH_TO_HEIGHT
                if (event.y > view.height - rowHeight / 2) {
                    val r = (view.height - event.y) / rowHeight * 2
                    val t = (690 + 640 * r).toInt()
                    if (canAutoScroll) {
                        canAutoScroll = false
                        view.handler.postDelayed({ canAutoScroll = true }, 256L)
                        this.view.smoothScrollBy(0, rowHeight.toInt(), t)
                    }
                }
            }
            DragEvent.ACTION_DRAG_ENDED -> {
                val state = event.localState as? ItemLongPress.State?
                val v = state?.view
                v?.isVisible = true
                ItemLongPress.currentPopup?.update()
                showDropTarget(-1, state)
                highlightDropArea = false
                updatePinned()
            }
            DragEvent.ACTION_DRAG_EXITED -> {
                val state = event.localState as? ItemLongPress.State?
                showDropTarget(-1, state)
                highlightDropArea = false
            }
            DragEvent.ACTION_DROP -> {
                val v = (event.localState as? ItemLongPress.State?)?.view
                v?.isVisible = true
                ItemLongPress.currentPopup?.update()
                val i = getPinnedItemIndex(event.x, event.y)
                if (i == -1)
                    return true
                v ?: onDrop(view, i, event.clipData)
            }
        }
        return true
    }

    fun updateBlur() {
        pinnedAdapter.notifyItemRangeChanged(0, pinnedAdapter.itemCount)
        dash.updateBlur()
    }

    fun updateLayout() {
        suggestionsRecycler.isVisible = launcherContext.settings.doSuggestionStrip
        dash.playerSpacer.isVisible = !launcherContext.settings.alignMediaPlayerToTop
        dash.suggestionsSpacer.isVisible = launcherContext.settings.alignMediaPlayerToTop
        val columns = calculateColumns(view.context, launcherContext.settings)
        val suggestionColumns = launcherContext.settings.suggestionColumnCount
        pinnedRecycler.layoutManager = GridLayoutManager(
            view.context,
            columns,
            RecyclerView.VERTICAL,
            false
        )
        suggestionsRecycler.layoutManager = GridLayoutManager(view.context, suggestionColumns, RecyclerView.VERTICAL, false)
        if (launcherContext.appManager.apps.isNotEmpty())
            updateSuggestions(launcherContext.appManager.pinnedItems)
        dash.view.doOnLayout {
            it.updateLayoutParams {
                height = fragment.requireView().height - HomeAreaFragment.calculateDockHeight(
                    it.context,
                    launcherContext.settings
                ) - ((fragment.requireActivity() as MainActivity).getSearchBarInset() - it.resources.getDimension(R.dimen.item_card_margin).toInt()) / 2
            }
        }
    }

    fun updateColorTheme() {
        dash.updateColorTheme()
        pinnedAdapter.notifyItemRangeChanged(0, pinnedAdapter.itemCount)
        suggestionsAdapter.notifyItemRangeChanged(0, suggestionsAdapter.itemCount)
    }

    fun onWindowFocusChanged(hasFocus: Boolean) {
        dash.onWindowFocusChanged(hasFocus)
    }

    fun updateSuggestions(pinnedItems: List<LauncherItem>) {
        val columns = launcherContext.settings.suggestionColumnCount
        suggestionsAdapter.updateItems(SuggestionsManager.get().minus(pinnedItems.let {
            val s = launcherContext.settings.dockRowCount * columns
            if (it.size > s) it.subList(0, s)
            else it
        }.toSet()).let {
            if (it.size > columns) it.subList(0, columns)
            else it
        })
    }
}