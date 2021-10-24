package io.posidon.android.slablauncher.data.notification

import android.content.res.Resources
import android.graphics.drawable.Drawable
import android.view.View
import io.posidon.android.slablauncher.R
import java.time.Instant

class NotificationData(
    val color: Int,
    val title: String,
    val sourceIcon: Drawable?,
    val description: String?,
    val source: String?,

    val image: Drawable?,

    /**
     * [Instant.MAX] if it's happening now / should on top of the feed (music playing, suggested apps...)
     * Feed items using [Instant.MAX] won't be included in the today filter
     */
    val instant: Instant,

    val onTap: (View) -> Unit,

    /**
     * Unique identifier (globally unique to this feed item)
     */
    val uid: String,

    val sourcePackageName: String?,
    val importance: Int,
    val isConversation: Boolean,
)

fun String.longHash(): Long {
    var h = 1125899906842597L // prime
    for (i in 0 until length) {
        h = 31 * h + this[i].code.toLong()
    }
    return h
}

fun NotificationData.formatTimeAgo(resources: Resources): String {
    val now = System.currentTimeMillis()
    val passed = now - instant.toEpochMilli()
    val seconds = passed / 1000
    if (seconds < 60) {
        return resources.getString(R.string.now)
    }
    val minutes = seconds / 60
    if (minutes < 60) {
        return "${minutes}m"
    }
    val hours = minutes / 60
    if (hours < 24) {
        return "${hours}h"
    }
    return "${hours / 24}d"
}

fun NotificationData.isToday(): Boolean {
    if (instant == Instant.MAX) return true
    val now = System.currentTimeMillis()
    val passed = now - instant.toEpochMilli()
    val seconds = passed / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24
    return days <= 1
}