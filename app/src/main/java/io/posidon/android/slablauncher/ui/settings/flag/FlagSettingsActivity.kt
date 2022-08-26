package io.posidon.android.slablauncher.ui.settings.flag

import android.app.AlertDialog
import android.os.Bundle
import android.view.View
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.posidon.android.conveniencelib.getNavigationBarHeight
import io.posidon.android.conveniencelib.getStatusBarHeight
import io.posidon.android.conveniencelib.units.dp
import io.posidon.android.conveniencelib.units.toPixels
import io.posidon.android.slablauncher.R
import io.posidon.android.slablauncher.ui.settings.SettingsActivity
import io.posidon.android.slablauncher.ui.settings.SettingsAdapter
import io.posidon.android.slablauncher.ui.settings.SettingsItem
import io.posidon.android.slablauncher.ui.view.FlagView
import io.posidon.android.slablauncher.util.storage.DoFlag.doFlag
import io.posidon.android.slablauncher.util.storage.FlagColors.flagColors
import io.posidon.android.slablauncher.util.storage.FlagHeight.flagHeight

class FlagSettingsActivity : SettingsActivity() {

    companion object {
        val FLAG_PRESET_ACE = intArrayOf(0xff000000.toInt(), 0xff7f7f7f.toInt(), 0xffffffff.toInt(), 0xff660066.toInt())
        val FLAG_PRESET_NB = intArrayOf(0xfffcf431.toInt(), 0xfffcfcfc.toInt(), 0xff9d59d2.toInt(), 0xff2a2a2a.toInt())
        val FLAG_PRESET_RAINBOW = intArrayOf(0xffe50000.toInt(), 0xffff8d00.toInt(), 0xffffee00.toInt(), 0xff008121.toInt(), 0xff3a62bf.toInt(), 0xff760188.toInt())
        val FLAG_PRESET_TRANS = intArrayOf(0xff5bcffa.toInt(), 0xfff5abb9.toInt(), 0xffffffff.toInt(), 0xfff5abb9.toInt(), 0xff5bcffa.toInt())
        val FLAG_PRESETS = arrayOf(FLAG_PRESET_ACE, FLAG_PRESET_NB, FLAG_PRESET_RAINBOW, FLAG_PRESET_TRANS)
    }

    override fun init(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_settings_flag)

        val container = findViewById<View>(R.id.container).apply {
            setPadding(0, getStatusBarHeight(), 0, getNavigationBarHeight())
        }
        val flag = findViewById<FlagView>(R.id.flag).apply {
            colors = settings.flagColors
            updateLayoutParams {
                height = settings.flagHeight.dp.toPixels(applicationContext)
            }
        }

        val adapter = SettingsAdapter()
        val colorsAdapter = ColorsAdapter(settings) {
            runOnUiThread {
                flag.colors = settings.flagColors
            }
        }

        val recycler = findViewById<RecyclerView>(R.id.recycler).apply {
            layoutManager = LinearLayoutManager(applicationContext, RecyclerView.VERTICAL, false)
            this.adapter = adapter
        }
        val colorsRecycler = findViewById<RecyclerView>(R.id.colors_recycler).apply {
            layoutManager = LinearLayoutManager(applicationContext, RecyclerView.VERTICAL, false)
            this.adapter = colorsAdapter
        }

        adapter.updateItems(listOf(
            SettingsItem(
                text = getString(R.string.enable),
                icon = getDrawable(R.drawable.ic_visible),
                states = 2,
                value = settings.doFlag,
                onValueChange = { v, b ->
                    settings.edit(v.context) {
                        doFlag = b
                    }
                }
            ),
            SettingsItem(
                text = getString(R.string.height),
                icon = getDrawable(R.drawable.ic_swipe_up),
                states = 192,
                value = settings.flagHeight,
                onValueChange = { v, h ->
                    settings.edit(v.context) {
                        flagHeight = h
                    }
                    flag.updateLayoutParams {
                        height = h.dp.toPixels(applicationContext)
                    }
                }
            ),
            SettingsItem(
                text = getString(R.string.apply_from_preset),
                icon = getDrawable(R.drawable.ic_shapes),
                onClick = {
                    AlertDialog.Builder(this)
                        .setItems(
                            resources.getStringArray(R.array.flag_presets)
                        ) { d, i ->
                            flag.colors = FLAG_PRESETS[i]
                            settings.edit(this) {
                                flagColors = FLAG_PRESETS[i]
                                runOnUiThread(colorsAdapter::updateColors)
                            }
                            d.dismiss()
                        }
                        .show()
                },
            ),
            SettingsItem(
                text = getString(R.string.add_color),
                icon = getDrawable(R.drawable.ic_plus),
                onClick = { colorsAdapter.addColor(this) },
            ),
        ))

        val th = ItemTouchHelper(TouchCallback(colorsAdapter))
        th.attachToRecyclerView(colorsRecycler)
    }

    class TouchCallback(val adapter: ColorsAdapter) : ItemTouchHelper.Callback() {
        override fun getMovementFlags(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder
        ) = makeMovementFlags(ItemTouchHelper.UP or ItemTouchHelper.DOWN, ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT)

        override fun onSwiped(v: RecyclerView.ViewHolder, d: Int) {
            adapter.removeColor(v.itemView.context, v.bindingAdapterPosition)
        }

        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            val fromPosition = viewHolder.bindingAdapterPosition
            val toPosition = target.bindingAdapterPosition
            adapter.onMove(recyclerView.context, fromPosition, toPosition)
            return true
        }
    }
}