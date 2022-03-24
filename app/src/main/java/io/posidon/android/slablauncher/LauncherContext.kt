package io.posidon.android.slablauncher

import android.content.Context
import io.posidon.android.launcherutils.appLoading.AppLoader
import io.posidon.android.launcherutils.appLoading.IconConfig
import io.posidon.android.slablauncher.data.items.App
import io.posidon.android.slablauncher.data.items.LauncherItem
import io.posidon.android.slablauncher.providers.app.AppCollection
import io.posidon.android.slablauncher.providers.suggestions.SuggestionsManager
import io.posidon.android.slablauncher.util.storage.Settings
import posidon.android.conveniencelib.dp

class LauncherContext {

    val settings = Settings()
    val suggestionData = Settings("stats")

    val appManager = AppManager()

    inner class AppManager {

        val pinnedItems: List<LauncherItem> get() = _pinnedItems

        var apps = emptyList<App>()
            private set

        fun <T : Context> loadApps(context: T, onEnd: T.(apps: AppCollection) -> Unit) {
            val iconConfig = IconConfig(
                size = context.dp(108).toInt(),
                density = context.resources.configuration.densityDpi,
                packPackages = settings.getStrings("icon_packs") ?: emptyArray(),
            )

            appLoader.async(context, iconConfig) {
                apps = it.list
                appsByName = it.byName
                _pinnedItems = settings.getStrings(PINNED_KEY)?.mapNotNull { LauncherItem.tryParse(it, appsByName, context) }?.toMutableList() ?: ArrayList()
                SuggestionsManager.onAppsLoaded(this, context, suggestionData, appsByName.values.map(List<App>::first))
                onEnd(context, it)
            }
        }

        fun tryParseLauncherItem(string: String, context: Context): LauncherItem? {
            return LauncherItem.tryParse(string, appsByName, context)
        }

        fun tryParseApp(string: String): App? {
            return App.tryParse(string, appsByName)
        }

        fun getAppByPackage(packageName: String): LauncherItem? = appsByName[packageName]?.first()

        fun pinItem(context: Context, launcherItem: LauncherItem, i: Int) {
            _pinnedItems.add(i, launcherItem)
            settings.edit(context) {
                val s = launcherItem.toString()
                PINNED_KEY set (settings.getStrings(PINNED_KEY)
                    ?.toMutableList()
                    ?.apply { add(i, s) }
                    ?.toTypedArray()
                    ?: arrayOf(s))
            }
        }

        fun unpinItem(context: Context, i: Int) {
            _pinnedItems.removeAt(i)
            settings.edit(context) {
                PINNED_KEY set (settings.getStrings(PINNED_KEY)
                    ?.toMutableList()
                    ?.apply { removeAt(i) }
                    ?.toTypedArray()
                    ?: throw IllegalStateException("Can't unpin an item when no items are pinned"))
            }
        }

        fun setPinned(context: Context, pinned: List<LauncherItem>) {
            _pinnedItems = pinned.toMutableList()
            settings.edit(context) {
                PINNED_KEY set pinned.map(LauncherItem::toString).toTypedArray()
            }
        }

        private val appLoader = AppLoader { AppCollection(it, settings) }

        private var appsByName = HashMap<String, MutableList<App>>()

        private var _pinnedItems: MutableList<LauncherItem> = ArrayList()
    }

    companion object {
        private const val PINNED_KEY = "pinned_items"
    }
}