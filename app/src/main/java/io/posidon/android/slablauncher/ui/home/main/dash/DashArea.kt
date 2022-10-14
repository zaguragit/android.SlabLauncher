package io.posidon.android.slablauncher.ui.home.main.dash

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.res.ColorStateList
import android.icu.util.Calendar
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.posidon.android.conveniencelib.Device
import io.posidon.android.conveniencelib.getStatusBarHeight
import io.posidon.android.conveniencelib.units.dp
import io.posidon.android.conveniencelib.units.toPixels
import io.posidon.android.slablauncher.R
import io.posidon.android.slablauncher.data.notification.NotificationGroupData
import io.posidon.android.slablauncher.providers.color.theme.ColorTheme
import io.posidon.android.slablauncher.providers.notification.NotificationService
import io.posidon.android.slablauncher.ui.home.MainActivity
import io.posidon.android.slablauncher.ui.home.main.HomeArea
import io.posidon.android.slablauncher.ui.popup.home.HomeLongPressPopup
import io.posidon.android.slablauncher.ui.view.FlagView
import io.posidon.android.slablauncher.ui.view.recycler.RecyclerViewLongPressHelper
import io.posidon.android.slablauncher.util.storage.DoFlag.doFlag
import io.posidon.android.slablauncher.util.storage.FlagColors.flagColors
import io.posidon.android.slablauncher.util.storage.FlagHeight.flagHeight

@SuppressLint("ClickableViewAccessibility")
class DashArea(val view: View, homeArea: HomeArea, val mainActivity: MainActivity) {

    private val date = view.findViewById<TextView>(R.id.date)!!
    private val alarm = view.findViewById<TextView>(R.id.alarm)!!

    private val flag = view.findViewById<FlagView>(R.id.flag)

    private val notificationsAdapter = NotificationAdapter(mainActivity.launcherContext)
    private val notificationsRecycler = view.findViewById<RecyclerView>(R.id.notifications)!!.apply {
        layoutManager = LinearLayoutManager(view.context, RecyclerView.VERTICAL, false)
        adapter = notificationsAdapter
    }

    private val popupHeight get() = (
        view.height -
        view.context.getStatusBarHeight() -
        view.resources.getDimension(R.dimen.item_card_margin).toInt() * 2
    ).coerceAtLeast(480.dp.toPixels(view))

    private val popupWidth get() =
        view.width - view.resources.getDimension(R.dimen.item_card_margin).toInt() * 4

    private var popupX = 0f
    private var popupY = 0f
    init {
        NotificationService.setOnUpdate(DashArea::class.simpleName!!) { _, new -> updateNotifications(new) }
        view.setOnTouchListener { _, e ->
            when (e.action and MotionEvent.ACTION_MASK) {
                MotionEvent.ACTION_DOWN -> {
                    popupX = e.rawX
                    popupY = e.rawY
                }
            }
            false
        }
        view.setOnLongClickListener {
            showSettingsPopup(it, homeArea, popupX, popupY)
            true
        }
        RecyclerViewLongPressHelper.setOnLongPressListener(notificationsRecycler) { v, x, y ->
            showSettingsPopup(v, homeArea, x, y)
        }
        date.setOnClickListener(::openClockApp)
        alarm.setOnClickListener(::openClockApp)
    }

    private fun showSettingsPopup(
        v: View,
        homeArea: HomeArea,
        x: Float,
        y: Float
    ) {
        val m = v.resources.getDimension(R.dimen.item_card_margin)
        HomeLongPressPopup.show(
            v,
            if (homeArea.scrollY == 0) Device.screenWidth(v.context) / 2f else x,
            if (homeArea.scrollY == 0) popupHeight / 2f + view.context.getStatusBarHeight() + m else y,
            mainActivity.settings,
            mainActivity::reloadColorPaletteSync,
            mainActivity::updateColorTheme,
            mainActivity::invalidateItemGraphics,
            mainActivity::reloadBlur,
            mainActivity::updateLayout,
            mainActivity::updateGreeting,
            if (homeArea.scrollY == 0) popupWidth else ViewGroup.LayoutParams.WRAP_CONTENT,
            if (homeArea.scrollY == 0) popupHeight else HomeLongPressPopup.calculateHeight(v.context),
        )
    }

    private fun openClockApp(v: View) {
        val i = Intent("android.intent.action.SHOW_ALARMS")
        try {
            v.context.startActivity(i)
        } catch (_: ActivityNotFoundException) {}
    }

    private fun updateNotifications(new: List<NotificationGroupData>) {
        mainActivity.runOnUiThread {
            if (new.isEmpty()) {
                notificationsRecycler.isVisible = false
                return@runOnUiThread
            }
            notificationsRecycler.isVisible = true
            notificationsAdapter.updateItems(new.subList(0, new.size.coerceAtMost(3)))
        }
    }

    fun updateColorTheme() {
        date.setTextColor(ColorTheme.uiTitle)
        alarm.setTextColor(ColorTheme.uiDescription)
        alarm.compoundDrawableTintList = ColorStateList.valueOf(ColorTheme.uiDescription)

        notificationsAdapter.notifyItemRangeChanged(0, notificationsAdapter.itemCount)
    }

    @SuppressLint("SetTextI18n")
    fun onResume() {
        val nextAlarm = view.context.getSystemService(AlarmManager::class.java).nextAlarmClock
        if (nextAlarm == null) {
            alarm.isVisible = false
        } else {
            val c = Calendar.getInstance()
            c.timeInMillis = nextAlarm.triggerTime
            alarm.text = "${c[Calendar.HOUR_OF_DAY]}:${c[Calendar.MINUTE].toString().padStart(2, '0')}"
            alarm.isVisible = true
        }
        updateNotifications(NotificationService.notifications)
        updateFlag()
    }

    fun updateBlur() {
    }

    fun updateFlag() {
        if (!mainActivity.settings.doFlag) {
            flag.isVisible = false
            return
        }
        flag.isVisible = true
        flag.colors = mainActivity.settings.flagColors
        flag.updateLayoutParams {
            height = mainActivity.settings.flagHeight.dp.toPixels(flag)
        }
    }
}