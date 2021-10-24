package io.posidon.android.slablauncher.providers.search

import android.app.Activity
import android.content.Context
import io.posidon.android.slablauncher.LauncherContext
import io.posidon.android.slablauncher.data.search.SearchResult
import io.posidon.android.slablauncher.providers.app.AppCollection
import java.util.*

class Searcher(
    val launcherContext: LauncherContext,
    vararg providers: (Searcher) -> SearchProvider,
    val update: (SearchQuery, List<SearchResult>) -> Unit
) {
    val settings by launcherContext::settings

    val providers = providers.map { it(this) }

    private fun query(query: SearchQuery): List<SearchResult> {
        val r = LinkedList<SearchResult>()
        providers.flatMapTo(r) { it.getResults(query) }
        r.sortWith { a, b ->
            b.relevance.compareTo(a.relevance)
        }
        return r
    }

    fun query(query: CharSequence?) {
        val q = SearchQuery(query ?: "")
        update(q, if (query == null) emptyList()
        else query(q))
    }

    fun onCreate(activity: Activity) {
        providers.forEach {
            it.run { activity.onCreate() }
        }
    }

    fun onAppsLoaded(context: Context, apps: AppCollection) {
        providers.forEach {
            it.onAppsLoaded(context, apps)
        }
    }
}