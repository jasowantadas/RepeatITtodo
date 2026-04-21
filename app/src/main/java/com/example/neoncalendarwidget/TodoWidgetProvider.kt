package com.example.neoncalendarwidget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.widget.RemoteViews

class TodoWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { id ->
            updateWidget(context, appWidgetManager, id)
        }
    }

    companion object {
        fun refreshAll(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(
                android.content.ComponentName(context, TodoWidgetProvider::class.java)
            )
            ids.forEach { id -> updateWidget(context, manager, id) }
        }
    }
}

internal fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
    val views = RemoteViews(context.packageName, R.layout.widget_calendar)
    val tasks = TodoStore.getTasks(context)

    views.setTextViewText(R.id.headerText, "TODAY'S TODOS")
    views.setTextViewText(
        R.id.summaryText,
        if (tasks.isEmpty()) "No tasks yet" else "${tasks.count { !it.done }} active task(s)"
    )

    val rows = listOf(
        Triple(R.id.row1, R.id.title1, R.id.time1),
        Triple(R.id.row2, R.id.title2, R.id.time2),
        Triple(R.id.row3, R.id.title3, R.id.time3),
        Triple(R.id.row4, R.id.title4, R.id.time4)
    )
    val checks = listOf(R.id.check1, R.id.check2, R.id.check3, R.id.check4)

    rows.forEachIndexed { index, (rowId, titleId, timeId) ->
        if (index < tasks.size) {
            val task = tasks[index]
            views.setViewVisibility(rowId, android.view.View.VISIBLE)
            views.setTextViewText(titleId, task.title)
            views.setTextViewText(timeId, if (task.done) "Done" else "Tap in app to check off")
            views.setTextViewText(checks[index], if (task.done) "☑" else "☐")
        } else {
            views.setViewVisibility(rowId, android.view.View.GONE)
        }
    }

    if (tasks.isEmpty()) {
        views.setViewVisibility(R.id.emptyState, android.view.View.VISIBLE)
    } else {
        views.setViewVisibility(R.id.emptyState, android.view.View.GONE)
    }

    appWidgetManager.updateAppWidget(appWidgetId, views)
}
