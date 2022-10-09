package io.posidon.android.slablauncher.providers.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationChannelGroup
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Build
import android.os.UserHandle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import io.posidon.android.slablauncher.BuildConfig
import io.posidon.android.slablauncher.data.notification.MediaPlayerData
import io.posidon.android.slablauncher.data.notification.NotificationData
import io.posidon.android.slablauncher.data.notification.NotificationGroupData
import io.posidon.android.slablauncher.data.notification.TempNotificationData
import io.posidon.android.slablauncher.providers.media.MediaItemCreator
import io.posidon.android.slablauncher.util.StackTraceActivity
import java.util.*
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.concurrent.thread

class NotificationService : NotificationListenerService() {

    private val componentName = ComponentName(BuildConfig.APPLICATION_ID, this::class.java.name)

    override fun onCreate() {
        StackTraceActivity.init(applicationContext)
        if (!NotificationManagerCompat.getEnabledListenerPackages(applicationContext).contains(applicationContext.packageName)) {
            stopSelf()
        } else {
            val msm = getSystemService(MediaSessionManager::class.java)
            msm.addOnActiveSessionsChangedListener(::onMediaControllersUpdated, componentName)
            onMediaControllersUpdated(msm.getActiveSessions(componentName))
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        val msm = getSystemService(MediaSessionManager::class.java)
        msm.removeOnActiveSessionsChangedListener(::onMediaControllersUpdated)
    }

    override fun onListenerConnected() {
        loadNotifications(activeNotifications)
    }

    override fun onNotificationPosted(s: StatusBarNotification) = loadNotifications(activeNotifications)
    override fun onNotificationPosted(s: StatusBarNotification?, rm: RankingMap?) = loadNotifications(activeNotifications)
    override fun onNotificationRemoved(s: StatusBarNotification) = loadNotifications(activeNotifications)
    override fun onNotificationRemoved(s: StatusBarNotification?, rm: RankingMap?) = loadNotifications(activeNotifications)
    override fun onNotificationRemoved(s: StatusBarNotification, rm: RankingMap, reason: Int) = loadNotifications(activeNotifications)
    override fun onNotificationRankingUpdate(rm: RankingMap) = loadNotifications(activeNotifications)
    override fun onNotificationChannelModified(pkg: String, u: UserHandle, c: NotificationChannel, modifType: Int) = loadNotifications(activeNotifications)
    override fun onNotificationChannelGroupModified(pkg: String, u: UserHandle, g: NotificationChannelGroup, modifType: Int) = loadNotifications(activeNotifications)

    private fun loadNotifications(notifications: Array<StatusBarNotification>?) {
        thread(name = "NotificationService loading thread", isDaemon = true) {
            val tmpNotifications = LinkedList<TempNotificationData>()
            try {
                if (notifications != null) {
                    for (i in notifications.indices) {
                        val notification = notifications[i]
                        if (
                            Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
                            && notification.notification.bubbleMetadata?.isNotificationSuppressed == true
                        ) continue

                        val isSummary = notification.notification.flags and Notification.FLAG_GROUP_SUMMARY != 0

                        if (isSummary)
                            continue

                        val isMusic = notification.notification.extras
                            .getCharSequence(Notification.EXTRA_TEMPLATE) == Notification.MediaStyle::class.java.name

                        if (isMusic)
                            continue

                        val isAnnoying = !notification.isClearable && run {
                            val messagingStyle = NotificationCompat.MessagingStyle.extractMessagingStyleFromNotification(
                                notification.notification)
                            val isConversation = messagingStyle != null
                            !isConversation
                        } && run {
                            val channel = NotificationManagerCompat.from(applicationContext).getNotificationChannel(notification.notification.channelId)
                            (channel?.importance?.let { NotificationCreator.getImportance(it) } ?: 0) <= 0
                        }

                        if (isAnnoying)
                            continue

                        tmpNotifications += NotificationCreator.create(
                            applicationContext,
                            notification,
                            this,
                        )
                    }
                }
                tmpNotifications.sortByDescending {
                    var r = it.millis
                    when (it.importance) {
                        1 -> r += 3600_000L * 3
                        2 -> r += 3600_000L * 7
                        else -> if (it.group.sourcePackageName == "android")
                            r -= 3600_000L * 1
                    }
                    if (it.isConversation) {
                        r += 3600_000L * 5
                    }
                    r
                }
            }
            catch (e: Exception) { e.printStackTrace() }
            val groups = tmpNotifications
                .map { it.group }
                .groupBy { Triple(it.title, it.source, it.sourcePackageName) }
                .map { (k, v) -> NotificationGroupData(k.first, k.second, k.third, v.flatMap { it.notifications }) }
            val old = Companion.notifications
            Companion.notifications = groups
            listeners.forEach { (_, x) -> x(old, Companion.notifications) }
        }
    }

    private fun onMediaControllersUpdated(controllers: MutableList<MediaController>?) {
        val old = mediaItem
        if (controllers.isNullOrEmpty()) {
            mediaItem = null
            if (old != mediaItem) {
                onMediaUpdate()
            }
            return
        }
        val controller = pickController(controllers)
        mediaItem = controller.metadata?.let { MediaItemCreator.create(applicationContext, controller, it) }
        if (old != mediaItem) {
            onMediaUpdate()
        }
        controller.registerCallback(object : MediaController.Callback() {
            override fun onMetadataChanged(metadata: MediaMetadata?) {
                mediaItem = metadata?.let { MediaItemCreator.create(applicationContext, controller, it) }
                onMediaUpdate()
            }
        })
    }

    companion object {
        fun init(context: Context) {
            if (NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)) {
                context.startService(Intent(context, NotificationService::class.java))
            }
        }

        var notifications: List<NotificationGroupData> = ArrayList()
            private set

        var mediaItem: MediaPlayerData? = null
            private set

        private var onMediaUpdate: () -> Unit = {}

        private fun pickController(controllers: List<MediaController>): MediaController {
            for (i in controllers.indices) {
                val mc = controllers[i]
                if (mc.playbackState?.state == PlaybackState.STATE_PLAYING) {
                    return mc
                }
            }
            return controllers[0]
        }

        private val listeners = HashMap<String, (old: List<NotificationGroupData>, new: List<NotificationGroupData>) -> Unit>()

        fun setOnUpdate(key: String, onUpdate: (old: List<NotificationGroupData>, new: List<NotificationGroupData>) -> Unit) {
            listeners[key] = onUpdate
        }

        fun setOnMediaUpdate(onUpdate: () -> Unit) {
            this.onMediaUpdate = onUpdate
        }
    }
}