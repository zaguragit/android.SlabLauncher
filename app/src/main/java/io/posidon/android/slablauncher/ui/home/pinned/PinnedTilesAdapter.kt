package io.posidon.android.slablauncher.ui.home.pinned

import android.content.ClipData
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.cardview.widget.CardView
import androidx.core.view.marginBottom
import androidx.core.view.marginTop
import androidx.recyclerview.widget.RecyclerView
import io.posidon.android.slablauncher.LauncherContext
import io.posidon.android.slablauncher.R
import io.posidon.android.slablauncher.data.items.LauncherItem
import io.posidon.android.slablauncher.ui.home.MainActivity
import io.posidon.android.slablauncher.ui.home.pinned.viewHolders.DropTargetViewHolder
import io.posidon.android.slablauncher.ui.home.pinned.viewHolders.TileViewHolder
import io.posidon.android.slablauncher.ui.home.pinned.viewHolders.atAGlance.AtAGlanceViewHolder
import io.posidon.android.slablauncher.ui.home.pinned.viewHolders.bindDropTargetViewHolder

class PinnedTilesAdapter(
    val activity: MainActivity,
    val launcherContext: LauncherContext,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private var dropTargetIndex = -1
    private var items: MutableList<LauncherItem> = ArrayList()

    override fun getItemCount(): Int = iToAdapterPosition(items.size)

    override fun getItemViewType(i: Int): Int {
        return when (i) {
            0 -> 2
            dropTargetIndex -> 1
            else -> 0
        }
    }

    fun adapterPositionToI(position: Int): Int {
        return when {
            dropTargetIndex == -1 -> position - 1
            dropTargetIndex <= position -> position - 2
            else -> position - 1
        }
    }

    fun iToAdapterPosition(i: Int): Int {
        return when {
            dropTargetIndex == -1 -> i + 1
            dropTargetIndex <= i -> i + 2
            else -> i + 1
        }
    }

    private var atAGlanceViewHolder: AtAGlanceViewHolder? = null

    val verticalOffset get() = atAGlanceViewHolder?.itemView
        ?.run { height + marginTop + marginBottom } ?: 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            2 -> AtAGlanceViewHolder(LayoutInflater.from(parent.context)
                .inflate(R.layout.at_a_glance, parent, false), activity).also { atAGlanceViewHolder = it }
            1 -> DropTargetViewHolder(LayoutInflater.from(parent.context)
                .inflate(R.layout.tile_drop_target, parent, false) as CardView)
            else -> TileViewHolder(LayoutInflater.from(parent.context)
                .inflate(R.layout.tile, parent, false) as CardView)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, ii: Int) {
        if (ii == 0) {
            holder as AtAGlanceViewHolder
            holder.onBind()
            return
        }
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
            onDragStart = {
                val i = adapterPositionToI(holder.bindingAdapterPosition)
                items.removeAt(i)
                dropTargetIndex = holder.bindingAdapterPosition
                notifyItemChanged(holder.bindingAdapterPosition)
                updatePins(it)
            },
        )
    }

    fun updateItems(
        items: List<LauncherItem>
    ) {
        this.items = items.toMutableList()
        notifyDataSetChanged()
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
        if (i != dropTargetIndex) throw IllegalStateException("PinnedItemsAdapter -> i = $i, dropTargetIndex = $dropTargetIndex")
        val item = launcherContext.appManager.parseLauncherItem(clipData.getItemAt(0).text.toString())
        item?.let { items.add(i - 1, it) }
        dropTargetIndex = -1
        notifyDataSetChanged()
        updatePins(v)
    }
}