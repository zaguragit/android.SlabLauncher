package io.posidon.android.slablauncher.data.search

import android.content.Intent
import android.net.Uri
import android.view.View
import com.willowtreeapps.fuzzywuzzy.diffutils.FuzzySearch
import io.posidon.android.slablauncher.providers.search.SearchQuery
import kotlin.math.max

class InstantAnswerResult(
    query: SearchQuery,
    override val title: String,
    val description: String,
    val sourceName: String,
    private val sourceUrl: String,
    private val searchUrl: String,
    val infoTable: List<Pair<String, String>>?
) : SearchResult {
    override var relevance = Relevance(max(
        FuzzySearch.tokenSortPartialRatio(query.toString(), title),
        FuzzySearch.tokenSortPartialRatio(query.toString(), description)
    ) / 100f)

    override fun open(view: View) {
        view.context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(sourceUrl)))
    }

    fun search(view: View) {
        view.context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(searchUrl)))
    }
}