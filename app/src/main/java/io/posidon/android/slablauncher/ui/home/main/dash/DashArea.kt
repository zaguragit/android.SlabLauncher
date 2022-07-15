package io.posidon.android.slablauncher.ui.home.main.dash

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.content.res.ColorStateList
import android.icu.util.Calendar
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.view.doOnLayout
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.posidon.android.conveniencelib.Device
import io.posidon.android.conveniencelib.getStatusBarHeight
import io.posidon.android.conveniencelib.pullStatusbar
import io.posidon.android.conveniencelib.units.dp
import io.posidon.android.conveniencelib.units.toPixels
import io.posidon.android.slablauncher.R
import io.posidon.android.slablauncher.data.notification.NotificationData
import io.posidon.android.slablauncher.providers.color.theme.ColorTheme
import io.posidon.android.slablauncher.providers.notification.NotificationService
import io.posidon.android.slablauncher.providers.personality.Statement
import io.posidon.android.slablauncher.ui.home.MainActivity
import io.posidon.android.slablauncher.ui.home.main.HomeArea
import io.posidon.android.slablauncher.ui.home.main.acrylicBlur
import io.posidon.android.slablauncher.ui.home.main.dash.media.MediaPlayer
import io.posidon.android.slablauncher.ui.popup.home.HomeLongPressPopup
import io.posidon.android.slablauncher.ui.view.SeeThroughView
import io.posidon.android.slablauncher.ui.view.recycler.RecyclerViewLongPressHelper
import io.posidon.android.slablauncher.util.drawable.setBackgroundColorFast

@SuppressLint("ClickableViewAccessibility")
class DashArea(val view: View, homeArea: HomeArea, val mainActivity: MainActivity) {

    val card = view.findViewById<CardView>(R.id.card)!!
    val container = card.findViewById<View>(R.id.container)!!
    private val statement = card.findViewById<TextView>(R.id.statement)!!
    private val date = card.findViewById<TextView>(R.id.date)!!
    private val alarm = card.findViewById<TextView>(R.id.alarm)!!

    private val notificationArea = card.findViewById<ViewGroup>(R.id.notification_area)!!

    private val separators = arrayOf<View>(
        card.findViewById(R.id.separator),
        card.findViewById(R.id.separator1),
    )

    private val notificationsAdapter = NotificationAdapter()
    private val notificationsRecycler = view.findViewById<RecyclerView>(R.id.notifications)!!.apply {
        layoutManager = LinearLayoutManager(view.context, RecyclerView.VERTICAL, false)
        adapter = notificationsAdapter
    }

    private val primaryNotification = notificationArea.findViewById<ViewGroup>(R.id.primary_notification)
    private val notificationIcon = primaryNotification.findViewById<ImageView>(R.id.icon)
    private val notificationSource = primaryNotification.findViewById<TextView>(R.id.source)
    private val notificationTitle = primaryNotification.findViewById<TextView>(R.id.title)
    private val notificationText = primaryNotification.findViewById<TextView>(R.id.text)
    private val notificationImageCard = primaryNotification.findViewById<CardView>(R.id.notification_image_card)
    private val notificationImage = notificationImageCard.findViewById<ImageView>(R.id.notification_image)

    val mediaPlayer = MediaPlayer(view.findViewById(R.id.media_player), mainActivity::updateLayout)

    private val notificationMoreText = view.findViewById<TextView>(R.id.x_more)!!.apply {
        setOnClickListener {
            it.context.pullStatusbar()
        }
    }

    private val blurBG = view.findViewById<SeeThroughView>(R.id.dash_blur_bg)!!.apply {
        viewTreeObserver.addOnPreDrawListener {
            invalidate()
            true
        }
    }

    private val popupHeight get() = (
        view.height -
        view.context.getStatusBarHeight() -
        view.resources.getDimension(R.dimen.item_card_margin).toInt() * 2
    ).coerceAtLeast(512.dp.toPixels(view))

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
            val loc = IntArray(2)
            it.getLocationOnScreen(loc)
            val m = it.resources.getDimension(R.dimen.item_card_margin)
            HomeLongPressPopup.show(
                it,
                if (homeArea.scrollY == 0) Device.screenWidth(it.context) / 2f else popupX,
                if (homeArea.scrollY == 0) popupHeight / 2f + view.context.getStatusBarHeight() + m else popupY,
                mainActivity.settings,
                mainActivity::reloadColorPaletteSync,
                mainActivity::updateColorTheme,
                mainActivity::invalidateItemGraphics,
                mainActivity::reloadBlur,
                mainActivity::updateLayout,
                ::updateGreeting,
                if (homeArea.scrollY == 0) popupWidth else ViewGroup.LayoutParams.WRAP_CONTENT,
                if (homeArea.scrollY == 0) popupHeight else HomeLongPressPopup.calculateHeight(it.context),
            )
            true
        }
        RecyclerViewLongPressHelper.setOnLongPressListener(notificationsRecycler) { v, x, y ->
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
                ::updateGreeting,
                if (homeArea.scrollY == 0) popupWidth else ViewGroup.LayoutParams.WRAP_CONTENT,
                if (homeArea.scrollY == 0) popupHeight else HomeLongPressPopup.calculateHeight(v.context),
            )
        }
    }

    private fun updateNotifications(new: List<NotificationData>) {
        mainActivity.runOnUiThread {
            if (new.isEmpty()) {
                notificationArea.isVisible = false
                separators[0].isVisible = false
                return@runOnUiThread
            }
            notificationArea.isVisible = true
            separators[0].isVisible = true
            val shownCount: Int
            val m = NotificationService.mediaItem != null
            if (new.size >= 3) {
                shownCount = new.size.coerceAtMost(if (m) 3 else 4)
                val list = new.subList(0, shownCount).toMutableList()
                val notification = list.firstOrNull { it.image != null } ?: list[0]
                list.remove(notification)
                notificationsAdapter.updateItems(list)
                primaryNotification.setOnClickListener { notification.open() }
                notificationIcon.setImageDrawable(notification.icon)
                notificationSource.text = notification.source
                notificationTitle.text = notification.title
                notificationText.text = notification.description
                if (notification.image != null) {
                    notificationImage.setImageDrawable(notification.image)
                    notificationImageCard.isVisible = true
                    notificationImageCard.doOnLayout {
                        it.updateLayoutParams {
                            height = (
                                it.width * notification.image.intrinsicHeight / notification.image.intrinsicWidth
                            ).coerceAtMost(256.dp.toPixels(view))
                        }
                    }
                } else {
                    notificationImageCard.isVisible = false
                }
                primaryNotification.isVisible = true
                separators[1].isVisible = true
            } else {
                shownCount = new.size.coerceAtMost(if (m) 2 else 3)
                notificationsAdapter.updateItems(new.subList(0, shownCount))
                primaryNotification.isVisible = false
                separators[1].isVisible = false
            }
            val more = new.size - shownCount
            if (more != 0) {
                notificationMoreText.text = view.context.getString(R.string.x_more, more)
                notificationMoreText.isVisible = true
            } else {
                notificationMoreText.isVisible = false
            }
        }
    }

    fun updateColorTheme() {
        val s = ColorTheme.separator
        container.foregroundTintList = ColorStateList.valueOf(s)
        separators.forEach { it.setBackgroundColorFast(s) }
        card.setCardBackgroundColor(ColorTheme.cardBG)
        statement.setTextColor(ColorTheme.cardTitle)
        date.setTextColor(ColorTheme.cardDescription)
        alarm.setTextColor(ColorTheme.cardDescription)
        alarm.compoundDrawableTintList = ColorStateList.valueOf(ColorTheme.cardDescription)

        notificationIcon.imageTintList = ColorStateList.valueOf(ColorTheme.cardDescription)
        notificationSource.setTextColor(ColorTheme.cardDescription)
        notificationTitle.setTextColor(ColorTheme.cardTitle)
        notificationText.setTextColor(ColorTheme.cardDescription)
        notificationMoreText.setTextColor(ColorTheme.cardDescription)
        notificationsAdapter.notifyItemRangeChanged(0, notificationsAdapter.itemCount)

        mediaPlayer.updateColorTheme()
    }

    @SuppressLint("SetTextI18n")
    fun onResume() {
        updateGreeting()
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
    }

    fun updateGreeting() {
        statement.text = Statement.get(view.context, Calendar.getInstance(), mainActivity.settings)
    }

    fun updateBlur() {
        blurBG.drawable = acrylicBlur?.smoothBlurDrawable
        mediaPlayer.updateBlur()
    }

    fun onWindowFocusChanged(hasFocus: Boolean) {
        mediaPlayer.onWindowFocusChanged(hasFocus)
    }
}