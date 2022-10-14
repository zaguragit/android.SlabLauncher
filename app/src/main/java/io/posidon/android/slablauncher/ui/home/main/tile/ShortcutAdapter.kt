package io.posidon.android.slablauncher.ui.home.main.tile

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.posidon.android.slablauncher.R
import io.posidon.android.slablauncher.data.items.LauncherItem
import io.posidon.android.slablauncher.providers.item.GraphicsLoader
import io.posidon.android.slablauncher.ui.home.main.tile.viewHolders.ShortcutViewHolder
import io.posidon.android.slablauncher.util.storage.Settings

class ShortcutAdapter(
    private val shortcuts: List<LauncherItem>,
    val graphicsLoader: GraphicsLoader,
    val settings: Settings,
) : RecyclerView.Adapter<ShortcutViewHolder>() {

    override fun getItemCount(): Int = shortcuts.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShortcutViewHolder {
        return ShortcutViewHolder(LayoutInflater.from(parent.context)
            .inflate(R.layout.tile_shortcut, parent, false))
    }

    override fun onBindViewHolder(holder: ShortcutViewHolder, i: Int) {
        val s = shortcuts[i]
        holder.onBind(s, graphicsLoader, settings)
    }
}
