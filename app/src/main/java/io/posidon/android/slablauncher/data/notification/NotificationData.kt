package io.posidon.android.slablauncher.data.notification

import android.graphics.drawable.Drawable

class NotificationData(
    val icon: Drawable,
    val source: String,
    val title: String,
    val description: String?,
    val image: Drawable?,
    val sourcePackageName: String?,
    val open: () -> Unit,
    val cancel: () -> Unit,
)

class TempNotificationData(
    val notificationData: NotificationData,
    val millis: Long,
    val importance: Int,
    val isConversation: Boolean,
)