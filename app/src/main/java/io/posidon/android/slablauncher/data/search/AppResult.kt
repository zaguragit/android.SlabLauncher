package io.posidon.android.slablauncher.data.search

import android.app.Activity
import android.graphics.drawable.Drawable
import android.graphics.drawable.LayerDrawable
import android.view.View
import io.posidon.android.launcherutils.IconTheming
import io.posidon.android.slablauncher.data.items.App
import io.posidon.android.slablauncher.providers.color.theme.ColorTheme
import io.posidon.android.slablauncher.ui.popup.appItem.ItemLongPress
import posidon.android.conveniencelib.drawable.MaskedDrawable
import posidon.android.conveniencelib.getNavigationBarHeight

class AppResult(
    val app: App
) : CompactResult() {

    inline val packageName: String get() = app.packageName
    inline val name: String get() = app.name
    override val title: String get() = app.label
    override val icon: Drawable get() {
        if (app.background == null || app.icon is MaskedDrawable) {
            return app.icon
        }
        return MaskedDrawable(
            LayerDrawable(arrayOf(
                app.background,
                app.icon,
            )).apply {
                 setBounds(0, 0, app.icon.intrinsicWidth, app.icon.intrinsicHeight)
            },
            IconTheming.getSystemAdaptiveIconPath(app.icon.intrinsicWidth, app.icon.intrinsicHeight),
        )
    }

    override val subtitle get() = null

    override var relevance = Relevance(0f)
    override val onLongPress = { v: View, activity: Activity ->
        val backgroundColor = ColorTheme.tintAppDrawerItem(getColor())
        ItemLongPress.onItemLongPress(
            v,
            backgroundColor,
            ColorTheme.titleColorForBG(v.context, backgroundColor),
            app,
            activity.getNavigationBarHeight(),
        )
        true
    }

    inline fun getColor(): Int = app.getColor()

    override fun open(view: View) {
        app.open(view.context, view)
    }
}