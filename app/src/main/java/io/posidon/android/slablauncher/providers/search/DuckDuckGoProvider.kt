package io.posidon.android.slablauncher.providers.search

import io.posidon.android.slablauncher.BuildConfig
import io.posidon.android.slablauncher.data.search.InstantAnswerResult
import posidon.android.loader.duckduckgo.DuckInstantAnswer

class DuckDuckGoProvider(searcher: Searcher) : AsyncSearchProvider(searcher) {

    override fun loadResults(query: SearchQuery) {
        if (query.length >= 3) {
            DuckInstantAnswer.load(query.toString(), BuildConfig.APPLICATION_ID) {
                update(query, listOf(
                    InstantAnswerResult(
                        query,
                        it.title,
                        it.description,
                        it.sourceName,
                        it.sourceUrl,
                        it.searchUrl,
                        it.infoTable?.filter { a -> a.dataType == "string" }?.map { a -> a.label + ':' to a.value }?.takeIf(List<*>::isNotEmpty)
                    )
                ))
            }
        }
    }
}