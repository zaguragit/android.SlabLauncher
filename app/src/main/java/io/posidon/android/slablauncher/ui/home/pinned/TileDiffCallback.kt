package io.posidon.android.slablauncher.ui.home.pinned

import androidx.recyclerview.widget.DiffUtil
import io.posidon.android.slablauncher.data.items.LauncherItem
import io.posidon.android.slablauncher.data.notification.NotificationData
import java.util.*

class TileDiffCallback(
    val old: List<LauncherItem>,
    val new: List<LauncherItem>,
    val oldNotifications: List<NotificationData>,
    val newNotifications: List<NotificationData>,
) : DiffUtil.Callback() {

    fun getOld(i: Int) = old[i - 1]
    fun getNew(i: Int) = new[i - 1]

    override fun getOldListSize() = old.size + 1
    override fun getNewListSize() = new.size + 1

    override fun areItemsTheSame(oldI: Int, newI: Int): Boolean {
        if (oldI == 0 && newI == 0) return false
        if (oldI == 0) return false
        if (newI == 0) return false
        val old = getOld(oldI)
        val new = getNew(newI)
        return old == new
    }

    override fun areContentsTheSame(oldI: Int, newI: Int): Boolean {
        if (oldI == 0) return false
        if (newI == 0) return false
        val old = getOld(oldI)
        val new = getNew(newI)
        val oldBanner = old.getBanner(oldNotifications)
        val newBanner = new.getBanner(newNotifications)
        return old.label == new.label
            && old.icon.isComputed()
            && new.icon.isComputed()
            && old.icon.computed() === new.icon.computed()
            && old.color.isComputed()
            && new.color.isComputed()
            && old.color.computed() == new.color.computed()
            && oldBanner.background.isComputed()
            && newBanner.background.isComputed()
            && oldBanner.background.computed() === newBanner.background.computed()
            && oldBanner == newBanner
    }

    override fun getChangePayload(oldI: Int, newI: Int): Any {
        if (oldI == 0) return false
        if (newI == 0) return false
        val old = getOld(oldI)
        val new = getNew(newI)
        val oldBanner = old.getBanner(oldNotifications)
        val newBanner = new.getBanner(newNotifications)
        val changes = LinkedList<Int>()
        if (
            !old.icon.isComputed() ||
            !new.icon.isComputed() ||
            !old.color.isComputed() ||
            !new.color.isComputed() ||
            !oldBanner.background.isComputed() ||
            !newBanner.background.isComputed() ||
            oldBanner.hideIcon != newBanner.hideIcon ||
            oldBanner.bgOpacity != newBanner.bgOpacity
        ) {
            changes += CHANGE_ALL
            return changes
        }
        if (
            oldBanner.title != newBanner.title ||
            oldBanner.text != newBanner.text
        ) changes += CHANGE_BANNER_TEXT
        if (old.label != new.label)
            changes += CHANGE_LABEL
        if (
            old.color.computed() != new.color.computed() ||
            old.icon.computed() !== new.icon.computed() ||
            oldBanner.background.computed() !== newBanner.background.computed()
        ) changes += CHANGE_GRAPHICS
        return changes
    }

    companion object {
        const val CHANGE_ALL = -1
        const val CHANGE_BANNER_TEXT = 0
        const val CHANGE_LABEL = 1
        const val CHANGE_GRAPHICS = 2
    }
}