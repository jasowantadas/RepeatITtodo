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
    val events = CalendarRepository.getTodayEvents(context)
    val header = CalendarRepository.todayHeader()

    views.setTextViewText(R.id.headerText, header)
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

    if (events.isEmpty()) {
        views.setViewVisibility(R.id.emptyState, android.view.View.VISIBLE)
    } else {
        views.setViewVisibility(R.id.emptyState, android.view.View.GONE)
    }

    appWidgetManager.updateAppWidget(appWidgetId, views)
}
