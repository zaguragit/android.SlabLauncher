package io.posidon.android.slablauncher.ui.home.today.viewHolders.apps

import io.posidon.android.slablauncher.data.items.LauncherItem

class SuggestedAppsTodayItem(
    val suggestions: List<LauncherItem>,
    val openAllApps: () -> Unit,
)