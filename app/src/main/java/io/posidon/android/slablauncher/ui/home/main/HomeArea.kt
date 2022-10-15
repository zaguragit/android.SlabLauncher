package io.posidon.android.slablauncher.ui.home.main

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.Context
import android.view.DragEvent
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.posidon.android.conveniencelib.Device
import io.posidon.android.conveniencelib.units.dp
import io.posidon.android.conveniencelib.units.toFloatPixels
import io.posidon.android.conveniencelib.units.toPixels
import io.posidon.android.slablauncher.LauncherContext
import io.posidon.android.slablauncher.R
import io.posidon.android.slablauncher.data.items.LauncherItem
import io.posidon.android.slablauncher.providers.suggestions.SuggestionsManager
import io.posidon.android.slablauncher.ui.home.MainActivity
import io.posidon.android.slablauncher.ui.home.main.dash.DashArea
import io.posidon.android.slablauncher.ui.home.main.suggestion.SuggestionsAdapter
import io.posidon.android.slablauncher.ui.home.main.tile.PinnedTilesAdapter
import io.posidon.android.slablauncher.ui.popup.appItem.ItemLongPress
import io.posidon.android.slablauncher.util.storage.DoSuggestionStripSetting.doSuggestionStrip
import io.posidon.android.slablauncher.util.storage.SuggestionColumnCount.suggestionColumnCount
import io.posidon.ksugar.delegates.observable
import kotlin.math.abs
import kotlin.properties.Delegates

class HomeArea(
    val view: NestedScrollView,
    private val fragment: HomeAreaFragment,
    private val launcherContext: LauncherContext
) {

    companion object {
        val ITEM_HEIGHT = 128.dp
        const val SUGGESTION_WIDTH_TO_HEIGHT = 5f / 4f

        fun calculateColumns(context: Context): Int =
            Device.screenWidth(context) / ITEM_HEIGHT.toPixels(context)
    }


    inline val scrollY: Int
        get() = view.scrollY

    val dash = DashArea(view.findViewById<ViewGroup>(R.id.dash), this, fragment.requireActivity() as MainActivity)

    val suggestionsAdapter = SuggestionsAdapter(fragment.requireActivity() as MainActivity, launcherContext.settings, (fragment.requireActivity() as MainActivity).graphicsLoader)
    private var suggestionsRecycler = view.findViewById<RecyclerView>(R.id.suggestions_recycler)!!.apply {
        adapter = suggestionsAdapter
    }

    init {
        view.setOnDragListener(::onDrag)
    }

    val pinnedAdapter = PinnedTilesAdapter(fragment.requireActivity() as MainActivity, launcherContext)
    @SuppressLint("ClickableViewAccessibility")
    val pinnedRecycler = view.findViewById<RecyclerView>(R.id.pinned_recycler).apply {
        adapter = pinnedAdapter
        background.alpha = 0
    }

    fun showDropTarget(i: Int, state: ItemLongPress.State?) {
        if (i != -1) pinnedRecycler.isVisible = true
        state?.view ?: pinnedAdapter.showDropTarget(i)
    }

    fun getTileGridIndex(x: Int, y: Int): Int {
        val relativeY: Int = y + scrollY - dash.view.height - pinnedRecycler.paddingTop
        if (relativeY < 0) return -1
        val c = calculateColumns(view.context)
        val gridY = relativeY * c / pinnedRecycler.width
        val gridX = x * c / pinnedRecycler.width
        val i = gridY * c + gridX
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
                val i = getTileGridIndex(event.x.toInt(), event.y.toInt())
                if (v == null)
                    highlightDropArea = true
                showDropTarget(i, state)
            }
            DragEvent.ACTION_DRAG_LOCATION -> {
                val state = event.localState as? ItemLongPress.State?
                val v = state?.view
                val location = state?.location
                val i = getTileGridIndex(event.x.toInt(), event.y.toInt())
                if (v != null && location != null) {
                    val x = abs(event.x - location[0] - v.measuredWidth / 2f)
                    val y = abs(event.y - location[1] - v.measuredHeight / 2f)
                    if (x > v.measuredWidth / 3.5f || y > v.measuredHeight / 3.5f) {
                        ItemLongPress.currentPopup?.dismiss()
                        val i = getTileGridIndex(location[0], location[1])
                        if (i != -1) {
                            pinnedAdapter.onDragOut(v, i)
                        }
                        highlightDropArea = true
                        v.isVisible = true
                        state.view = null
                    }
                }
                showDropTarget(i, state)
                val rowHeight = ITEM_HEIGHT.toFloatPixels(view)
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
                val i = getTileGridIndex(event.x.toInt(), event.y.toInt())
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
        val suggestionColumns = launcherContext.settings.suggestionColumnCount
        pinnedRecycler.layoutManager = GridLayoutManager(
            view.context,
            calculateColumns(view.context),
            RecyclerView.VERTICAL,
            false
        )
        suggestionsRecycler.layoutManager = GridLayoutManager(view.context, suggestionColumns, RecyclerView.VERTICAL, false)
        if (launcherContext.appManager.apps.isNotEmpty())
            updateSuggestions(launcherContext.appManager.pinnedItems)
    }

    fun updateColorTheme() {
        dash.updateColorTheme()
        pinnedAdapter.notifyItemRangeChanged(0, pinnedAdapter.itemCount)
        suggestionsAdapter.notifyItemRangeChanged(0, suggestionsAdapter.itemCount)
    }

    fun onWindowFocusChanged(hasFocus: Boolean) {
        if (hasFocus)
            pinnedAdapter.notifyItemRangeChanged(0, pinnedAdapter.itemCount)
    }

    fun updateSuggestions(pinnedItems: List<LauncherItem>) {
        val columns = launcherContext.settings.suggestionColumnCount
        suggestionsAdapter.updateItems(SuggestionsManager.get().minus(pinnedItems.let {
            val s = (Device.screenHeight(view.context) - pinnedRecycler.top) / ITEM_HEIGHT.toPixels(view) * columns
            if (it.size > s) it.subList(0, s)
            else it
        }.toSet()).let {
            if (it.size > columns) it.subList(0, columns)
            else it
        })
    }
}