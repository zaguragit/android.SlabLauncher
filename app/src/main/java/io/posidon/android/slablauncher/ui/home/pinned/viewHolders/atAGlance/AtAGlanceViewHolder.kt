package io.posidon.android.slablauncher.ui.home.pinned.viewHolders.atAGlance

import android.annotation.SuppressLint
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import io.posidon.android.slablauncher.R
import io.posidon.android.slablauncher.providers.color.theme.ColorTheme
import io.posidon.android.slablauncher.ui.home.MainActivity
import io.posidon.android.slablauncher.ui.home.pinned.TileArea.Companion.COLUMNS
import io.posidon.android.slablauncher.ui.home.pinned.TileArea.Companion.DOCK_ROWS
import io.posidon.android.slablauncher.ui.home.pinned.TileArea.Companion.WIDTH_TO_HEIGHT
import io.posidon.android.slablauncher.ui.popup.home.HomeLongPressPopup
import posidon.android.conveniencelib.Device
import posidon.android.conveniencelib.getNavigationBarHeight
import posidon.android.conveniencelib.getStatusBarHeight

@SuppressLint("ClickableViewAccessibility")
class AtAGlanceViewHolder(
    itemView: View,
    mainActivity: MainActivity,
) : RecyclerView.ViewHolder(itemView) {

    val date = itemView.findViewById<TextView>(R.id.date)!!

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
            HomeLongPressPopup.show(
                it,
                popupX,
                popupY,
                mainActivity.getNavigationBarHeight(),
                mainActivity.settings,
                mainActivity::reloadColorPaletteSync,
                mainActivity::updateColorTheme,
                mainActivity::loadApps,
                mainActivity::reloadBlur,
            )
            true
        }
        itemView.updateLayoutParams {
            val tileMargin = itemView.context.resources.getDimension(R.dimen.item_card_margin)
            val tileWidth = (Device.screenWidth(itemView.context) - tileMargin * 2) / COLUMNS - tileMargin * 2
            val tileHeight = tileWidth / WIDTH_TO_HEIGHT
            val dockHeight = DOCK_ROWS * (tileHeight + tileMargin * 2)
            height = Device.screenHeight(itemView.context) - itemView.context.getStatusBarHeight() - (tileMargin * 2 + dockHeight.toInt()).toInt() + 1
        }
    }

    fun onBind() {
        date.setTextColor(ColorTheme.uiTitle)
    }
}