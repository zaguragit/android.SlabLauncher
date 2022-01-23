package io.posidon.android.slablauncher.providers.search

import io.posidon.android.slablauncher.data.search.MathResult
import io.posidon.android.slablauncher.data.search.SearchResult
import io.posidon.android.slablauncher.providers.search.parsing.Parser

class MathProvider(
    searcher: Searcher
) : SearchProvider {

    override fun getResults(query: SearchQuery): List<SearchResult> {
        return try {
            val (result, operation) = Parser(query.text.toString()).parseOperation()
            listOf(MathResult(
                query,
                operation,
                result.toString().let { if (it.endsWith(".0")) it.substring(0, it.length - 2) else it },
            ))
        } catch (e: Exception) { emptyList() }
    }
}