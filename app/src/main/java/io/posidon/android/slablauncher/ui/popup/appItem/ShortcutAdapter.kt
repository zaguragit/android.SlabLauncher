package io.posidon.android.slablauncher.ui.popup.appItem

import android.content.Context
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.posidon.android.slablauncher.R

class ShortcutAdapter(
    val shortcuts: List<ShortcutInfo>,
    val txtColor: Int
) : RecyclerView.Adapter<ShortcutViewHolder>() {

    override fun getItemCount(): Int = shortcuts.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ShortcutViewHolder {
        return ShortcutViewHolder(LayoutInflater.from(parent.context)
            .inflate(R.layout.long_press_item_popup_item, parent, false))
    }

    override fun onBindViewHolder(holder: ShortcutViewHolder, i: Int) {
        val s = shortcuts[i]
        holder.label.text = s.shortLabel
        holder.label.setTextColor(txtColor)
        val launcherApps = holder.itemView.context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as LauncherApps
        holder.icon.setImageDrawable(launcherApps.getShortcutIconDrawable(s, holder.itemView.resources.displayMetrics.densityDpi))
        holder.itemView.setOnClickListener {
            ItemLongPress.currentPopup?.dismiss()
            launcherApps.startShortcut(s, null, null)
        }
    }
}
