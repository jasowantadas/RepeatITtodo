package com.example.neoncalendarwidget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
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
}

internal fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
    val views = RemoteViews(context.packageName, R.layout.widget_calendar)
    val header = CalendarRepository.todayHeader()

    views.setTextViewText(R.id.headerText, header)

    try {
        val events = CalendarRepository.getTodayEvents(context)

        views.setTextViewText(
            R.id.summaryText,
            if (events.isEmpty()) "no events today" else "${events.size} event(s) today"
        )

        val rows = listOf(
            Pair(R.id.row1, R.id.title1),
            Pair(R.id.row2, R.id.title2),
            Pair(R.id.row3, R.id.title3),
            Pair(R.id.row4, R.id.title4)
        )

        rows.forEachIndexed { index, (rowId, titleId) ->
            if (index < events.size) {
                val event = events[index]
                views.setViewVisibility(rowId, android.view.View.VISIBLE)
                views.setTextViewText(titleId, event.title)
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
