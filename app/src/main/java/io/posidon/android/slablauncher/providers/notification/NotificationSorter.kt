package io.posidon.android.slablauncher.providers.notification

import io.posidon.android.slablauncher.data.notification.NotificationData
import java.time.Instant

object NotificationSorter {
    fun rearranged(items: List<NotificationData>): List<NotificationData> {
        items.sortedByDescending(NotificationSorter::sorter)
        return items
    }

    fun getMostRelevant(items: List<NotificationData>): NotificationData? {
        return items.maxByOrNull(NotificationSorter::sorter)
    }

    private fun sorter(it: NotificationData): Instant {
        var r = it.instant
        when (it.importance) {
            1 -> r = r.plusMillis(3600L * 2)
            2 -> r = r.plusMillis(3600L * 7)
        }
        if (it.isConversation) {
            r = r.plusMillis(3600L * 4)
        }
        return r
    }
}