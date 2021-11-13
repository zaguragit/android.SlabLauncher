package io.posidon.android.slablauncher.providers.app

import android.content.pm.LauncherApps
import android.os.UserHandle

class AppCallback(
    val callback: () -> Unit,
    val onPackageProgressChanged: (String, UserHandle, Float) -> Unit
) : LauncherApps.Callback() {

    override fun onPackageRemoved(packageName: String?, user: UserHandle?) = callback()

    override fun onPackageAdded(packageName: String?, user: UserHandle?) = callback()

    override fun onPackageChanged(packageName: String?, user: UserHandle?) = callback()

    override fun onPackagesAvailable(
        packageNames: Array<out String>?,
        user: UserHandle?,
        replacing: Boolean
    ) = callback()

    override fun onPackagesUnavailable(
        packageNames: Array<out String>?,
        user: UserHandle?,
        replacing: Boolean
    ) = callback()

    override fun onPackagesSuspended(packageNames: Array<out String>?, user: UserHandle?) = callback()

    override fun onPackagesUnsuspended(packageNames: Array<out String>?, user: UserHandle?) = callback()

    override fun onPackageLoadingProgressChanged(
        packageName: String,
        user: UserHandle,
        progress: Float
    ) = onPackageProgressChanged(packageName, user, progress)
}
