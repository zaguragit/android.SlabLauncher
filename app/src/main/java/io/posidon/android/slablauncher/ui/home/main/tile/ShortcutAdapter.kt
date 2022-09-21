package io.posidon.android.slablauncher.ui.home.main.tile

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.posidon.android.slablauncher.R
import io.posidon.android.slablauncher.data.items.ShortcutItem
import io.posidon.android.slablauncher.providers.item.GraphicsLoader
import io.posidon.android.slablauncher.ui.home.main.tile.viewHolders.ShortcutViewHolder

class ShortcutAdapter(
    private val shortcuts: List<ShortcutItem>,
    val graphicsLoader: GraphicsLoader,
) : RecyclerView.Adapter<ShortcutViewHolder>() {

    override fun getItemCount(): Int = shortcuts.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShortcutViewHolder {
        return ShortcutViewHolder(LayoutInflater.from(parent.context)
            .inflate(R.layout.tile_shortcut, parent, false))
    }

    override fun onBindViewHolder(holder: ShortcutViewHolder, i: Int) {
        val s = shortcuts[i]
        val iconData = graphicsLoader.load(holder.itemView.context, s)
        holder.icon.setImageDrawable(iconData.icon)
        holder.itemView.setOnClickListener {
            s.open(it.context, it)
        }
    }
}
