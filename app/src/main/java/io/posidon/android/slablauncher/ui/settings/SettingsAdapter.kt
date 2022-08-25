package io.posidon.android.slablauncher.ui.settings

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.posidon.android.slablauncher.R
import io.posidon.android.slablauncher.ui.settings.viewHolders.*

class SettingsAdapter : RecyclerView.Adapter<SettingsViewHolder>() {

    override fun getItemViewType(i: Int): Int {
        val item = items[i]
        return when {
            item.states == 2 -> 2
            item.states == 3 -> 3
            item.states > 3 -> 4
            item.isTitle -> if (item.icon != null) -1 else 1
            item.value is String -> 5
            else -> 0
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SettingsViewHolder {
        return when (viewType) {
            -1 -> SettingsPrimaryTitleViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.settings_primary_title, parent, false))
            1 -> SettingsTitleViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.settings_title, parent, false))
            2 -> SettingsSwitchItemViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.settings_switch_item, parent, false))
            3 -> SettingsMultistateItemViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.settings_multistate_item, parent, false))
            4 -> SettingsSeekBarItemViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.settings_seekbar_item, parent, false))
            5 -> SettingsEntryItemViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.settings_entry_item, parent, false))
            else -> SettingsItemViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.settings_item, parent, false))
        }
    }

    override fun onBindViewHolder(holder: SettingsViewHolder, i: Int) {
        holder.onBind(items[i])
    }

    override fun getItemCount() = items.size

    private var items: List<SettingsItem<*>> = emptyList()

    fun updateItems(items: List<SettingsItem<*>>) {
        this.items = items
        notifyDataSetChanged()
    }
}
