package io.posidon.android.slablauncher.ui.settings.flag

import android.annotation.SuppressLint
import android.content.Context
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import io.posidon.android.slablauncher.util.storage.FlagColors.flagColors
import io.posidon.android.slablauncher.util.storage.Settings

class ColorsAdapter(
    val settings: Settings,
    val onColorsChanged: () -> Unit,
) : RecyclerView.Adapter<ColorViewHolder>() {
    private var colors = settings.flagColors.toMutableList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        ColorViewHolder(parent, ::onColorChanged)

    override fun onBindViewHolder(holder: ColorViewHolder, i: Int) {
        holder.bind(colors[i])
    }

    override fun getItemCount() = colors.size

    fun onColorChanged(context: Context, newColor: Int, i: Int) {
        colors[i] = newColor
        saveColors(context)
    }

    fun addColor(context: Context) {
        colors.add(0)
        notifyItemInserted(colors.lastIndex)
        saveColors(context)
    }

    fun removeColor(context: Context, i: Int) {
        colors.removeAt(i)
        notifyItemRemoved(i)
        saveColors(context)
    }

    fun onMove(
        context: Context,
        from: Int,
        to: Int,
    ) {
        colors.add(to, colors.removeAt(from))
        notifyItemMoved(from, to)
        saveColors(context)
    }

    fun saveColors(context: Context) {
        settings.edit(context) {
            flagColors = colors.toIntArray()
            onColorsChanged()
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun updateColors() {
        colors = settings.flagColors.toMutableList()
        notifyDataSetChanged()
    }
}