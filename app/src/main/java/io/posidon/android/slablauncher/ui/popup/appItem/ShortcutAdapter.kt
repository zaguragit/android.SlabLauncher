package io.posidon.android.slablauncher.ui.popup.appItem

import android.content.Context
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.posidon.android.slablauncher.R
import io.posidon.android.slablauncher.data.items.ShortcutItem
import io.posidon.android.slablauncher.providers.item.GraphicsLoader

class ShortcutAdapter(
    val shortcuts: List<ShortcutItem>,
    val txtColor: Int,
    val graphicsLoader: GraphicsLoader,
) : RecyclerView.Adapter<ShortcutViewHolder>() {

    override fun getItemCount(): Int = shortcuts.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShortcutViewHolder {
        return ShortcutViewHolder(LayoutInflater.from(parent.context)
            .inflate(R.layout.long_press_item_popup_item, parent, false))
    }

    override fun onBindViewHolder(holder: ShortcutViewHolder, i: Int) {
        val s = shortcuts[i]
        holder.label.text = s.label
        holder.label.setTextColor(txtColor)
        val iconData = graphicsLoader.load(holder.itemView.context, s)
        holder.icon.setImageDrawable(iconData.icon)
        holder.itemView.setOnClickListener {
            ItemLongPress.currentPopup?.dismiss()
            s.open(it.context, it)
        }
    }
}
