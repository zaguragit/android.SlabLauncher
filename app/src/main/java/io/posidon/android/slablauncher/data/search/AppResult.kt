package io.posidon.android.slablauncher.data.search

import android.app.Activity
import android.view.View
import io.posidon.android.computable.Computable
import io.posidon.android.computable.compute
import io.posidon.android.slablauncher.data.items.App
import io.posidon.android.slablauncher.providers.color.theme.ColorTheme
import io.posidon.android.slablauncher.ui.popup.appItem.ItemLongPress
import io.posidon.android.conveniencelib.getNavigationBarHeight

class AppResult(
    val app: App
) : CompactResult() {

    inline val packageName: String get() = app.packageName
    inline val name: String get() = app.name
    override val title: String get() = app.label
    override val icon = app.icon

    override val subtitle = null

    override var relevance = Relevance(0f)
    override val onLongPress = { v: View, activity: Activity ->
        app.color.compute {
            val backgroundColor = ColorTheme.tintPopup(it)
            activity.runOnUiThread {
                ItemLongPress.onItemLongPress(
                    v,
                    backgroundColor,
                    ColorTheme.titleColorForBG(backgroundColor),
                    app,
                    activity.getNavigationBarHeight(),
                )
            }
        }
        true
    }

    override fun open(view: View) {
        app.open(view.context, view)
    }
}