package io.posidon.android.slablauncher.ui.settings.iconPackPicker

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.posidon.android.slablauncher.R
import io.posidon.android.slablauncher.ui.settings.iconPackPicker.viewHolders.IconPackViewHolder
import io.posidon.android.slablauncher.ui.settings.iconPackPicker.viewHolders.SectionViewHolder
import io.posidon.android.slablauncher.util.storage.Settings
import java.util.*

class IconPackPickerAdapter(
    val settings: Settings,
    val chosenIconPacks: LinkedList<IconPackPickerActivity.IconPack>,
    val availableIconPacks: LinkedList<IconPackPickerActivity.IconPack>,
    val systemPack: IconPackPickerActivity.IconPack,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun getItemViewType(i: Int): Int {
        return when {
            i == 0 || i == 1 + chosenIconPacks.size + 1 -> SECTION
            i == 1 + chosenIconPacks.size -> SYSTEM_ICON_PACK
            else -> ICON_PACK
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            SECTION -> SectionViewHolder(LayoutInflater.from(parent.context)
                .inflate(R.layout.icon_packs_section, parent, false))
            SYSTEM_ICON_PACK -> IconPackViewHolder(LayoutInflater.from(parent.context)
                .inflate(R.layout.icon_packs_item, parent, false), SYSTEM_ICON_PACK)
            else -> IconPackViewHolder(LayoutInflater.from(parent.context)
                .inflate(R.layout.icon_packs_item, parent, false), viewType)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, i: Int) {
        when (i) {
            0 -> (holder as SectionViewHolder).bind(holder.itemView.context.getString(R.string.chosen))
            1 + chosenIconPacks.size + 1 -> (holder as SectionViewHolder).bind(holder.itemView.context.getString(R.string.available))
            1 + chosenIconPacks.size -> (holder as IconPackViewHolder).bind(systemPack)
            else -> {
                holder as IconPackViewHolder
                val pack = if (i > 1 + chosenIconPacks.size + 1)
                    availableIconPacks[i - 1 - chosenIconPacks.size - 1 - 1]
                else chosenIconPacks[i - 1]
                holder.bind(pack)
            }
        }
    }

    override fun getItemCount(): Int = 1 + chosenIconPacks.size + 1 + 1 + availableIconPacks.size

    fun onItemMove(
        context: Context,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        val toPosition = target.adapterPosition
        if (target !is IconPackViewHolder && toPosition == 0) {
            return false
        }
        if (viewHolder is IconPackViewHolder && viewHolder.type == SYSTEM_ICON_PACK) {
            return false
        }

        val fromPosition = viewHolder.adapterPosition
        var changed = false
        val chosenIconPacksSize = chosenIconPacks.size
        val isFromAvailable = fromPosition >= 1 + chosenIconPacksSize + 1
        val isToAvailable = toPosition >= 1 + chosenIconPacksSize + 1
        val r = if (isFromAvailable) {
            if (isToAvailable) {
                return false
            }
            availableIconPacks.removeAt(fromPosition - 1 - chosenIconPacksSize - 1 - 1)
        } else {
            changed = true
            chosenIconPacks.removeAt(fromPosition - 1)
        }
        if (isToAvailable) {
            availableIconPacks.add(toPosition - 1 - chosenIconPacksSize - 1, r)
        } else {
            if (!isFromAvailable) {
                if (toPosition == 1 + chosenIconPacksSize) {
                    chosenIconPacks.add(fromPosition - 1, r)
                    return false
                }
            }
            changed = true
            chosenIconPacks.add(toPosition - 1, r)
        }
        notifyItemMoved(fromPosition, toPosition)
        if (changed) {
            settings.edit(context) {
                "icon_packs" set chosenIconPacks.map(IconPackPickerActivity.IconPack::packageName).toTypedArray()
            }
        }
        return true
    }

    companion object {
        const val ICON_PACK = 0
        const val SYSTEM_ICON_PACK = 1
        const val SECTION = 2
    }
}