package io.posidon.android.slablauncher.providers.search

import io.posidon.android.slablauncher.data.search.SearchResult

abstract class AsyncSearchProvider(
    val searcher: Searcher
) : SearchProvider {

    val lastResults = HashMap<SearchQuery, List<SearchResult>>()
    private var lastQuery: SearchQuery? = null

    override fun getResults(query: SearchQuery): List<SearchResult> {
        return lastResults.getOrElse(query) {
            lastQuery = query
            loadResults(query)
            emptyList()
        }
    }

    abstract fun loadResults(query: SearchQuery)

    fun update(query: SearchQuery, results: List<SearchResult>) {
        lastResults[query] = results
        if (lastQuery == query)
            searcher.query(query)
    }
}