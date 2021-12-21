package io.posidon.android.slablauncher.data.search

import android.app.Activity
import android.graphics.drawable.Drawable
import android.view.View
import io.posidon.android.computable.Computable

abstract class CompactResult : SearchResult {

    abstract val icon: Computable<Drawable>
    abstract val subtitle: String?

    override var relevance = Relevance(0f)
    
    abstract val onLongPress: ((View, Activity) -> Boolean)?
}