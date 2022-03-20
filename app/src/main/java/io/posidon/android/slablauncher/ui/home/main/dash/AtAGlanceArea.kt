package io.posidon.android.slablauncher.ui.home.main.dash

import android.annotation.SuppressLint
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
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.posidon.android.slablauncher.R
import io.posidon.android.slablauncher.data.items.LauncherItem
import io.posidon.android.slablauncher.data.notification.NotificationData
import io.posidon.android.slablauncher.providers.color.theme.ColorTheme
import io.posidon.android.slablauncher.providers.notification.NotificationService
import io.posidon.android.slablauncher.providers.personality.Statement
import io.posidon.android.slablauncher.providers.suggestions.SuggestionsManager
import io.posidon.android.slablauncher.ui.home.MainActivity
import io.posidon.android.slablauncher.ui.home.main.DashArea
import io.posidon.android.slablauncher.ui.home.main.acrylicBlur
import io.posidon.android.slablauncher.ui.home.main.dash.media.MediaPlayer
import io.posidon.android.slablauncher.ui.home.main.suggestion.SuggestionsAdapter
import io.posidon.android.slablauncher.ui.popup.home.HomeLongPressPopup
import io.posidon.android.slablauncher.util.storage.DoSuggestionStripSetting.doSuggestionStrip
import io.posidon.android.slablauncher.ui.view.SeeThroughView
import io.posidon.android.slablauncher.ui.view.recycler.RecyclerViewLongPressHelper
import posidon.android.conveniencelib.Device
import posidon.android.conveniencelib.dp
import posidon.android.conveniencelib.getStatusBarHeight
import posidon.android.conveniencelib.pullStatusbar

@SuppressLint("ClickableViewAccessibility")
class AtAGlanceArea(val view: View, dashArea: DashArea, val mainActivity: MainActivity) {

    companion object {
        const val SUGGESTION_COUNT = 5
    }

    val card = view.findViewById<CardView>(R.id.card)!!
    val statement = card.findViewById<TextView>(R.id.statement)!!
    val date = card.findViewById<TextView>(R.id.date)!!

    val notificationArea = card.findViewById<ViewGroup>(R.id.notification_area)!!

    val separators = arrayOf<View>(
        card.findViewById(R.id.separator),
        card.findViewById(R.id.separator1),
        card.findViewById(R.id.separator2),
    )

    val notificationsAdapter = NotificationAdapter()
    val notificationsRecycler = view.findViewById<RecyclerView>(R.id.notifications)!!.apply {
        layoutManager = LinearLayoutManager(view.context, RecyclerView.VERTICAL, false)
        adapter = notificationsAdapter
    }

    val primaryNotification = notificationArea.findViewById<ViewGroup>(R.id.primary_notification)
    val notificationIcon = primaryNotification.findViewById<ImageView>(R.id.icon)
    val notificationSource = primaryNotification.findViewById<TextView>(R.id.source)
    val notificationTitle = primaryNotification.findViewById<TextView>(R.id.title)
    val notificationText = primaryNotification.findViewById<TextView>(R.id.text)
    val notificationImageCard = primaryNotification.findViewById<CardView>(R.id.notification_image_card)
    val notificationImage = notificationImageCard.findViewById<ImageView>(R.id.notification_image)

    val mediaPlayer = MediaPlayer(view.findViewById(R.id.media_player), separators[2])

    val notificationMoreText = view.findViewById<TextView>(R.id.x_more)!!.apply {
        setOnClickListener {
            it.context.pullStatusbar()
        }
    }

    val suggestionsAdapter = SuggestionsAdapter(mainActivity, mainActivity.settings)
    val suggestionsRecycler = view.findViewById<RecyclerView>(R.id.suggestions_recycler)!!.apply {
        layoutManager = GridLayoutManager(view.context, SUGGESTION_COUNT, RecyclerView.VERTICAL, false)
        adapter = suggestionsAdapter
    }

    private val blurBG = view.findViewById<SeeThroughView>(R.id.dash_blur_bg)!!.apply {
        viewTreeObserver.addOnPreDrawListener {
            invalidate()
            true
        }
    }

    private val popupHeight get() =
        view.height -
        view.context.getStatusBarHeight() -
        suggestionsRecycler.let { if (it.isVisible) it.height else 0 } -
        view.resources.getDimension(R.dimen.item_card_margin).toInt() * 2
    private val popupWidth get() =
        view.width - view.resources.getDimension(R.dimen.item_card_margin).toInt() * 4

    private var popupX = 0f
    private var popupY = 0f
    init {
        NotificationService.setOnUpdate(AtAGlanceArea::class.simpleName!!) { _, new -> updateNotifications(new) }
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
                if (dashArea.scrollY == 0) Device.screenWidth(it.context) / 2f else popupX,
                if (dashArea.scrollY == 0) popupHeight / 2f + view.context.getStatusBarHeight() + m else popupY,
                mainActivity.settings,
                mainActivity::reloadColorPaletteSync,
                mainActivity::updateColorTheme,
                mainActivity::loadApps,
                mainActivity::reloadBlur,
                ::updateLayout,
                if (dashArea.scrollY == 0) popupWidth else ViewGroup.LayoutParams.WRAP_CONTENT,
                if (dashArea.scrollY == 0) popupHeight else HomeLongPressPopup.calculateHeight(it.context),
            )
            true
        }
        RecyclerViewLongPressHelper.setOnLongPressListener(suggestionsRecycler) { v, x, y ->
            val m = v.resources.getDimension(R.dimen.item_card_margin)
            HomeLongPressPopup.show(
                v,
                if (dashArea.scrollY == 0) Device.screenWidth(v.context) / 2f else x,
                if (dashArea.scrollY == 0) popupHeight / 2f + view.context.getStatusBarHeight() + m else y,
                mainActivity.settings,
                mainActivity::reloadColorPaletteSync,
                mainActivity::updateColorTheme,
                mainActivity::loadApps,
                mainActivity::reloadBlur,
                ::updateLayout,
                if (dashArea.scrollY == 0) popupWidth else ViewGroup.LayoutParams.WRAP_CONTENT,
                if (dashArea.scrollY == 0) popupHeight else HomeLongPressPopup.calculateHeight(v.context),
            )
        }
        updateLayout()
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
                notificationIcon.setImageDrawable(notification.icon)
                notificationSource.text = notification.source
                notificationTitle.text = notification.title
                notificationText.text = notification.description
                if (notification.image != null) {
                    notificationImage.setImageDrawable(notification.image)
                    notificationImageCard.isVisible = true
                    notificationImageCard.doOnLayout {
                        it.updateLayoutParams {
                            height = (it.width * notification.image.intrinsicHeight / notification.image.intrinsicWidth).coerceAtMost(view.dp(256).toInt())
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

    fun updateLayout() {
        suggestionsRecycler.isVisible = mainActivity.settings.doSuggestionStrip
    }

    fun updateColorTheme() {
        separators.forEach { it.setBackgroundColor(ColorTheme.cardHint) }
        card.setCardBackgroundColor(ColorTheme.cardBG)
        statement.setTextColor(ColorTheme.cardTitle)
        date.setTextColor(ColorTheme.cardDescription)

        notificationIcon.imageTintList = ColorStateList.valueOf(ColorTheme.cardDescription)
        notificationSource.setTextColor(ColorTheme.cardDescription)
        notificationTitle.setTextColor(ColorTheme.cardTitle)
        notificationText.setTextColor(ColorTheme.cardDescription)
        notificationMoreText.setTextColor(ColorTheme.cardDescription)
        notificationsAdapter.notifyItemRangeChanged(0, notificationsAdapter.itemCount)
        
        mediaPlayer.updateColorTheme()
    }

    fun onResume() {
        statement.text = Statement.get(view.context, Calendar.getInstance())
        updateNotifications(NotificationService.notifications)
    }

    fun updateSuggestions(pinnedItems: List<LauncherItem>) {
        suggestionsAdapter.updateItems((SuggestionsManager.getTimeBasedSuggestions() - pinnedItems.let {
            val s = DashArea.DOCK_ROWS * DashArea.COLUMNS
            if (it.size > s) it.subList(0, s)
            else it
        }.toSet()).let {
            if (it.size > SUGGESTION_COUNT) it.subList(0, SUGGESTION_COUNT)
            else it
        })
    }

    fun updateBlur() {
        blurBG.drawable = acrylicBlur?.smoothBlurDrawable
    }
}