package io.posidon.android.slablauncher.providers.notification

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.os.Bundle
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import io.posidon.android.slablauncher.data.notification.NotificationData
import java.time.Instant

object NotificationCreator {

    inline fun getSmallIcon(context: Context, n: StatusBarNotification): Drawable? {
        return n.notification.smallIcon?.loadDrawable(context) ?: n.notification.getLargeIcon()?.loadDrawable(context)
    }

    inline fun getLargeIcon(context: Context, n: StatusBarNotification): Drawable? {
        return n.notification.getLargeIcon()?.loadDrawable(context)
    }

    inline fun getSource(context: Context, n: StatusBarNotification): String {
        return context.packageManager.getApplicationLabel(context.packageManager.getApplicationInfo(n.packageName, 0)).toString()
    }

    inline fun getColor(n: StatusBarNotification): Int {
        return n.notification.color
    }

    inline fun getTitle(extras: Bundle): CharSequence? {
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)
        if (title == null || title.toString().replace(" ", "").isEmpty()) {
            return null
        }
        return title
    }

    inline fun getText(extras: Bundle): CharSequence? {
        val messages = extras.getParcelableArray(Notification.EXTRA_MESSAGES)
        return if (messages == null) {
            extras.getCharSequence(Notification.EXTRA_BIG_TEXT)
                ?: extras.getCharSequence(Notification.EXTRA_TEXT)
        } else buildString {
            messages.forEach {
                val bundle = it as Bundle
                appendLine(bundle.getCharSequence("text"))
            }
            delete(lastIndex, length)
        }
    }

    inline fun getBigImage(context: Context, extras: Bundle): Drawable? {
        val b = extras[Notification.EXTRA_PICTURE] as Bitmap?
        if (b != null) {
            try {
                val d = BitmapDrawable(context.resources, b)
                if (b.width < 64 || b.height < 64) {
                    return null
                }
                return d
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return null
    }

    inline fun getImportance(importance: Int): Int {
        return when (importance) {
            NotificationManager.IMPORTANCE_NONE,
            NotificationManager.IMPORTANCE_MIN -> -1
            NotificationManager.IMPORTANCE_LOW,
            NotificationManager.IMPORTANCE_DEFAULT -> 0
            NotificationManager.IMPORTANCE_HIGH -> 1
            NotificationManager.IMPORTANCE_MAX -> 2
            else -> throw IllegalStateException("Invalid notification importance")
        }
    }

    fun create(context: Context, notification: StatusBarNotification, notificationService: NotificationService): NotificationData {

        val extras = notification.notification.extras

        var title = getTitle(extras)
        var text = getText(extras)
        if (title == null) {
            title = text
            text = null
        }
        val icon = getSmallIcon(context, notification)
        var source = getSource(context, notification)

        //println(extras.keySet().joinToString("\n") { "$it -> " + extras[it].toString() })

        val instant = Instant.ofEpochMilli(notification.postTime)

        val color = getColor(notification)

        val channel = NotificationManagerCompat.from(context).getNotificationChannel(notification.notification.channelId)
        val importance = channel?.importance?.let { getImportance(it) } ?: 0

        val uid = buildString {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                append('⍾')
                append((notification.id.toLong() shl 32 or notification.uid.toLong()).toString(16))
                append('⍾')
                append(notification.tag)
            } else {
                append(notification.packageName)
                append('⍾')
                append(notification.id.toString(16))
                append('⍾')
                append(notification.tag)
            }
        }

        val autoCancel = notification.notification.flags and Notification.FLAG_AUTO_CANCEL != 0

        val bigPic = getBigImage(context, extras)

        val messagingStyle = NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(
            notification.notification)
        val isConversation = messagingStyle != null
        if (isConversation) {
            messagingStyle?.conversationTitle?.toString()?.let { source = it }
            messagingStyle?.messages?.lastOrNull()?.text?.toString()?.let { text = it }
        }

        return NotificationData(
            color = color,
            title = title?.toString() ?: "",
            sourceIcon = icon,
            description = text?.toString(),
            source = source,
            image = bigPic,
            instant = instant,
            onTap = {
                try {
                    notification.notification.contentIntent?.send()
                    if (autoCancel)
                        notificationService.cancelNotification(notification.key)
                } catch (e: Exception) {
                    notificationService.cancelNotification(notification.key)
                    e.printStackTrace()
                }
            },
            uid = uid,
            sourcePackageName = notification.packageName,
            importance = importance.coerceAtLeast(0),
            isConversation = isConversation,
        )
    }
}