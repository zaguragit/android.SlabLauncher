package io.posidon.android.slablauncher.ui.settings.viewHolders

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.*
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import io.posidon.android.slablauncher.R
import io.posidon.android.slablauncher.providers.color.theme.ColorTheme
import io.posidon.android.slablauncher.ui.home.main.tile.viewHolders.hideIfNullOr
import io.posidon.android.slablauncher.ui.settings.SettingsItem
import io.posidon.android.slablauncher.util.drawable.FastColorDrawable
import io.posidon.android.conveniencelib.units.dp
import io.posidon.android.conveniencelib.units.toPixels

class SettingsSeekBarItemViewHolder(itemView: View) : SettingsViewHolder(itemView) {

    private val icon = itemView.findViewById<ImageView>(R.id.icon)

    private val text = itemView.findViewById<TextView>(R.id.text)
    private val description = itemView.findViewById<TextView>(R.id.description)

    private val seekBar = itemView.findViewById<SeekBar>(R.id.seekbar)

    private val ripple = RippleDrawable(ColorStateList.valueOf(0), null, FastColorDrawable(0xffffffff.toInt()))

    init {
        itemView.background = ripple
        seekBar.splitTrack = false
    }

    override fun onBind(item: SettingsItem<*>) {
        item as SettingsItem<Int>

        text.text = item.text
        description.text = item.description

        text.setTextColor(ColorTheme.cardTitle)
        seekBar.progressDrawable = generateDrawable()
        seekBar.thumb = generateThumb(itemView.context)

        ripple.setColor(ColorStateList.valueOf(ColorTheme.accentColor and 0xffffff or 0x33000000))

        description.hideIfNullOr(item.description) {
            text = it
            setTextColor(ColorTheme.cardDescription)
        }
        icon.hideIfNullOr(item.icon) {
            setImageDrawable(it)
            imageTintList = ColorStateList.valueOf(ColorTheme.cardDescription)
        }
        seekBar.progress = item.value ?: 0
        seekBar.max = item.states
        seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onStartTrackingTouch(v: SeekBar) {}
            override fun onStopTrackingTouch(v: SeekBar) {}
            override fun onProgressChanged(v: SeekBar, progress: Int, fromUser: Boolean) {
                item.onValueChange!!(v, progress)
            }
        })
    }

    private fun generateDrawable(): Drawable {
        val out = LayerDrawable(arrayOf(
            generateBG(0xff08090a.toInt()),
            ClipDrawable(generateBG(ColorTheme.accentColor and 0x00ffffff or 0x88000000.toInt()), Gravity.START, GradientDrawable.Orientation.BL_TR.ordinal)
        ))
        out.setId(0, android.R.id.background)
        out.setId(1, android.R.id.progress)
        return out
    }

    private fun generateThumb(context: Context): Drawable {
        return generateCircle(context, ColorTheme.accentColor)
    }

    fun generateCircle(context: Context, color: Int): Drawable {
        val r = 18.dp.toPixels(context)
        val inset = 4.dp.toPixels(context)
        return LayerDrawable(arrayOf(
            GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(color)
                setSize(r, r)
                setStroke(1, 0x33000000)
            },
        )).apply {
            setLayerInset(0, inset, inset, inset, inset)
        }
    }

    fun generateBG(color: Int): Drawable {
        return GradientDrawable().apply {
            cornerRadius = Float.MAX_VALUE
            setColor(color)
            setStroke(1, 0x88000000.toInt())
        }
    }
}