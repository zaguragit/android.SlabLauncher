package io.posidon.android.slablauncher.ui.intro

import android.Manifest
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.view.isVisible
import io.posidon.android.slablauncher.R
import io.posidon.android.slablauncher.providers.color.pallete.ColorPalette
import io.posidon.android.slablauncher.providers.color.theme.ColorTheme
import io.posidon.android.slablauncher.providers.suggestions.SuggestionsManager
import io.posidon.android.slablauncher.ui.home.MainActivity
import io.posidon.android.slablauncher.util.FakeLauncherActivity

class PermissionsFragment : FragmentWithNext(R.layout.intro_permissions) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        updatePermissionStatus(view)
    }

    fun updatePermissionStatus() = updatePermissionStatus(requireView())

    fun updateColorTheme() {
        val v = requireView()
        arrayOf(
            R.id.title,
            R.id.storage_title,
            R.id.contacts_title,
            R.id.notifications_title,
            R.id.usage_title
        ).forEach { v.findViewById<TextView>(it).setTextColor(ColorTheme.uiTitle) }
        arrayOf(
            R.id.storage_description,
            R.id.contacts_description,
            R.id.notifications_description,
            R.id.usage_description
        ).forEach { v.findViewById<TextView>(it).setTextColor(ColorTheme.uiDescription) }
    }

    private fun updatePermissionStatus(v: View) = v.apply {
        val tickStorage = findViewById<ImageView>(R.id.tick_storage)!!
        val tickContacts = findViewById<ImageView>(R.id.tick_contacts)!!
        val tickNotifications = findViewById<ImageView>(R.id.tick_notifications)!!
        val tickUsageAccess = findViewById<ImageView>(R.id.tick_usage_access)!!
        if (
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            findViewById<View>(R.id.button_storage)!!.isVisible = false
            tickStorage.isVisible = true
            ColorPalette.loadWallColorTheme(requireActivity() as IntroActivity) { a, p ->
                a.updateColorTheme(p)
                updateColorTheme()
                val tl = ColorStateList.valueOf(ColorTheme.accentColor)
                tickStorage.imageTintList = tl
                tickContacts.imageTintList = tl
                tickNotifications.imageTintList = tl
                tickUsageAccess.imageTintList = tl
            }
        } else {
            findViewById<View>(R.id.button_storage)!!
                .setOnClickListener(::requestStoragePermission)
            updateColorTheme()
        }

        if (
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_CONTACTS
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            findViewById<View>(R.id.button_contacts)!!.isVisible = false
            tickContacts.isVisible = true
        } else {
            findViewById<View>(R.id.button_contacts)!!
                .setOnClickListener(::requestContactsPermission)
        }

        if (
            NotificationManagerCompat.getEnabledListenerPackages(context)
                .contains(context.packageName)
        ) {
            findViewById<View>(R.id.button_notifications)!!.isVisible = false
            tickNotifications.isVisible = true
        } else {
            findViewById<View>(R.id.button_notifications)!!
                .setOnClickListener(::requestNotificationsPermission)
        }

        if (SuggestionsManager.checkUsageAccessPermission(context)) {
            findViewById<View>(R.id.button_usage_access)!!.isVisible = false
            tickUsageAccess.isVisible = true
        } else {
            findViewById<View>(R.id.button_usage_access)!!
                .setOnClickListener(::requestUsageAccessPermission)
        }
    }

    override fun next(activity: IntroActivity) {
        //activity.setFragment(QuickSettingsFragment())
        val home = ComponentName(requireContext(), MainActivity::class.java)
        requireContext().packageManager.setComponentEnabledSetting(home, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
        val intro = ComponentName(requireContext(), IntroActivity::class.java.name + "Alias")
        requireContext().packageManager.setComponentEnabledSetting(intro, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
        startActivity(Intent(requireContext(), MainActivity::class.java))
        chooseLauncher()
    }

    private fun chooseLauncher() {
        val componentName = ComponentName(requireContext(), FakeLauncherActivity::class.java)
        requireContext().packageManager.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)
        val selector = Intent(Intent.ACTION_MAIN)
        selector.addCategory(Intent.CATEGORY_HOME)
        selector.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        startActivity(selector)
        requireContext().packageManager.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_DEFAULT, PackageManager.DONT_KILL_APP)
    }

    private fun requestStoragePermission(v: View) {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requireActivity().requestPermissions(
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ), 0
            )
        }
    }

    private fun requestContactsPermission(v: View) {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.READ_CONTACTS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            requireActivity().requestPermissions(
                arrayOf(
                    Manifest.permission.READ_CONTACTS
                ), 0
            )
        }
    }

    private fun requestNotificationsPermission(v: View) {
        if (!NotificationManagerCompat.getEnabledListenerPackages(v.context).contains(v.context.packageName)) {
            v.context.startActivity(
                Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS").addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
                )
            )
        }
    }

    private fun requestUsageAccessPermission(v: View) {
        v.context.startActivity(
            Intent(android.provider.Settings.ACTION_USAGE_ACCESS_SETTINGS).addFlags(
                Intent.FLAG_ACTIVITY_NEW_TASK
            )
        )
    }
}