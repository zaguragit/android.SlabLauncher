package io.posidon.android.slablauncher.providers.personality

import android.content.Context
import android.icu.util.Calendar
import io.posidon.android.slablauncher.R
import io.posidon.android.slablauncher.util.storage.DoBlurSetting.doBlur
import io.posidon.android.slablauncher.util.storage.GreetingSetting.getDefaultGreeting
import io.posidon.android.slablauncher.util.storage.Settings

object Statement {
    fun get(context: Context, time: Calendar, settings: Settings): String {
        val dayOfYear = time[Calendar.DAY_OF_YEAR]
        val hour = time[Calendar.HOUR_OF_DAY]
        return when {
            dayOfYear in arrayOf(0, 355) -> context.getString(R.string.new_year)
            hour in 5..9 -> context.getString(R.string.good_morning)
            hour in 21..23 ||
            hour in 0..4 -> context.getString(R.string.good_night)
            else -> settings.getDefaultGreeting(context)
        }
    }
}