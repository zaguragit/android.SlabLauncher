package io.posidon.android.slablauncher.ui.intro

import android.os.Bundle
import android.view.View
import android.widget.TextView
import io.posidon.android.slablauncher.R
import io.posidon.android.slablauncher.providers.color.theme.ColorTheme

class SplashFragment : FragmentWithNext(R.layout.intro_splash) {
    override fun next(activity: IntroActivity) {
        activity.setFragment(PermissionsFragment())
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        view.findViewById<TextView>(R.id.title).setTextColor(ColorTheme.uiTitle)
    }
}