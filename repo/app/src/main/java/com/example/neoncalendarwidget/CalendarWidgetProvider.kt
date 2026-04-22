package com.example.neoncalendarwidget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Intent
import android.content.Context
import android.app.PendingIntent
import android.graphics.Paint
import java.time.LocalDate
import android.widget.RemoteViews

class CalendarWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { id ->
            updateWidget(context, appWidgetManager, id)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)

        if (intent.action == ACTION_TOGGLE_EVENT) {
            val appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
            val eventId = intent.getLongExtra(EXTRA_EVENT_ID, -1L)
            val instanceStartMillis = intent.getLongExtra(EXTRA_INSTANCE_START_MILLIS, -1L)

            if (
                appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID &&
                eventId != -1L &&
                instanceStartMillis != -1L
            ) {
                val todayKey = LocalDate.now().toString()
                WidgetCheckStateStore.toggle(context, appWidgetId, eventId, instanceStartMillis, todayKey)
                updateWidget(context, AppWidgetManager.getInstance(context), appWidgetId)
            }
        }
    }
}

internal fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
    val views = RemoteViews(context.packageName, R.layout.widget_calendar)
    val header = CalendarRepository.todayHeader()
    val todayKey = LocalDate.now().toString()

    views.setTextViewText(R.id.headerText, header)

    try {
        val events = CalendarRepository.getTodayEvents(context)

        views.setTextViewText(
            R.id.summaryText,
            if (events.isEmpty()) "no events today" else "${events.size} event(s) today"
        )

        val rows = listOf(
            Triple(R.id.row1, R.id.title1, R.id.checkbox1),
            Triple(R.id.row2, R.id.title2, R.id.checkbox2),
            Triple(R.id.row3, R.id.title3, R.id.checkbox3),
            Triple(R.id.row4, R.id.title4, R.id.checkbox4)
        )

        rows.forEachIndexed { index, (rowId, titleId, checkboxId) ->
            if (index < events.size) {
                val event = events[index]
                val checked = WidgetCheckStateStore.isChecked(
                    context,
                    appWidgetId,
                    event.eventId,
                    event.startMillis,
                    todayKey
                )
                val toggleIntent = Intent(context, CalendarWidgetProvider::class.java).apply {
                    action = ACTION_TOGGLE_EVENT
                    putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                    putExtra(EXTRA_EVENT_ID, event.eventId)
                    putExtra(EXTRA_INSTANCE_START_MILLIS, event.startMillis)
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    appWidgetId * 10 + index,
                    toggleIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                views.setViewVisibility(rowId, android.view.View.VISIBLE)
                views.setTextViewText(titleId, event.title)
                views.setTextColor(
                    titleId,
                    if (checked) 0xFF8E8E8E.toInt() else 0xFFE8E8E8.toInt()
                )
                views.setInt(
                    titleId,
                    "setPaintFlags",
                    Paint.ANTI_ALIAS_FLAG
                )
                views.setImageViewResource(
                    checkboxId,
                    if (checked) R.drawable.ic_status_done else R.drawable.ic_status_unfinished
                )
                views.setOnClickPendingIntent(rowId, pendingIntent)
                views.setOnClickPendingIntent(checkboxId, pendingIntent)
            } else {
                views.setViewVisibility(rowId, android.view.View.GONE)
            }
        }

        views.setViewVisibility(R.id.emptyState, if (events.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE)
    } catch (e: SecurityException) {
        views.setTextViewText(R.id.summaryText, "calendar permission needed")
        views.setTextViewText(R.id.emptyState, "open the app and grant calendar access")
        views.setViewVisibility(R.id.emptyState, android.view.View.VISIBLE)
        listOf(R.id.row1, R.id.row2, R.id.row3, R.id.row4).forEach {
            views.setViewVisibility(it, android.view.View.GONE)
        }
    } catch (e: Exception) {
        views.setTextViewText(R.id.summaryText, "widget failed to load")
        views.setTextViewText(R.id.emptyState, "tap the app to refresh")
        views.setViewVisibility(R.id.emptyState, android.view.View.VISIBLE)
        listOf(R.id.row1, R.id.row2, R.id.row3, R.id.row4).forEach {
            views.setViewVisibility(it, android.view.View.GONE)
        }
    }

    appWidgetManager.updateAppWidget(appWidgetId, views)
}

private object WidgetCheckStateStore {
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

private const val ACTION_TOGGLE_EVENT = "com.example.neoncalendarwidget.ACTION_TOGGLE_EVENT"
private const val EXTRA_EVENT_ID = "extra_event_id"
private const val EXTRA_INSTANCE_START_MILLIS = "extra_instance_start_millis"
