package io.posidon.android.slablauncher.ui.home.today.viewHolders.apps

import android.view.View
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.posidon.android.slablauncher.R
import io.posidon.android.slablauncher.providers.color.theme.ColorTheme
import io.posidon.android.slablauncher.ui.home.MainActivity
import io.posidon.android.slablauncher.ui.home.pinned.acrylicBlur
import io.posidon.android.slablauncher.util.view.SeeThroughView

class SuggestedAppsViewHolder(
    itemView: View,
    activity: MainActivity,
) : RecyclerView.ViewHolder(itemView) {

    companion object {
        const val COLUMNS = 2
    }

    val card = itemView.findViewById<CardView>(R.id.card)!!

    val suggestionsAdapter = SuggestedAppsAdapter(activity)
    val recycler = itemView.findViewById<RecyclerView>(R.id.recycler)!!.apply {
        layoutManager = GridLayoutManager(context, COLUMNS, RecyclerView.VERTICAL, false)
        adapter = suggestionsAdapter
    }

    val blurBG = itemView.findViewById<SeeThroughView>(R.id.blur_bg)!!.apply {
        viewTreeObserver.addOnPreDrawListener {
            invalidate()
            true
        }
    }

    fun onBind(
        suggestedAppsTodayItem: SuggestedAppsTodayItem,
        activity: MainActivity,
    ) {
        blurBG.drawable = acrylicBlur?.smoothBlurDrawable
        blurBG.offset = 1f
        activity.setOnPageScrollListener(SuggestedAppsViewHolder::class.simpleName!!) { blurBG.offset = it }

        card.setCardBackgroundColor(ColorTheme.cardBG)

        val suggestions = suggestedAppsTodayItem.suggestions
        suggestionsAdapter.updateItems(suggestions)

        suggestionsAdapter.openAllApps = suggestedAppsTodayItem.openAllApps
    }
}