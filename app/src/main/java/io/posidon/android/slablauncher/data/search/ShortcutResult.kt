package io.posidon.android.slablauncher.data.search

import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.graphics.drawable.Drawable
import android.view.View
import io.posidon.android.computable.Computable
import io.posidon.android.slablauncher.data.items.App

class ShortcutResult(
    val shortcutInfo: ShortcutInfo,
    override val title: String,
    override val icon: Computable<Drawable>,
    val app: App
) : CompactResult() {

    override val subtitle get() = app.label
    override var relevance = Relevance(0f)
    override val onLongPress = null

    override fun open(view: View) {
        try {
            val launcherApps = view.context.getSystemService(LauncherApps::class.java)
            launcherApps.startShortcut(shortcutInfo, null, null)
        } catch (e: Exception) { e.printStackTrace() }
    }
}