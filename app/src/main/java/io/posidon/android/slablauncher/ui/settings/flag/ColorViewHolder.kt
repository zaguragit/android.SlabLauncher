package io.posidon.android.slablauncher.ui.settings.flag

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.core.graphics.luminance
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.RecyclerView
import io.posidon.android.slablauncher.R
import io.posidon.android.slablauncher.util.drawable.setBackgroundColorFast

class ColorViewHolder(
    parent: ViewGroup,
    onColorChanged: (Context, newColor: Int, i: Int) -> Unit
) : RecyclerView.ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.flag_color_stripe, parent, false)) {

    private var listening = false
    val entry = itemView.findViewById<EditText>(R.id.entry).apply {
        addTextChangedListener {
            if (!listening) return@addTextChangedListener
            val newColor = (it?.toString()?.toIntOrNull(16) ?: 0) or 0xff000000.toInt()
            itemView.setBackgroundColorFast(newColor)
            onColorChanged(itemView.context, newColor, bindingAdapterPosition)
        }
    }
    val hash = itemView.findViewById<TextView>(R.id.hash)

    fun bind(color: Int) {
        listening = false
        itemView.setBackgroundColorFast(color)
        entry.setText((color and 0xffffff).toString(16))
        val textColor = (if (color.luminance > .6f) 0xff000000 else 0xffffffff).toInt()
        entry.setTextColor(textColor)
        hash.setTextColor(textColor)
        listening = true
    }
}