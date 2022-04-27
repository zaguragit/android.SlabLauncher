package io.posidon.android.slablauncher.ui.home.main

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.Context
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
import io.posidon.android.conveniencelib.Device
import io.posidon.android.conveniencelib.units.dp
import io.posidon.android.conveniencelib.units.toPixels
import io.posidon.android.slablauncher.util.storage.ColumnCount.dockColumnCount
import io.posidon.android.slablauncher.util.storage.Settings
import io.posidon.ksugar.delegates.observable
import kotlin.math.abs
import kotlin.math.min
import kotlin.properties.Delegates

class HomeArea(val view: NestedScrollView, val fragment: DashAreaFragment, val launcherContext: LauncherContext) {

    companion object {
        const val WIDTH_TO_HEIGHT = 6f / 5f

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
    }

    fun forceUpdatePinned() {
        pinnedAdapter.forceUpdateItems(launcherContext.appManager.pinnedItems)
    }

    var highlightDropArea by Delegates.observable(false) { new ->
        pinnedRecycler.background.alpha = if (new) 255 else 0
    }

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
}