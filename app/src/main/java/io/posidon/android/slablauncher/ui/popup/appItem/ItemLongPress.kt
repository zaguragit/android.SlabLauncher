package io.posidon.android.slablauncher.ui.popup.appItem

import android.content.ClipData
import android.content.ClipDescription
import android.content.Context
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.graphics.ColorUtils
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.posidon.android.slablauncher.R
import io.posidon.android.slablauncher.data.items.App
import io.posidon.android.slablauncher.data.items.LauncherItem
import io.posidon.android.slablauncher.data.items.ShortcutItem
import io.posidon.android.slablauncher.data.items.showProperties
import io.posidon.android.slablauncher.providers.color.theme.ColorTheme
import io.posidon.android.slablauncher.providers.item.GraphicsLoader
import io.posidon.android.slablauncher.ui.home.main.acrylicBlur
import io.posidon.android.slablauncher.ui.popup.PopupUtils
import io.posidon.android.slablauncher.ui.view.SeeThroughView
import io.posidon.android.slablauncher.util.drawable.setBackgroundColorFast

object ItemLongPress {

    var currentPopup: PopupWindow? = null
    fun makePopupWindow(context: Context, item: LauncherItem, backgroundColor: Int, textColor: Int, graphicsLoader: GraphicsLoader, onInfo: (View) -> Unit): PopupWindow {
        val content = LayoutInflater.from(context).inflate(R.layout.long_press_item_popup, null)
        if (item is App) {
            val launcherApps = context.getSystemService(LauncherApps::class.java)
            val shortcuts = item.getStaticShortcuts(launcherApps)
            val dynamicShortcuts = (item as? App)?.getDynamicShortcuts(launcherApps)?.let {
                it.subList(0, it.size.coerceAtMost(6 - shortcuts.size).coerceAtLeast(0))
            }

            val recyclerViewStatic = content.findViewById<RecyclerView>(R.id.recycler_static)
            val separatorStatic = content.findViewById<View>(R.id.separator_static)
            val recyclerViewDynamic = content.findViewById<RecyclerView>(R.id.recycler_dynamic)
            val separatorDynamic = content.findViewById<View>(R.id.separator_dynamic)
            if (shortcuts.isEmpty()) {
                separatorStatic.isVisible = false
                recyclerViewStatic.isVisible = false
            } else {
                separatorStatic.setBackgroundColorFast(ColorTheme.separator)
                recyclerViewStatic.isNestedScrollingEnabled = false
                recyclerViewStatic.layoutManager = LinearLayoutManager(context)
                recyclerViewStatic.adapter = ShortcutAdapter(shortcuts, textColor, graphicsLoader)
            }
            if (dynamicShortcuts.isNullOrEmpty()) {
                separatorDynamic.isVisible = false
                recyclerViewDynamic.isVisible = false
            } else {
                separatorDynamic.setBackgroundColorFast(ColorTheme.separator)
                recyclerViewDynamic.isNestedScrollingEnabled = false
                recyclerViewDynamic.layoutManager = LinearLayoutManager(context)
                recyclerViewDynamic.adapter = ShortcutAdapter(dynamicShortcuts, textColor, graphicsLoader)
            }
        }
        val window = PopupWindow(content, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true)
        window.setOnDismissListener {
            currentPopup = null
        }

        val blurBG = content.findViewById<SeeThroughView>(R.id.blur_bg)
        blurBG.drawable = acrylicBlur?.insaneBlurDrawable

        val propertiesButton = content.findViewById<View>(R.id.properties_item)
        val propertiesText = propertiesButton.findViewById<TextView>(R.id.properties_text)
        val propertiesIcon = propertiesButton.findViewById<ImageView>(R.id.properties_icon)

        propertiesText.setTextColor(textColor)
        propertiesIcon.imageTintList = ColorStateList.valueOf(ColorUtils.blendARGB(textColor, ColorTheme.accentColor, 0.5f))

        content.findViewById<CardView>(R.id.card).setCardBackgroundColor(backgroundColor)
        content.findViewById<View>(R.id.container).backgroundTintList = ColorStateList.valueOf(ColorTheme.separator)

        propertiesButton.setOnClickListener {
            window.dismiss()
            onInfo(it)
        }

        return window
    }

    private fun dismissCurrent() {
        val c = currentPopup
        currentPopup = null
        c?.dismiss()
    }

    class State(
        var view: View?,
        val location: IntArray,
    )

    fun onItemLongPress(
        view: View,
        backgroundColor: Int,
        textColor: Int,
        item: LauncherItem,
        navbarHeight: Int,
        graphicsLoader: GraphicsLoader,
    ) {
        dismissCurrent()
        val context = view.context
        val (x, y, gravity) = PopupUtils.getPopupLocationFromView(view, navbarHeight)
        val popupWindow = makePopupWindow(
            context,
            item,
            backgroundColor,
            textColor,
            graphicsLoader,
            item::showProperties,
        )
        popupWindow.showAtLocation(view, gravity, x, y + (view.resources.getDimension(R.dimen.item_card_margin) * 2).toInt())

        currentPopup = popupWindow

        val shadow = View.DragShadowBuilder(view)
        val clipData = ClipData(
            item.label,
            arrayOf(ClipDescription.MIMETYPE_TEXT_PLAIN),
            ClipData.Item(item.toString()))

        val location = IntArray(2)
        view.getLocationOnScreen(location)
        view.startDragAndDrop(clipData, shadow, State(view, location), View.DRAG_FLAG_OPAQUE or View.DRAG_FLAG_GLOBAL)
    }

    fun onItemLongPress(
        view: View,
        item: LauncherItem,
    ) {
        dismissCurrent()
        val shadow = View.DragShadowBuilder(view)
        val clipData = ClipData(
            item.label,
            arrayOf(ClipDescription.MIMETYPE_TEXT_PLAIN),
            ClipData.Item(item.toString()))

        val location = IntArray(2)
        view.getLocationOnScreen(location)
        view.startDragAndDrop(clipData, shadow, State(view, location), View.DRAG_FLAG_OPAQUE or View.DRAG_FLAG_GLOBAL)
    }
}