package io.posidon.android.slablauncher.ui.intro

import io.posidon.android.slablauncher.R

class SplashFragment : FragmentWithNext(R.layout.intro_splash) {
    override fun next(activity: IntroActivity) {
        activity.setFragment(PermissionsFragment())
    }
}