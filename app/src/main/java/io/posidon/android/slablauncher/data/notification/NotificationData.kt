package io.posidon.android.slablauncher.data.notification

import android.graphics.drawable.Drawable

class NotificationData(
    val icon: Drawable,
    val description: String?,
    val image: Drawable?,
    val open: () -> Unit,
    val cancel: () -> Unit,
)

class NotificationGroupData(
    val title: String,
    val source: String,
    val sourcePackageName: String?,
    val notifications: List<NotificationData>,
)

class TempNotificationData(
    val group: NotificationGroupData,
    val millis: Long,
    val importance: Int,
    val isConversation: Boolean,
)