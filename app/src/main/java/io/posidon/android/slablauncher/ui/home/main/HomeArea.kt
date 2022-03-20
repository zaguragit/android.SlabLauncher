package io.posidon.android.slablauncher.ui.home.main

import android.annotation.SuppressLint
import android.content.ClipData
import android.view.*
import androidx.core.view.isVisible
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.posidon.android.slablauncher.LauncherContext
import io.posidon.android.slablauncher.R
import io.posidon.android.slablauncher.ui.home.MainActivity
import io.posidon.android.slablauncher.ui.home.main.dash.DashArea
import io.posidon.android.slablauncher.ui.home.main.tile.PinnedTilesAdapter
import io.posidon.android.slablauncher.ui.popup.appItem.ItemLongPress
import io.posidon.android.slablauncher.ui.popup.home.HomeLongPressPopup
import io.posidon.android.slablauncher.ui.view.recycler.RecyclerViewLongPressHelper
import posidon.android.conveniencelib.Device
import kotlin.math.abs

class HomeArea(val view: NestedScrollView, val fragment: DashAreaFragment, val launcherContext: LauncherContext) {

    companion object {
        const val COLUMNS = 4
        const val DOCK_ROWS = 3
        const val WIDTH_TO_HEIGHT = 6f / 5f
    }

    inline val scrollY: Int
        get() = view.scrollY

    val dash = DashArea(view.findViewById<ViewGroup>(R.id.dash), this, fragment.requireActivity() as MainActivity)

    init {
        val activity = fragment.requireActivity() as MainActivity
        view.setOnScrollChangeListener { v, _, scrollY, _, _ ->
            activity.overlayOpacity = run {
                val tileMargin = v.resources.getDimension(R.dimen.item_card_margin)
                val tileWidth = (Device.screenWidth(v.context) - tileMargin * 2) / COLUMNS - tileMargin * 2
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
        layoutManager = GridLayoutManager(fragment.requireContext(), COLUMNS, RecyclerView.VERTICAL, false)
        adapter = pinnedAdapter
        background.alpha = 0
        val activity = fragment.requireActivity() as MainActivity

        RecyclerViewLongPressHelper.setOnLongPressListener(this) { v, x, y ->
            HomeLongPressPopup.show(
                v, x, y,
                launcherContext.settings,
                activity::reloadColorPaletteSync,
                activity::updateColorTheme,
                activity::loadApps,
                activity::reloadBlur,
                activity::updateLayout,
            )
        }
    }

    fun showDropTarget(i: Int) {
        if (i != -1) pinnedRecycler.isVisible = true
        ItemLongPress.currentPopup ?: pinnedAdapter.showDropTarget(i)
    }

    fun getPinnedItemIndex(x: Float, y: Float): Int {
        var y = y + scrollY - dash.view.height
        if (y < 0) return -1
        val x = x / pinnedRecycler.width * COLUMNS
        y = ((y - pinnedRecycler.paddingTop) / pinnedRecycler.width * COLUMNS) * WIDTH_TO_HEIGHT
        val i = y.toInt() * COLUMNS + x.toInt()
        return if (i > pinnedAdapter.tileCount) -1 else i
    }

    fun onDrop(v: View, i: Int, clipData: ClipData) {
        pinnedAdapter.onDrop(v, i, clipData)
    }

    fun updatePinned() {
        pinnedAdapter.updateItems(launcherContext.appManager.pinnedItems)
    }

    var highlightDropArea = false
        set(value) {
            field = value
            pinnedRecycler.background.alpha = if (value) 255 else 0
        }

    fun onDrag(view: View, event: DragEvent): Boolean {
        when (event.action) {
            DragEvent.ACTION_DRAG_ENTERED,
            DragEvent.ACTION_DRAG_STARTED -> {
                ((event.localState as? Pair<*, *>?)?.first as? View)?.visibility = View.INVISIBLE
                val i = getPinnedItemIndex(event.x, event.y)
                if (ItemLongPress.currentPopup == null)
                    highlightDropArea = true
                showDropTarget(i)
            }
            DragEvent.ACTION_DRAG_LOCATION -> {
                val pair = (event.localState as? Pair<*, *>?)
                val v = pair?.first as? View
                val location = pair?.second as? IntArray
                val i = getPinnedItemIndex(event.x, event.y)
                if (v != null && location != null) {
                    val x = abs(event.x - location[0] - v.measuredWidth / 2f)
                    val y = abs(event.y - location[1] - v.measuredHeight / 2f)
                    if (x > v.measuredWidth / 3.5f || y > v.measuredHeight / 3.5f) {
                        ItemLongPress.currentPopup?.dismiss()?.let {
                            val i = getPinnedItemIndex(location[0].toFloat(), location[1].toFloat())
                            if (i != -1) {
                                pinnedAdapter.onDragOut(v, i)
                            }
                            highlightDropArea = true
                        }
                    }
                }
                showDropTarget(i)
            }
            DragEvent.ACTION_DRAG_ENDED -> {
                val pair = (event.localState as? Pair<*, *>?)
                val v = pair?.first as? View
                v?.isVisible = true
                ItemLongPress.currentPopup?.isFocusable = true
                ItemLongPress.currentPopup?.update()
                showDropTarget(-1)
                highlightDropArea = false
                updatePinned()
            }
            DragEvent.ACTION_DRAG_EXITED -> {
                showDropTarget(-1)
                highlightDropArea = false
            }
            DragEvent.ACTION_DROP -> {
                ((event.localState as? Pair<*, *>?)?.first as? View)?.isVisible = true
                ItemLongPress.currentPopup?.isFocusable = true
                ItemLongPress.currentPopup?.update()
                val i = getPinnedItemIndex(event.x, event.y)
                if (i == -1)
                    return true
                ItemLongPress.currentPopup ?: onDrop(view, i, event.clipData)
            }
        }
        return true
    }

    fun updateBlur() {
        pinnedAdapter.notifyItemRangeChanged(0, pinnedAdapter.itemCount)
        dash.updateBlur()
    }
}