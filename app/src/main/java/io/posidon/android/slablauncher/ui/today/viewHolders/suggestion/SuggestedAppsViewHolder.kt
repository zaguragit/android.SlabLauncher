package io.posidon.android.slablauncher.ui.today.viewHolders.suggestion

import android.content.res.ColorStateList
import android.view.View
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.posidon.android.slablauncher.R
import io.posidon.android.slablauncher.providers.color.theme.ColorTheme
import io.posidon.android.slablauncher.providers.suggestions.SuggestionsManager
import io.posidon.android.slablauncher.ui.home.MainActivity
import io.posidon.android.slablauncher.ui.home.acrylicBlur
import io.posidon.android.slablauncher.ui.view.SeeThroughView

class SuggestedAppsViewHolder(
    itemView: View,
    val activity: MainActivity
) : RecyclerView.ViewHolder(itemView) {

    companion object {
        const val COLUMNS = 3
    }

    val card = itemView.findViewById<CardView>(R.id.card)!!

    val suggestionsAdapter = SuggestionsAdapter(activity)
    val recycler = itemView.findViewById<RecyclerView>(R.id.recycler)!!.apply {
        layoutManager = GridLayoutManager(context, COLUMNS, RecyclerView.VERTICAL, false)
        adapter = suggestionsAdapter
    }

    val allAppsButton = itemView.findViewById<TextView>(R.id.all_apps_button)

    val blurBG = itemView.findViewById<SeeThroughView>(R.id.blur_bg)!!.apply {
        viewTreeObserver.addOnPreDrawListener {
            invalidate()
            true
        }
    }

    fun onBind(suggestionsTodayItem: SuggestionsTodayItem) {
        blurBG.drawable = acrylicBlur?.smoothBlurDrawable
        blurBG.offset = 1f
        activity.setOnPageScrollListener(SuggestedAppsViewHolder::class.simpleName!!) { blurBG.offset = it }

        card.setCardBackgroundColor(ColorTheme.cardBG)

        allAppsButton.backgroundTintList = ColorStateList.valueOf(ColorTheme.uiTitle)
        allAppsButton.setTextColor(ColorTheme.uiBG)

        val suggestions = SuggestionsManager.getSuggestions()
        if (suggestions.isEmpty()) {
            card.isVisible = false
        } else {
            card.isVisible = true
            suggestionsAdapter.updateItems(suggestions)
        }

        allAppsButton.setOnClickListener {
            suggestionsTodayItem.openAllApps()
        }
    }
}