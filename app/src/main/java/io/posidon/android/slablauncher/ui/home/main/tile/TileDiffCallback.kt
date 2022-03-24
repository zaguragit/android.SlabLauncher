package io.posidon.android.slablauncher.ui.home.main.tile

import androidx.recyclerview.widget.DiffUtil
import io.posidon.android.slablauncher.data.items.LauncherItem
import io.posidon.android.slablauncher.data.notification.NotificationData

class TileDiffCallback(
    val old: List<LauncherItem>,
    val new: List<LauncherItem>,
) : DiffUtil.Callback() {

    fun getOld(i: Int) = old[i]
    fun getNew(i: Int) = new[i]

    override fun getOldListSize() = old.size
    override fun getNewListSize() = new.size

    override fun areItemsTheSame(oldI: Int, newI: Int): Boolean {
        val old = getOld(oldI)
        val new = getNew(newI)
        return old == new
    }

    override fun areContentsTheSame(oldI: Int, newI: Int): Boolean {
        val old = getOld(oldI)
        val new = getNew(newI)
        val oldBanner = old.tileImage
        val newBanner = new.tileImage
        return old.label == new.label
            && old.icon.isComputed()
            && new.icon.isComputed()
            && old.icon.computed() === new.icon.computed()
            && old.color.isComputed()
            && new.color.isComputed()
            && old.color.computed() == new.color.computed()
            && oldBanner == newBanner
    }
}