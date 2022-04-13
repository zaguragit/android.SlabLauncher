package io.posidon.android.slablauncher.data.items

import android.app.ActivityOptions
import android.content.Context
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.view.View
import io.posidon.android.slablauncher.providers.suggestions.SuggestionsManager

class ShortcutItem(
    override val label: String,
    val longLabel: String?,
    val app: App,
    val shortcutInfo: ShortcutInfo,
) : LauncherItem {

    override fun open(context: Context, view: View?) {
        SuggestionsManager.onItemOpened(context, this)
        try {
            val launcherApps = context.getSystemService(LauncherApps::class.java)
            launcherApps.startShortcut(
                shortcutInfo,
                view?.clipBounds,
                ActivityOptions.makeClipRevealAnimation(
                    view,
                    0,
                    0,
                    view?.measuredWidth ?: 0,
                    view?.measuredHeight ?: 0
                ).toBundle()
            )
        } catch (e: Exception) { e.printStackTrace() }
    }

    override fun toString() = "shortcut:$app:TODO"
}