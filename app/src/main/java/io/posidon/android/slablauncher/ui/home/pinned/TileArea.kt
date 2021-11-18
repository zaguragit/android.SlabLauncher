package io.posidon.android.slablauncher.ui.home.pinned

import android.annotation.SuppressLint
import android.content.ClipData
import android.view.*
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.posidon.android.slablauncher.LauncherContext
import io.posidon.android.slablauncher.R
import io.posidon.android.slablauncher.providers.notification.NotificationService
import io.posidon.android.slablauncher.ui.home.LauncherFragment
import io.posidon.android.slablauncher.ui.home.MainActivity
import io.posidon.android.slablauncher.ui.popup.appItem.ItemLongPress
import io.posidon.android.slablauncher.ui.popup.home.HomeLongPressPopup
import posidon.android.conveniencelib.getNavigationBarHeight
import kotlin.math.abs


class TileArea(view: View, val fragment: LauncherFragment, val launcherContext: LauncherContext) {

    companion object {
        const val COLUMNS = 3
        const val DOCK_ROWS = 3
        const val WIDTH_TO_HEIGHT = 5f / 4f
    }

    var scrollY: Int = 0
        private set
    val pinnedAdapter = PinnedTilesAdapter(fragment.requireActivity() as MainActivity, launcherContext)
    @SuppressLint("ClickableViewAccessibility")
    val pinnedRecycler = view.findViewById<RecyclerView>(R.id.pinned_recycler).apply {
        layoutManager = GridLayoutManager(fragment.requireContext(), COLUMNS, RecyclerView.VERTICAL, false).apply {
            spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(i: Int): Int {
                    return if (i == 0) COLUMNS else 1
                }
            }
        }
        adapter = pinnedAdapter
        setOnDragListener(::onDrag)
        val activity = fragment.requireActivity() as MainActivity
        NotificationService.setOnUpdate(TileArea::class.simpleName!!) {
            activity.runOnUiThread(pinnedAdapter::notifyDataSetChanged)
        }

        var popupX = 0f
        var popupY = 0f
        val onLongPress = Runnable {
            performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
            HomeLongPressPopup.show(
                this,
                popupX,
                popupY,
                activity.getNavigationBarHeight(),
                launcherContext.settings,
                activity::reloadColorPaletteSync,
                activity::updateColorTheme,
                activity::loadApps,
                activity::reloadBlur,
            )
        }
        var lastRecyclerViewDownTouchEvent: MotionEvent? = null
        setOnTouchListener { v, event ->
            when (event.action and MotionEvent.ACTION_MASK) {
                MotionEvent.ACTION_DOWN -> {
                    popupX = event.rawX
                    popupY = event.rawY
                    if (findChildViewUnder(event.x, event.y) == null) {
                        v.handler.removeCallbacks(onLongPress)
                        lastRecyclerViewDownTouchEvent = event
                        v.handler.postDelayed(onLongPress, ViewConfiguration.getLongPressTimeout().toLong())
                    }
                }
                MotionEvent.ACTION_MOVE -> if (lastRecyclerViewDownTouchEvent != null) {
                    val xDelta = abs(popupX - event.x)
                    val yDelta = abs(popupY - event.y)
                    if (xDelta >= 10 || yDelta >= 10) {
                        v.handler.removeCallbacks(onLongPress)
                        lastRecyclerViewDownTouchEvent = null
                    }
                }
                MotionEvent.ACTION_CANCEL,
                MotionEvent.ACTION_UP -> {
                    v.handler.removeCallbacks(onLongPress)
                    lastRecyclerViewDownTouchEvent = null
                }
            }
            false
        }
        addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                this@TileArea.scrollY += dy
            }
        })
    }

    fun showDropTarget(i: Int) {
        if (i != -1) pinnedRecycler.isVisible = true
        pinnedAdapter.showDropTarget(i)
    }

    fun getPinnedItemIndex(x: Float, y: Float): Int {
        var y = y - pinnedAdapter.verticalOffset + scrollY
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

    fun onDrag(view: View, event: DragEvent): Boolean {
        when (event.action) {
            DragEvent.ACTION_DRAG_ENTERED,
            DragEvent.ACTION_DRAG_STARTED -> {
                ((event.localState as? Pair<*, *>?)?.first as? View)?.visibility = View.INVISIBLE
                val i = getPinnedItemIndex(event.x, event.y)
                showDropTarget(i)
            }
            DragEvent.ACTION_DRAG_LOCATION -> {
                val pair = (event.localState as? Pair<*, *>?)
                val v = pair?.first as? View
                val location = pair?.second as? IntArray
                if (v != null && location != null) {
                    val x = abs(event.x - location[0] - v.measuredWidth / 2f)
                    val y = abs(event.y - location[1] - v.measuredHeight / 2f)
                    if (x > v.measuredWidth / 3.5f || y > v.measuredHeight / 3.5f) {
                        ItemLongPress.currentPopup?.dismiss()
                    }
                }

                val i = getPinnedItemIndex(event.x, event.y)
                showDropTarget(i)
            }
            DragEvent.ACTION_DRAG_ENDED -> {
                ItemLongPress.currentPopup?.isFocusable = true
                ItemLongPress.currentPopup?.update()
                updatePinned()
                showDropTarget(-1)
            }
            DragEvent.ACTION_DRAG_EXITED -> {
                showDropTarget(-1)
            }
            DragEvent.ACTION_DROP -> {
                ((event.localState as? Pair<*, *>?)?.first as? View)?.visibility = View.VISIBLE
                ItemLongPress.currentPopup?.isFocusable = true
                ItemLongPress.currentPopup?.update()
                val i = getPinnedItemIndex(event.x, event.y)
                if (i == -1)
                    return false
                onDrop(view, i, event.clipData)
            }
        }
        return true
    }
}