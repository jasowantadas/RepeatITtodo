package com.example.neoncalendarwidget

import android.content.Context

object WidgetCheckStateStore {
    private const val PREFS_NAME = "widget_checks"

    private fun key(widgetId: Int, eventId: Long, startMillis: Long, date: String): String =
        "${widgetId}_${eventId}_${startMillis}_$date"

    fun isChecked(context: Context, widgetId: Int, eventId: Long, startMillis: Long, date: String): Boolean {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(key(widgetId, eventId, startMillis, date), false)
    }

    fun toggle(context: Context, widgetId: Int, eventId: Long, startMillis: Long, date: String): Boolean {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val prefKey = key(widgetId, eventId, startMillis, date)
        val newValue = !prefs.getBoolean(prefKey, false)
        prefs.edit().putBoolean(prefKey, newValue).apply()
        return newValue
    }
}
