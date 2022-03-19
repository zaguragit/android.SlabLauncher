package io.posidon.android.slablauncher.providers.personality

import android.content.Context
import android.icu.util.Calendar
import io.posidon.android.slablauncher.R

object Statement {
    fun get(context: Context, time: Calendar): String {
        return when {
            time[Calendar.HOUR_OF_DAY] in 5..9 -> context.getString(R.string.good_morning)
            time[Calendar.HOUR_OF_DAY] in 21..23 ||
            time[Calendar.HOUR_OF_DAY] in 0..4 -> context.getString(R.string.good_night)
            else -> context.getString(R.string.good_day)
        }
    }
}