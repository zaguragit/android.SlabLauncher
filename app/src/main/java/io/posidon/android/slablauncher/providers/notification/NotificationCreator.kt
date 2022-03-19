package io.posidon.android.slablauncher.providers.notification

import android.app.Notification
import android.app.NotificationManager
import android.content.ContentResolver
import android.content.Context
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toFile
import io.posidon.android.slablauncher.data.notification.NotificationData
import io.posidon.android.slablauncher.data.notification.TempNotificationData
import java.io.File
import java.net.URI

object NotificationCreator {

    inline fun getSource(context: Context, n: StatusBarNotification): String {
        return context.packageManager.getApplicationLabel(context.packageManager.getApplicationInfo(n.packageName, 0)).toString()
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

    inline fun getSmallIcon(context: Context, n: StatusBarNotification): Drawable? {
        return n.notification.smallIcon?.loadDrawable(context)
    }

    inline fun getLargeIcon(context: Context, n: StatusBarNotification): Drawable? {
        return n.notification.getLargeIcon()?.loadDrawable(context)
    }

    inline fun getBigImage(context: Context, extras: Bundle): Drawable? {
        val b = extras[Notification.EXTRA_PICTURE] as Bitmap?
        if (b != null) {
            try {
                if (b.width < 64 || b.height < 64) {
                    return null
                }
                return BitmapDrawable(context.resources, b)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return null
    }

    inline fun getBigImageFromMessages(context: Context, messagingStyle: NotificationCompat.MessagingStyle): Drawable? {
        messagingStyle.messages.asReversed().forEach {
            it.dataUri?.let { uri ->
                runCatching {
                    return Drawable.createFromStream(context.contentResolver.openInputStream(uri), null)
                }
            }
        }
        messagingStyle.historicMessages.asReversed().forEach {
            it.dataUri?.let { uri ->
                runCatching {
                    return Drawable.createFromStream(context.contentResolver.openInputStream(uri), null)
                }
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

    fun create(context: Context, notification: StatusBarNotification): TempNotificationData {

        val extras = notification.notification.extras

        var title = getTitle(extras)
        var text = getText(extras)
        if (title == null) {
            title = text
            text = null
        }

        val source = getSource(context, notification)
        val icon = getSmallIcon(context, notification)!!

        val channel = NotificationManagerCompat.from(context).getNotificationChannel(notification.notification.channelId)
        val importance = channel?.importance?.let { getImportance(it) } ?: 0

        var bigPic = getBigImage(context, extras)

        val messagingStyle = NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(
            notification.notification)
        val isConversation = messagingStyle != null
        if (messagingStyle != null) {
            messagingStyle.conversationTitle?.toString()?.let { title = it }
            messagingStyle.messages.lastOrNull()?.text?.toString()?.let { text = it }
            if (bigPic == null) {
                bigPic = getBigImageFromMessages(context, messagingStyle)
            }
        }

        return TempNotificationData(
            NotificationData(
                icon = icon,
                source = source,
                title = title?.toString() ?: "",
                description = text?.toString(),
                image = bigPic,
                sourcePackageName = notification.packageName,
            ),
            millis = notification.postTime,
            importance = importance.coerceAtLeast(0),
            isConversation = isConversation,
        )
    }
}