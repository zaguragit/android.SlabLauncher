package io.posidon.android.slablauncher.data.notification

import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageView

class MediaPlayerData(
    val title: String,
    val subtitle: String?,

    val image: Drawable?,
    val color: Int,

    val onTap: () -> Unit?,
    val previous: (View) -> Unit,
    val next: (View) -> Unit,
    val togglePause: (ImageView) -> Unit,
    val isPlaying: () -> Boolean,

    val sourcePackageName: String,
)