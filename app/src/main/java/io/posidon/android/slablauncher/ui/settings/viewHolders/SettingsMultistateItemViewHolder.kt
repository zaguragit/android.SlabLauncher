package io.posidon.android.slablauncher.ui.settings.viewHolders

import android.annotation.SuppressLint
import android.content.res.ColorStateList
import android.graphics.drawable.RippleDrawable
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import io.posidon.android.slablauncher.R
import io.posidon.android.slablauncher.providers.color.theme.ColorTheme
import io.posidon.android.slablauncher.ui.home.main.tile.viewHolders.hideIfNullOr
import io.posidon.android.slablauncher.ui.settings.SettingsItem
import io.posidon.android.slablauncher.util.drawable.FastColorDrawable
import io.posidon.android.slablauncher.ui.view.multiswitch.MultiSwitch
import io.posidon.android.conveniencelib.units.dp
import io.posidon.android.conveniencelib.units.toFloatPixels

class SettingsMultistateItemViewHolder(itemView: View) : SettingsViewHolder(itemView) {

    val icon = itemView.findViewById<ImageView>(R.id.icon)

    val text = itemView.findViewById<TextView>(R.id.text)
    val description = itemView.findViewById<TextView>(R.id.description)

    @SuppressLint("UseSwitchCompatOrMaterialCode")
    val switch = itemView.findViewById<MultiSwitch>(R.id.toggle)

    val ripple = RippleDrawable(ColorStateList.valueOf(0), null, FastColorDrawable(0xffffffff.toInt()))

    init {
        itemView.background = ripple
    }

    override fun onBind(item: SettingsItem<*>) {
        item as SettingsItem<Int>

        text.text = item.text
        description.text = item.description
        switch.setOnStateChangeListener(null)

        text.setTextColor(ColorTheme.cardTitle)
        switch.setBackgroundColor(ColorTheme.cardHint and 0x00ffffff or 0x55000000)
        switch.onColor = ColorTheme.accentColor
        switch.unsafeOnColor = ColorTheme.tintWithColor(ColorTheme.accentColor, 0xdd3333)
        switch.offColor = ColorTheme.cardHint
        switch.unsafeOffColor = ColorTheme.tintWithColor(ColorTheme.cardHint, 0xdd3333)
        switch.borderColor = 0x88000000.toInt()
        switch.borderWidth = 1f
        switch.radius = 32.dp.toFloatPixels(itemView)
        switch.smallRadius = 2.dp.toFloatPixels(itemView)
        switch.cellMargin = 4.dp.toFloatPixels(itemView)

        ripple.setColor(ColorStateList.valueOf(ColorTheme.accentColor and 0xffffff or 0x33000000))

        description.hideIfNullOr(item.description) {
            text = it
            setTextColor(ColorTheme.cardDescription)
        }
        icon.hideIfNullOr(item.icon) {
            setImageDrawable(it)
            imageTintList = ColorStateList.valueOf(ColorTheme.cardDescription)
        }
        switch.state = item.value ?: 0
        switch.states = item.states
        switch.setOnStateChangeListener(item.onValueChange!!)
        switch.unsafeLevel = item.unsafeLevel

        itemView.setOnClickListener {
            switch.state = (switch.state + 1) % switch.states
        }
    }
}