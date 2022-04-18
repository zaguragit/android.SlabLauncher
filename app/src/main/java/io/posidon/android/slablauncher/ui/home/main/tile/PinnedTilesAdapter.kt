package io.posidon.android.slablauncher.ui.home.main.tile

import android.content.ClipData
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.cardview.widget.CardView
import androidx.core.view.isInvisible
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import io.posidon.android.slablauncher.LauncherContext
import io.posidon.android.slablauncher.R
import io.posidon.android.slablauncher.data.items.LauncherItem
import io.posidon.android.slablauncher.providers.notification.NotificationService
import io.posidon.android.slablauncher.ui.home.MainActivity
import io.posidon.android.slablauncher.ui.home.main.DashAreaFragment
import io.posidon.android.slablauncher.ui.home.main.tile.viewHolders.DropTargetViewHolder
import io.posidon.android.slablauncher.ui.home.main.tile.viewHolders.TileViewHolder
import io.posidon.android.slablauncher.ui.home.main.tile.viewHolders.bindDropTargetViewHolder

class PinnedTilesAdapter(
    val activity: MainActivity,
    val launcherContext: LauncherContext,
    val fragment: DashAreaFragment,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var dropTargetIndex = -1
    private var items: MutableList<LauncherItem> = ArrayList()

    override fun getItemCount(): Int = items.size + if (dropTargetIndex == -1) 0 else 1

    val tileCount get() = items.size

    override fun getItemViewType(i: Int): Int {
        return when (i) {
            dropTargetIndex -> 1
            else -> 0
        }
    }

    fun adapterPositionToI(position: Int): Int {
        return when {
            dropTargetIndex == -1 -> position
            dropTargetIndex < position -> position - 1
            else -> position - 1
        }
    }

    fun iToAdapterPosition(i: Int): Int {
        return when {
            dropTargetIndex == -1 -> i
            dropTargetIndex < i -> i + 1
            else -> i
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            1 -> DropTargetViewHolder(LayoutInflater.from(parent.context)
                .inflate(R.layout.tile_drop_target, parent, false))
            else -> TileViewHolder(LayoutInflater.from(parent.context)
                .inflate(R.layout.tile, parent, false) as CardView)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, ii: Int) {
        if (ii == dropTargetIndex) {
            holder as DropTargetViewHolder
            bindDropTargetViewHolder(holder)
            return
        }
        val item = items[adapterPositionToI(ii)]
        holder as TileViewHolder
        holder.bind(
            item,
            activity,
            activity.settings,
            activity.graphicsLoader,
            onDragStart = {
                holder.itemView.isInvisible = true
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
        val c = TileDiffCallback(this.items, items)
        val diff = DiffUtil.calculateDiff(c)
        this.items = items.toMutableList()
        diff.dispatchUpdatesTo(this)
    }

    fun forceUpdateItems(
        items: List<LauncherItem>
    ) {
        val oldItems = this.items
        this.items = items.toMutableList()
        notifyItemRangeChanged(0, oldItems.size)
    }

    private fun updatePins(context: Context) {
        launcherContext.appManager.setPinned(context, ArrayList(items))
    }

    fun onDragOut(view: View, i: Int) {
        view.isVisible = true
        items.removeAt(i)
        dropTargetIndex = i
        notifyItemChanged(i)
        updatePins(view.context)
    }

    fun showDropTarget(i: Int) {
        if (i != dropTargetIndex) {
            when {
                i == -1 -> {
                    val old = dropTargetIndex
                    dropTargetIndex = -1
                    notifyItemRemoved(old)
                }
                dropTargetIndex == -1 -> {
                    dropTargetIndex = i
                    notifyItemInserted(i)
                }
                else -> {
                    val old = dropTargetIndex
                    dropTargetIndex = i
                    notifyItemMoved(old, i)
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
        notifyItemChanged(i)
        updatePins(v.context)
    }
}