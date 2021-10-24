package io.posidon.android.slablauncher.providers.search

import android.app.Activity
import android.content.Context
import android.content.pm.LauncherApps
import android.content.res.Resources
import android.graphics.drawable.ColorDrawable
import com.willowtreeapps.fuzzywuzzy.diffutils.FuzzySearch
import io.posidon.android.slablauncher.data.items.App
import io.posidon.android.slablauncher.data.search.AppResult
import io.posidon.android.slablauncher.data.search.Relevance
import io.posidon.android.slablauncher.data.search.SearchResult
import io.posidon.android.slablauncher.data.search.ShortcutResult
import io.posidon.android.slablauncher.providers.app.AppCollection
import io.posidon.android.slablauncher.providers.suggestions.SuggestionsManager
import java.util.*
import kotlin.concurrent.thread
import kotlin.math.pow

class AppProvider(
    val searcher: Searcher
) : SearchProvider {

    var appList = emptyList<App>()
    var staticShortcuts = emptyList<ShortcutResult>()
    var dynamicShortcuts = emptyList<ShortcutResult>()

    private lateinit var launcherApps: LauncherApps

    override fun Activity.onCreate() {
        launcherApps = getSystemService(LauncherApps::class.java)
        updateAppCashe(resources, searcher.launcherContext.appManager.apps)
    }

    private fun updateAppCashe(resources: Resources, list: List<App>) {
        appList = list
        thread (isDaemon = true) {
            staticShortcuts = appList.flatMap { app ->
                app.getStaticShortcuts(launcherApps).map {
                    ShortcutResult(
                        it,
                        (it.longLabel ?: it.shortLabel).toString(),
                        launcherApps.getShortcutIconDrawable(
                            it,
                            resources.displayMetrics.densityDpi
                        ) ?: ColorDrawable(),
                        app
                    )
                }
            }
            dynamicShortcuts = appList.flatMap { app ->
                app.getDynamicShortcuts(launcherApps).map {
                    ShortcutResult(
                        it,
                        (it.longLabel ?: it.shortLabel).toString(),
                        launcherApps.getShortcutIconDrawable(
                            it,
                            resources.displayMetrics.densityDpi
                        ) ?: ColorDrawable(),
                        app
                    )
                }
            }
        }
    }

    override fun onAppsLoaded(context: Context, apps: AppCollection) {
        updateAppCashe(context.resources, apps.list)
    }

    override fun getResults(query: SearchQuery): List<SearchResult> {
        val results = LinkedList<SearchResult>()
        val suggestions = SuggestionsManager.getSuggestions().let { it.subList(0, it.size.coerceAtMost(6)) }
        appList.forEach {
            val i = suggestions.indexOf(it)
            val suggestionFactor = if(i == -1) 0f else (suggestions.size - i).toFloat() / suggestions.size
            val r = FuzzySearch.tokenSortPartialRatio(query.toString(), it.label) / 100f + suggestionFactor * 0.5f
            if (r > .8f) {
                results += AppResult(it).apply {
                    relevance = Relevance(r.coerceAtLeast(0.98f))
                }
            }
        }
        staticShortcuts.forEach {
            val l = FuzzySearch.tokenSortPartialRatio(query.toString(), it.title) / 100f
            val a = FuzzySearch.tokenSortPartialRatio(query.toString(), it.app.label) / 100f
            val r = (a * a * .5f + l * l).pow(.2f)
            if (r > .95f) {
                it.relevance = Relevance(l)
                results += it
            }
        }
        dynamicShortcuts.forEach {
            val l = FuzzySearch.tokenSortPartialRatio(query.toString(), it.title) / 100f
            val a = FuzzySearch.tokenSortPartialRatio(query.toString(), it.app.label) / 100f
            val r = (a * a * .2f + l * l).pow(.3f)
            if (r > .9f) {
                it.relevance = Relevance(if (l >= .95) r.coerceAtLeast(0.98f) else r.coerceAtMost(0.9f))
                results += it
            }
        }
        return results
    }
}