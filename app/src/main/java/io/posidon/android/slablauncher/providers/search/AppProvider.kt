package io.posidon.android.slablauncher.providers.search

import android.app.Activity
import android.content.Context
import android.content.pm.LauncherApps
import com.willowtreeapps.fuzzywuzzy.diffutils.FuzzySearch
import io.posidon.android.slablauncher.data.items.App
import io.posidon.android.slablauncher.data.search.AppResult
import io.posidon.android.slablauncher.data.search.Relevance
import io.posidon.android.slablauncher.data.search.SearchResult
import io.posidon.android.slablauncher.data.search.ShortcutResult
import io.posidon.android.slablauncher.providers.suggestions.SuggestionsManager
import java.util.*
import kotlin.concurrent.thread
import kotlin.math.pow

class AppProvider(
    private val searcher: Searcher
) : SearchProvider {

    private var appList = emptyList<App>()
    private var staticShortcuts = emptyList<ShortcutResult>()
    private var dynamicShortcuts = emptyList<ShortcutResult>()

    private lateinit var launcherApps: LauncherApps

    override fun Activity.onCreate() {
        launcherApps = getSystemService(LauncherApps::class.java)
        updateAppCache(searcher.launcherContext.appManager.apps)
    }

    private fun updateAppCache(list: List<App>) {
        appList = list
        thread(isDaemon = true) {
            staticShortcuts = appList.flatMap { app ->
                app.getStaticShortcuts(launcherApps).map(::ShortcutResult)
            }
            dynamicShortcuts = appList.flatMap { app ->
                app.getDynamicShortcuts(launcherApps).map(::ShortcutResult)
            }
        }
    }

    override fun onAppsLoaded(context: Context, list: List<App>) {
        updateAppCache(list)
    }

    override fun getResults(query: SearchQuery): List<SearchResult> {
        val results = LinkedList<SearchResult>()
        val suggestions = SuggestionsManager.get().let { it.subList(0, it.size.coerceAtMost(6)) }
        val queryString = query.toString()
        appList.forEach {
            val i = suggestions.indexOf(it)
            val suggestionFactor = if(i == -1) 0f else (suggestions.size - i).toFloat() / suggestions.size
            val packageFactor = run {
                val r = FuzzySearch.tokenSortPartialRatio(queryString, it.packageName) / 100f
                r * r * r * 0.8f
            } * 0.5f
            val initialsFactor = if (queryString.length > 1 && SearchProvider.matchInitials(queryString, it.label)) 0.6f else 0f
            val r = FuzzySearch.tokenSortPartialRatio(queryString, it.label) / 100f +
                suggestionFactor +
                initialsFactor +
                packageFactor
            if (r > .8f) {
                results += AppResult(it).apply {
                    relevance = Relevance(r.coerceAtLeast(0.98f))
                }
            }
        }
        staticShortcuts.forEach {
            val l = FuzzySearch.tokenSortPartialRatio(queryString, it.title) / 100f
            val a = FuzzySearch.tokenSortPartialRatio(queryString, it.shortcut.app.label) / 100f
            val initials = if (queryString.length > 1 && SearchProvider.matchInitials(queryString, it.title)) 0.5f else 0f
            val r = (a * a * .5f + l * l).pow(.2f) + initials
            if (r > .95f) {
                it.relevance = Relevance(l)
                results += it
            }
        }
        dynamicShortcuts.forEach {
            val l = FuzzySearch.tokenSortPartialRatio(queryString, it.title) / 100f
            val a = FuzzySearch.tokenSortPartialRatio(queryString, it.shortcut.app.label) / 100f
            val initials = if (queryString.length > 1 && SearchProvider.matchInitials(queryString, it.title)) 0.5f else 0f
            val r = (a * a * .2f + l * l).pow(.3f) + initials
            if (r > .9f) {
                it.relevance = Relevance(if (l >= .95) r.coerceAtLeast(0.98f) else r.coerceAtMost(0.9f))
                results += it
            }
        }
        return results
    }
}