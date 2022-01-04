package io.posidon.android.slablauncher.ui.home.pinned

import android.content.ClipData
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.core.view.marginBottom
import androidx.core.view.marginTop
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import io.posidon.android.computable.computedOrNull
import io.posidon.android.slablauncher.LauncherContext
import io.posidon.android.slablauncher.R
import io.posidon.android.slablauncher.data.items.LauncherItem
import io.posidon.android.slablauncher.data.items.getBanner
import io.posidon.android.slablauncher.data.notification.NotificationData
import io.posidon.android.slablauncher.providers.notification.NotificationService
import io.posidon.android.slablauncher.ui.home.MainActivity
import io.posidon.android.slablauncher.ui.home.pinned.TileDiffCallback.Companion.CHANGE_ALL
import io.posidon.android.slablauncher.ui.home.pinned.TileDiffCallback.Companion.CHANGE_BANNER_TEXT
import io.posidon.android.slablauncher.ui.home.pinned.TileDiffCallback.Companion.CHANGE_GRAPHICS
import io.posidon.android.slablauncher.ui.home.pinned.TileDiffCallback.Companion.CHANGE_LABEL
import io.posidon.android.slablauncher.ui.home.pinned.viewHolders.DropTargetViewHolder
import io.posidon.android.slablauncher.ui.home.pinned.viewHolders.TileViewHolder
import io.posidon.android.slablauncher.ui.home.pinned.viewHolders.atAGlance.AtAGlanceViewHolder
import io.posidon.android.slablauncher.ui.home.pinned.viewHolders.bindDropTargetViewHolder

class PinnedTilesAdapter(
    val activity: MainActivity,
    val launcherContext: LauncherContext,
    val fragment: TileAreaFragment,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var dropTargetIndex = -1
    private var items: MutableList<LauncherItem> = ArrayList()

    override fun getItemCount(): Int = items.size + if (dropTargetIndex == -1) 1 else 2

    val tileCount get() = items.size

    override fun getItemViewType(i: Int): Int {
        return when (i) {
            0 -> 2
            dropTargetIndex + 1 -> 1
            else -> 0
        }
    }

    fun adapterPositionToI(position: Int): Int {
        return when {
            dropTargetIndex == -1 -> position - 1
            dropTargetIndex + 1 < position -> position - 2
            else -> position - 1
        }
    }

    fun iToAdapterPosition(i: Int): Int {
        return when {
            dropTargetIndex == -1 -> i + 1
            dropTargetIndex < i -> i + 2
            else -> i + 1
        }
    }

    private var atAGlanceViewHolder: AtAGlanceViewHolder? = null

    val verticalOffset get() = atAGlanceViewHolder?.itemView
        ?.run { height + marginTop + marginBottom } ?: 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            2 -> AtAGlanceViewHolder(LayoutInflater.from(parent.context)
                .inflate(R.layout.at_a_glance, parent, false), activity, fragment).also { atAGlanceViewHolder = it }
            1 -> DropTargetViewHolder(LayoutInflater.from(parent.context)
                .inflate(R.layout.tile_drop_target, parent, false))
            else -> TileViewHolder(LayoutInflater.from(parent.context)
                .inflate(R.layout.tile, parent, false) as CardView)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, ii: Int) {
        if (ii == 0) {
            holder as AtAGlanceViewHolder
            holder.onBind(items)
            return
        }
        if (ii == dropTargetIndex + 1) {
            holder as DropTargetViewHolder
            bindDropTargetViewHolder(holder)
            return
        }
        val item = items[adapterPositionToI(ii)]
        holder as TileViewHolder
        bindViewHolderUpdateAll(holder, item)
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        ii: Int,
        payloads: MutableList<Any>
    ) {
        if (ii == 0) {
            holder as AtAGlanceViewHolder
            holder.onBind(items)
            return
        }
        if (ii == dropTargetIndex + 1) {
            holder as DropTargetViewHolder
            bindDropTargetViewHolder(holder)
            return
        }
        val item = items[adapterPositionToI(ii)]
        holder as TileViewHolder

        if (payloads.isEmpty()) {
            return bindViewHolderUpdateAll(holder, item)
        }
        payloads.forEach { payload ->
            payload as List<*>

            if (payload.contains(CHANGE_ALL))
                return bindViewHolderUpdateAll(holder, item)

            if (payload.contains(CHANGE_BANNER_TEXT))
                holder.updateBannerText(item.getBanner())

            if (payload.contains(CHANGE_LABEL))
                holder.updateLabel(item)

            if (payload.contains(CHANGE_GRAPHICS)) {
                val b = item.getBanner()
                holder.updateBackground(item, b.background.computedOrNull(), activity.settings, b)
            }
        }

        holder.updateTimeMark(item)

        println("payloads: " + payloads.joinToString("; ") { (it as List<*>?)?.joinToString().toString() })
    }

    private fun bindViewHolderUpdateAll(
        holder: TileViewHolder,
        item: LauncherItem
    ) {
        holder.bind(
            item,
            activity,
            activity.settings,
            onDragStart = {
                val i = adapterPositionToI(holder.bindingAdapterPosition)
                items.removeAt(i)
                dropTargetIndex = i
                notifyItemChanged(holder.bindingAdapterPosition)
                updatePins(it)
            },
        )
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        val i = adapterPositionToI(holder.bindingAdapterPosition)
        if (i >= 0 && holder is TileViewHolder)
            holder.recycle(items[i])
    }

    fun updateItems(
        items: List<LauncherItem>
    ) {
        val c = TileDiffCallback(this.items, items, NotificationService.notifications, NotificationService.notifications)
        val diff = DiffUtil.calculateDiff(c)
        this.items = items.toMutableList()
        diff.dispatchUpdatesTo(this)
    }

    fun updateItems(old: List<NotificationData>, new: List<NotificationData>) {
        val c = TileDiffCallback(this.items, items, old, new)
        val diff = DiffUtil.calculateDiff(c)
        diff.dispatchUpdatesTo(this)
    }

    private fun updatePins(v: View) {
        launcherContext.appManager.setPinned(v.context, ArrayList(items))
    }

    fun showDropTarget(i: Int) {
        if (i != dropTargetIndex) {
            when {
                i == -1 -> {
                    val old = dropTargetIndex
                    dropTargetIndex = -1
                    notifyItemRemoved(old + 1)
                }
                dropTargetIndex == -1 -> {
                    dropTargetIndex = i
                    notifyItemInserted(i + 1)
                }
                else -> {
                    val old = dropTargetIndex
                    dropTargetIndex = i
                    notifyItemMoved(old + 1, i + 1)
                }
            }
        }
    }

    fun onDrop(v: View, i: Int, clipData: ClipData) {
        if (i != dropTargetIndex) Toast.makeText(
            v.context,
            "PinnedTilesAdapter -> i = $i, dropTargetIndex = $dropTargetIndex",
            Toast.LENGTH_LONG
        ).show()
        val item = launcherContext.appManager.tryParseLauncherItem(clipData.getItemAt(0).text.toString(), v.context)
        item?.let { items.add(i, it) }
        dropTargetIndex = -1
        notifyItemChanged(i + 1)
        updateItems(items)
        updatePins(v)
    }
}