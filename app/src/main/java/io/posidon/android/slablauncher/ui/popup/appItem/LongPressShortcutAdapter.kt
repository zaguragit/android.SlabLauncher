package io.posidon.android.slablauncher.ui.popup.appItem

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.posidon.android.slablauncher.R
import io.posidon.android.slablauncher.data.items.ShortcutItem
import io.posidon.android.slablauncher.providers.item.GraphicsLoader

class LongPressShortcutAdapter(
    private val shortcuts: List<ShortcutItem>,
    private val txtColor: Int,
    val graphicsLoader: GraphicsLoader,
) : RecyclerView.Adapter<LongPressShortcutViewHolder>() {

    override fun getItemCount(): Int = shortcuts.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LongPressShortcutViewHolder {
        return LongPressShortcutViewHolder(LayoutInflater.from(parent.context)
            .inflate(R.layout.long_press_item_popup_item, parent, false))
    }

    override fun onBindViewHolder(holder: LongPressShortcutViewHolder, i: Int) {
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
