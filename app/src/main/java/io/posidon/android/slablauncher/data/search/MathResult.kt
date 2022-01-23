package io.posidon.android.slablauncher.data.search

import android.view.View
import io.posidon.android.slablauncher.providers.search.SearchQuery

class MathResult(
    query: SearchQuery,
    val operation: String,
    val result: String,
) : SearchResult {
    override var relevance = Relevance(2.0f)

    override val title = operation

    override fun open(view: View) {
    }
}