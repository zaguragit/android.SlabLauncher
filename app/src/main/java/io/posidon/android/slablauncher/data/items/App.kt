package io.posidon.android.slablauncher.data.items

import android.app.ActivityOptions
import android.content.ComponentName
import android.content.Context
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.content.pm.ShortcutInfo
import android.graphics.drawable.Drawable
import android.os.Process
import android.os.UserHandle
import android.view.View
import io.posidon.android.slablauncher.providers.notification.NotificationService
import io.posidon.android.slablauncher.providers.notification.NotificationSorter
import posidon.android.conveniencelib.isInstalled
import java.util.*

class App(
    val packageName: String,
    val name: String,
    val userHandle: UserHandle = Process.myUserHandle(),
    override val label: String,
    override val icon: Drawable,
    val background: Drawable?,
    private val _color: Int,
) : LauncherItem {

    inline fun getBanner(): Banner? {
        val notifications = NotificationService.notifications.filter { it.sourcePackageName == packageName }
        val mediaItem = NotificationService.mediaItem
        if (background == null && notifications.isEmpty() && mediaItem == null) return null
        if (mediaItem != null && mediaItem.sourcePackageName == packageName) return Banner(
            mediaItem.title,
            mediaItem.subtitle,
            mediaItem.image,
            .4f
        )
        val notification = NotificationSorter.getMostRelevant(notifications)
        val image = notification?.image
        if (image != null) return Banner(
            notification.title.takeIf { label != it },
            notification.description,
            image,
            .4f
        )

        return Banner(
            notification?.title.takeIf { label != it },
            notification?.description,
            background,
            1f
        )
    }

    class Banner(
        val title: String?,
        val text: String?,
        val background: Drawable?,
        val bgOpacity: Float,
    )

    override fun open(context: Context, view: View?) {
        try {
            context.getSystemService(LauncherApps::class.java).startMainActivity(ComponentName(packageName, name), userHandle, view?.clipBounds,
                ActivityOptions.makeScaleUpAnimation(view, 0, 0, view?.measuredWidth ?: 0, view?.measuredHeight ?: 0).toBundle())
        } catch (e: Exception) { e.printStackTrace() }
    }

    override fun getColor(): Int = _color

    override fun toString() = "$packageName/$name/${userHandle.hashCode()}"

    fun getStaticShortcuts(launcherApps: LauncherApps): List<ShortcutInfo> {
        val shortcutQuery = LauncherApps.ShortcutQuery()
        shortcutQuery.setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST)
        shortcutQuery.setPackage(packageName)
        return try {
            launcherApps.getShortcuts(shortcutQuery, Process.myUserHandle())!!
        } catch (e: Exception) { emptyList() }
    }

    fun getDynamicShortcuts(launcherApps: LauncherApps): List<ShortcutInfo> {
        val shortcutQuery = LauncherApps.ShortcutQuery()
        shortcutQuery.setQueryFlags(LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC)
        shortcutQuery.setPackage(packageName)
        return try {
            launcherApps.getShortcuts(shortcutQuery, Process.myUserHandle())!!
        } catch (e: Exception) { emptyList() }
    }

    inline fun isInstalled(packageManager: PackageManager) = packageManager.isInstalled(packageName)

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