package io.posidon.android.slablauncher.ui.popup.listPopup

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.posidon.android.slablauncher.R
import io.posidon.android.slablauncher.ui.popup.listPopup.viewHolders.*

class ListPopupAdapter : RecyclerView.Adapter<ListPopupViewHolder>() {

    override fun getItemViewType(i: Int): Int {
        return when {
            items[i].states == 2 -> 2
            items[i].states > 3 -> 4
            items[i].states > 2 -> 3
            items[i].isTitle -> if (items[i].icon != null) -1 else 1
            else -> 0
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListPopupViewHolder {
        return when (viewType) {
            -1 -> ListPopupPrimaryTitleViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.list_popup_primary_title, parent, false))
            1 -> ListPopupTitleViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.list_popup_title, parent, false))
            2 -> ListPopupSwitchItemViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.list_popup_switch_item, parent, false))
            3 -> ListPopupMultistateItemViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.list_popup_multistate_item, parent, false))
            4 -> ListPopupSeekBarItemViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.list_popup_seekbar_item, parent, false))
            else -> ListPopupItemViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.list_popup_item, parent, false))
        }
    }

    override fun onBindViewHolder(holder: ListPopupViewHolder, i: Int) {
        holder.onBind(items[i])
    }

    override fun getItemCount() = items.size

    private var items: List<ListPopupItem> = emptyList()

    fun updateItems(items: List<ListPopupItem>) {
        this.items = items
        notifyDataSetChanged()
    }
}
