package io.posidon.android.slablauncher.providers.search

import android.app.Activity
import android.content.Context
import io.posidon.android.slablauncher.data.search.SearchResult
import io.posidon.android.slablauncher.providers.app.AppCollection

interface SearchProvider {

    fun Activity.onCreate() {}
    fun getResults(query: SearchQuery): List<SearchResult>

    fun onAppsLoaded(context: Context, apps: AppCollection) {}

    companion object {
        fun matchInitials(query: String, string: String): Boolean {
            val initials = string.split(Regex("([ .\\-_]|([a-z](?=[A-Z0-9])))")).mapNotNull(String::firstOrNull).joinToString("")
            val initialsBasic = string.split(Regex("[ .\\-_]")).mapNotNull(String::firstOrNull).joinToString("")
            return initials.startsWith(query, ignoreCase = true) || initialsBasic.startsWith(query, ignoreCase = true)
        }
    }
}