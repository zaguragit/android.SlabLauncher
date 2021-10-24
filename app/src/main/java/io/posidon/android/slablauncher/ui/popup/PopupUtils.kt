package io.posidon.android.slablauncher.ui.popup

import android.content.Context
import android.view.Gravity
import android.view.View
import android.widget.PopupWindow
import posidon.android.conveniencelib.Device

object PopupUtils {

    private var currentPopup: PopupWindow? = null

    fun dismissCurrent() = currentPopup?.dismiss()
    fun setCurrent(popup: PopupWindow) {
        dismissCurrent()
        popup.setOnDismissListener {
            currentPopup = null
        }
        currentPopup = popup
    }

    /**
     * @return Triple(x, y, gravity)
     */
    inline fun getPopupLocationFromView(
        view: View,
        navbarHeight: Int,
    ): Triple<Int, Int, Int> {

        val location = IntArray(2).also {
            view.getLocationOnScreen(it)
        }

        return getPopupLocation(view.context, location[0], location[1], view.measuredWidth, view.measuredHeight, navbarHeight, 0, 0)
    }

    /**
     * @return Triple(x, y, gravity)
     */
    inline fun getPopupLocation(
        context: Context,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        navbarHeight: Int,
        offsetX: Int,
        offsetY: Int,
    ): Triple<Int, Int, Int> {

        var gravity: Int

        val screenWidth = Device.screenWidth(context)
        val screenHeight = Device.screenHeight(context)

        val x = if (x > screenWidth / 2) {
            gravity = Gravity.END
            screenWidth - x - width
        } else {
            gravity = Gravity.START
            x
        } + offsetX

        val y = if (y < screenHeight / 2) {
            gravity = gravity or Gravity.TOP
            y + height
        } else {
            gravity = gravity or Gravity.BOTTOM
            screenHeight - y + navbarHeight
        } + offsetY

        return Triple(x, y, gravity)
    }
}