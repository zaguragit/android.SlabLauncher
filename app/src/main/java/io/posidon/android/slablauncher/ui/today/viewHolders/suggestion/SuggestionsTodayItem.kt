package io.posidon.android.slablauncher.ui.today.viewHolders.suggestion

import io.posidon.android.slablauncher.data.items.LauncherItem

class SuggestionsTodayItem(
    val suggestions: List<LauncherItem>,
    val openAllApps: () -> Unit,
)