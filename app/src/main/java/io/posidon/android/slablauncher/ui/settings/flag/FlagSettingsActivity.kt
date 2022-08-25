package io.posidon.android.slablauncher.ui.settings.flag

import android.graphics.drawable.Drawable
import android.os.Bundle
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.ItemTouchHelper.DOWN
import androidx.recyclerview.widget.ItemTouchHelper.UP
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.posidon.android.launcherutil.IconTheming
import io.posidon.android.slablauncher.R
import io.posidon.android.slablauncher.ui.settings.SettingsActivity
import io.posidon.android.slablauncher.ui.settings.iconPackPicker.viewHolders.IconPackViewHolder
import io.posidon.android.conveniencelib.getNavigationBarHeight
import io.posidon.android.conveniencelib.getStatusBarHeight
import java.util.*

class FlagSettingsActivity : SettingsActivity() {
    override fun init(savedInstanceState: Bundle?) {
        setContentView(R.layout.activity_settings_flag)

    }
}