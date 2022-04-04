package io.posidon.android.slablauncher.ui.settings.iconPackPicker

import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ItemTouchHelper.DOWN
import androidx.recyclerview.widget.ItemTouchHelper.UP
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.posidon.android.launcherutils.IconTheming
import io.posidon.android.slablauncher.R
import io.posidon.android.slablauncher.ui.settings.SettingsActivity
import io.posidon.android.slablauncher.ui.settings.iconPackPicker.viewHolders.IconPackViewHolder
import io.posidon.android.conveniencelib.getNavigationBarHeight
import io.posidon.android.conveniencelib.getStatusBarHeight
import java.util.*

class IconPackPickerActivity : SettingsActivity() {
    override fun init(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_settings_icon_pack_picker)
        val recycler = findViewById<RecyclerView>(R.id.recycler)

        val iconPacks = IconTheming.getAvailableIconPacks(packageManager).mapTo(LinkedList()) {
            IconPack(
                it.loadIcon(packageManager),
                it.loadLabel(packageManager).toString(),
                it.activityInfo.packageName
            )
        }

        iconPacks.sortWith { o1, o2 ->
            o1.label.compareTo(o2.label, ignoreCase = true)
        }

        val chosenIconPacks = run {
            val list = LinkedList<IconPack>()
            val strings = settings.getStrings("icon_packs")?.let(Array<String>::toMutableList) ?: return@run list
            var deleted = false
            for (string in strings) {
                val iconPack = iconPacks.find { it.packageName == string }
                if (iconPack == null) {
                    strings -= string
                    deleted = true
                } else {
                    iconPacks -= iconPack
                    list += iconPack
                }
            }
            if (deleted) {
                settings.edit(this) {
                    "icon_packs" set strings.toTypedArray()
                }
            }
            list
        }

        val systemPack = IconPack(
            ContextCompat.getDrawable(this, R.mipmap.ic_launcher)!!,
            getString(R.string.system),
            "system"
        )

        recycler.setPadding(0, getStatusBarHeight(), 0, getNavigationBarHeight())
        recycler.layoutManager = LinearLayoutManager(this, RecyclerView.VERTICAL, false)
        val adapter = IconPackPickerAdapter(settings, chosenIconPacks, iconPacks, systemPack)

        recycler.adapter = adapter
        val th = ItemTouchHelper(TouchCallback(adapter))
        th.attachToRecyclerView(recycler)
    }

    class TouchCallback(val adapter: IconPackPickerAdapter) : ItemTouchHelper.Callback() {
        override fun getMovementFlags(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder
        ) = makeMovementFlags(if (viewHolder is IconPackViewHolder && viewHolder.type != IconPackPickerAdapter.SYSTEM_ICON_PACK) UP or DOWN else 0, 0)

        override fun onSwiped(v: RecyclerView.ViewHolder, d: Int) {}

        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean {
            return adapter.onItemMove(recyclerView.context, viewHolder, target)
        }
    }

    class IconPack(
        val icon: Drawable,
        val label: String,
        val packageName: String,
    )
}