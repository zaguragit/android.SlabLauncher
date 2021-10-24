package io.posidon.android.slablauncher.providers.search

import android.app.Activity
import android.content.Context
import io.posidon.android.slablauncher.data.search.SearchResult
import io.posidon.android.slablauncher.providers.app.AppCollection

interface SearchProvider {

    fun Activity.onCreate() {}
    fun getResults(query: SearchQuery): List<SearchResult>

    fun onAppsLoaded(context: Context, apps: AppCollection) {}
}