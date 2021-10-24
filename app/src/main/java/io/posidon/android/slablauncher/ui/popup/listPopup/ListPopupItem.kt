package io.posidon.android.slablauncher.ui.popup.listPopup

import android.graphics.drawable.Drawable
import android.view.View

class ListPopupItem(
    val text: String,
    val description: String? = null,
    val icon: Drawable? = null,
    val isTitle: Boolean = false,
    val value: Any? = null,
    val onToggle: ((View, Boolean) -> Unit)? = null,
    val onClick: ((View) -> Unit)? = null,
)