package io.posidon.android.slablauncher.data.items

import android.app.ActivityOptions
import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherApps
import android.os.Process
import android.os.UserHandle
import android.util.Log
import android.view.View
import io.posidon.android.slablauncher.providers.suggestions.SuggestionsManager

class App(
    val packageName: String,
    val name: String,
    val userHandle: UserHandle = Process.myUserHandle(),
    override val label: String,
) : LauncherItem {

    override fun open(context: Context, view: View?) {
        SuggestionsManager.onItemOpened(context, this)
        try {
            context.getSystemService(LauncherApps::class.java).startMainActivity(
                ComponentName(packageName, name),
                userHandle,
                view?.clipBounds,
                ActivityOptions.makeClipRevealAnimation(
                    view,
                    0,
                    0,
                    view?.measuredWidth ?: 0,
                    view?.measuredHeight ?: 0
                ).toBundle()
            )
        } catch (e: Exception) { e.printStackTrace() }
    }

    override fun toString() = "$packageName/$name/${userHandle.hashCode()}"

    fun getStaticShortcuts(launcherApps: LauncherApps): List<ShortcutItem> {
        val shortcutQuery = LauncherApps.ShortcutQuery()
        shortcutQuery.setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST)
        shortcutQuery.setPackage(packageName)
        return try {
            launcherApps.getShortcuts(shortcutQuery, Process.myUserHandle())!!.map {
                ShortcutItem(
                    (it.shortLabel ?: it.longLabel).toString(),
                    it.longLabel?.toString(),
                    this,
                    it,
                )
            }
        } catch (e: Exception) { emptyList() }
    }

    fun getDynamicShortcuts(launcherApps: LauncherApps): List<ShortcutItem> {
        val shortcutQuery = LauncherApps.ShortcutQuery()
        shortcutQuery.setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC)
        shortcutQuery.setPackage(packageName)
        return try {
            launcherApps.getShortcuts(shortcutQuery, Process.myUserHandle())!!.map {
                ShortcutItem(
                    (it.shortLabel ?: it.longLabel).toString(),
                    it.longLabel?.toString(),
                    this,
                    it,
                )
            }
        } catch (e: Exception) { emptyList() }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as App
        if (packageName != other.packageName) return false
        if (name != other.name) return false
        if (userHandle != other.userHandle) return false
        return true
    }

    override fun hashCode(): Int {
        var result = packageName.hashCode()
        result = 31 * result + name.hashCode()
        result = 31 * result + userHandle.hashCode()
        return result
    }


    companion object {
        fun tryParse(string: String, appsByName: HashMap<String, MutableList<App>>): App? =
            try { parse(string, appsByName) }
            catch (e: Exception) { null }.also {
                it ?: Log.v("LauncherItem parsing", "\"$string\" !is App")
            }

        fun parse(string: String, appsByName: HashMap<String, MutableList<App>>): App? {
            val (packageName, name, u) = string.split('/')
            val userHandle = u.toInt()
            return appsByName[packageName]?.find {
                it.name == name &&
                it.userHandle.hashCode() == userHandle
            }
        }
    }
}