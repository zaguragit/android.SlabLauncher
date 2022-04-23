package io.posidon.android.slablauncher.ui.intro

import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.TextView
import io.posidon.android.slablauncher.R
import io.posidon.android.slablauncher.providers.color.theme.ColorTheme
import io.posidon.android.slablauncher.ui.home.MainActivity
import io.posidon.android.slablauncher.util.FakeLauncherActivity

class TutorialFragment : FragmentWithNext(R.layout.intro_tutorial) {

    override fun onViewCreated(v: View, savedInstanceState: Bundle?) {
        arrayOf(
            R.id.title,
            R.id.title0,
            R.id.title1,
        ).forEach { v.findViewById<TextView>(it).setTextColor(ColorTheme.uiTitle) }
        arrayOf(
            R.id.description0,
            R.id.description1,
        ).forEach { v.findViewById<TextView>(it).setTextColor(ColorTheme.uiDescription) }
    }

    override fun next(activity: IntroActivity) {
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
}