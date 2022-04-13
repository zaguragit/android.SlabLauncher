package io.posidon.android.slablauncher.data.search

import android.view.View
import io.posidon.android.slablauncher.data.items.ShortcutItem

class ShortcutResult(
    val shortcut: ShortcutItem,
) : CompactResult() {

    override val launcherItem get() = shortcut

    override val title get() = shortcut.longLabel ?: shortcut.label
    override val subtitle get() = shortcut.app.label
    override var relevance = Relevance(0f)
    override val onLongPress = null

    override fun open(view: View) = shortcut.open(view.context, view)
}