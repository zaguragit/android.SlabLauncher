package io.posidon.android.slablauncher.data.search

import android.app.Activity
import android.graphics.drawable.LayerDrawable
import android.view.View
import io.posidon.android.computable.Computable
import io.posidon.android.computable.compute
import io.posidon.android.computable.syncCompute
import io.posidon.android.launcherutils.IconTheming
import io.posidon.android.slablauncher.data.items.App
import io.posidon.android.slablauncher.data.items.getBanner
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
    override val icon = Computable {
        val background = app.background.syncCompute()
        val icon = app.icon.syncCompute()
        if (background == null || icon is MaskedDrawable) {
            return@Computable icon
        }
        return@Computable MaskedDrawable(
            LayerDrawable(arrayOf(
                background.constantState?.newDrawable()?.mutate(),
                icon,
            )).apply {
                 setBounds(0, 0, icon.intrinsicWidth, icon.intrinsicHeight)
            },
            IconTheming.getSystemAdaptiveIconPath(icon.intrinsicWidth, icon.intrinsicHeight),
        )
    }

    override val subtitle = null

    override var relevance = Relevance(0f)
    override val onLongPress = { v: View, activity: Activity ->
        app.color.compute {
            val backgroundColor = ColorTheme.tileColor(it)
            activity.runOnUiThread {
                ItemLongPress.onItemLongPress(
                    v,
                    backgroundColor,
                    ColorTheme.titleColorForBG(v.context, backgroundColor),
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