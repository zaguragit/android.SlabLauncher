package io.posidon.android.slablauncher.providers.personality

import android.app.AlarmManager
import android.content.Context
import android.icu.util.Calendar
import androidx.core.view.isVisible
import io.posidon.android.slablauncher.R
import io.posidon.android.slablauncher.util.storage.DoBlurSetting.doBlur
import io.posidon.android.slablauncher.util.storage.GreetingSetting.getDefaultGreeting
import io.posidon.android.slablauncher.util.storage.Settings
import kotlin.math.abs
import kotlin.math.min

object Statement {
    fun get(context: Context, time: Calendar, settings: Settings): String {
        val nextAlarm = context.getSystemService(AlarmManager::class.java).nextAlarmClock
        val hour = time[Calendar.HOUR_OF_DAY]
        if (nextAlarm != null) {
            val c = Calendar.getInstance()
            c.timeInMillis = nextAlarm.triggerTime
            val alarmHour = c[Calendar.HOUR_OF_DAY]
            val diff = arrayOf(alarmHour - hour, alarmHour - hour + 24).minBy(::abs)
            if (diff in 0..3) {
                return context.getString(
                    R.string.next_alarm_at_x,
                    "$alarmHour:${c[Calendar.MINUTE].toString().padStart(2, '0')}", diff
                )
            }
        }
        val dayOfYear = time[Calendar.DAY_OF_YEAR]
        return when {
            dayOfYear in arrayOf(0, 355) -> context.getString(R.string.new_year)
            hour in 5..9 -> context.getString(R.string.good_morning)
            hour in 21..23 ||
            hour in 0..4 -> context.getString(R.string.good_night)
            else -> settings.getDefaultGreeting(context)
        }
    }
}