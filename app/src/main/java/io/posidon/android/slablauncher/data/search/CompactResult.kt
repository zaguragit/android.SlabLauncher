package io.posidon.android.slablauncher.data.search

import android.app.Activity
import android.view.View
import io.posidon.android.slablauncher.data.items.LauncherItem
import io.posidon.android.slablauncher.providers.item.GraphicsLoader

abstract class CompactResult : SearchResult {
    abstract val launcherItem: LauncherItem
    abstract val subtitle: String?
    override var relevance = Relevance(0f)
    abstract val onLongPress: ((GraphicsLoader, View, Activity) -> Boolean)?
}