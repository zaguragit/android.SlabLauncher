package io.posidon.android.slablauncher

import android.content.Context
import io.posidon.android.launcherutil.Launcher
import io.posidon.android.slablauncher.data.items.App
import io.posidon.android.slablauncher.data.items.LauncherItem
import io.posidon.android.slablauncher.providers.suggestions.SuggestionsManager
import io.posidon.android.slablauncher.util.storage.Settings

class LauncherContext {

    val settings = Settings()
    val suggestionData = Settings("stats")

    val appManager = AppManager()

    inner class AppManager {

        val pinnedItems: List<LauncherItem> get() = _pinnedItems

        var apps = emptyList<App>()
            private set

        fun <T : Context> loadApps(context: T, onEnd: T.(List<App>) -> Unit) {
            val list = ArrayList<App>()
            val byName = HashMap<String, MutableList<App>>()

            Launcher.appLoader.loadAsync(
                context,
                onEnd = {
                    list.sortWith { o1, o2 ->
                        o1.label.compareTo(o2.label, ignoreCase = true)
                    }
                    apps = list
                    appsByName = byName
                    _pinnedItems = settings.getStrings(PINNED_KEY)?.mapNotNull { tryParseLauncherItem(it, context) }?.toMutableList() ?: ArrayList()
                    SuggestionsManager.onAppsLoaded(this, context, suggestionData, appsByName.values.map(List<App>::first))
                    onEnd(context, list)
                },
                forEachApp = {
                    val app = App(
                        it.packageName,
                        it.name,
                        it.profile,
                        it.getBadgedLabel(context),
                    )
                    list.add(app)
                    putInMap(app, byName)
                },
            )
        }

        fun tryParseLauncherItem(string: String, context: Context): LauncherItem? {
            return LauncherItem.tryParse(string, appsByName, context)
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

        private var appsByName = HashMap<String, MutableList<App>>()
        private var _pinnedItems: MutableList<LauncherItem> = ArrayList()

        private inline fun putInMap(app: App, byName: HashMap<String, MutableList<App>>) {
            val list = byName[app.packageName]
            if (list == null) {
                byName[app.packageName] = arrayListOf(app)
                return
            }
            val thisAppI = list.indexOfFirst {
                it.name == app.name && it.userHandle.hashCode() == app.userHandle.hashCode()
            }
            if (thisAppI == -1) {
                list.add(app)
                return
            }
            list[thisAppI] = app
        }
    }

    companion object {
        private const val PINNED_KEY = "pinned_items"
    }
}