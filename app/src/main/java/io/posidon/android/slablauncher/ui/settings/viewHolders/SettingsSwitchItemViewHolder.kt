package io.posidon.android.slablauncher.ui.settings.viewHolders

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.*
import android.util.StateSet
import android.view.View
import android.widget.ImageView
import android.widget.Switch
import android.widget.TextView
import io.posidon.android.slablauncher.R
import io.posidon.android.slablauncher.providers.color.theme.ColorTheme
import io.posidon.android.slablauncher.ui.home.main.tile.viewHolders.hideIfNullOr
import io.posidon.android.slablauncher.ui.settings.SettingsItem
import io.posidon.android.slablauncher.util.drawable.FastColorDrawable
import io.posidon.android.conveniencelib.units.dp
import io.posidon.android.conveniencelib.units.toPixels

class SettingsSwitchItemViewHolder(itemView: View) : SettingsViewHolder(itemView) {

    val icon = itemView.findViewById<ImageView>(R.id.icon)

    val text = itemView.findViewById<TextView>(R.id.text)
    val description = itemView.findViewById<TextView>(R.id.description)

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    val switch = itemView.findViewById<Switch>(R.id.toggle)

    val ripple = RippleDrawable(ColorStateList.valueOf(0), null, FastColorDrawable(0xffffffff.toInt()))

    init {
        itemView.background = ripple
    }

    override fun onBind(item: SettingsItem<*>) {
        item as SettingsItem<Any>

        text.text = item.text
        description.text = item.description

        itemView.setOnClickListener {
            switch.toggle()
        }

        text.setTextColor(ColorTheme.cardTitle)
        switch.trackDrawable = generateTrackDrawable()
        switch.thumbDrawable = generateThumbDrawable(itemView.context)

        ripple.setColor(ColorStateList.valueOf(ColorTheme.accentColor and 0xffffff or 0x33000000))

        description.hideIfNullOr(item.description) {
            text = it
            setTextColor(ColorTheme.cardDescription)
        }
        icon.hideIfNullOr(item.icon) {
            setImageDrawable(it)
            imageTintList = ColorStateList.valueOf(ColorTheme.cardDescription)
        }
        switch.isChecked = (item.value as? Boolean) ?: (item.value as Int != 0)
        switch.setOnCheckedChangeListener { v, value ->
            item.onValueChange!!(v, if (item.value is Boolean) value else if (value) 1 else 0)
        }
    }

    private fun generateTrackDrawable(): Drawable {
        val out = StateListDrawable()
        out.addState(intArrayOf(android.R.attr.state_checked), generateBG(ColorTheme.accentColor and 0x00ffffff or 0x55000000))
        out.addState(StateSet.WILD_CARD, generateBG(ColorTheme.cardHint and 0x00ffffff or 0x55000000))
        return out
    }

    private fun generateThumbDrawable(context: Context): Drawable {
        val out = StateListDrawable()
        out.addState(intArrayOf(android.R.attr.state_checked), generateCircle(context, ColorTheme.accentColor))
        out.addState(StateSet.WILD_CARD, generateCircle(context, ColorTheme.cardHint and 0x00ffffff or 0x55000000))
        return out
    }

    private fun generateCircle(context: Context, color: Int): Drawable {
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

    private fun generateBG(color: Int): Drawable {
        return GradientDrawable().apply {
            cornerRadius = Float.MAX_VALUE
            setColor(color)
            setStroke(1, 0x88000000.toInt())
        }
    }
}