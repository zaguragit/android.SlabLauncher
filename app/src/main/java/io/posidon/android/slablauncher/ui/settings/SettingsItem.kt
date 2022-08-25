package io.posidon.android.slablauncher.ui.settings

import android.graphics.drawable.Drawable
import android.view.View

class SettingsItem <T : Any> (
    val text: String,
    val description: String? = null,
    val icon: Drawable? = null,
    val isTitle: Boolean = false,
    val value: T? = null,
    val states: Int = 0,
    val unsafeLevel: Int = -1,
    val onValueChange: ((View, T) -> Unit)? = null,
    val onClick: ((View) -> Unit)? = null,
)