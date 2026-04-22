package com.example.neoncalendarwidget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import java.time.LocalDate

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
            val appWidgetId = intent.getIntExtra(
                AppWidgetManager.EXTRA_APPWIDGET_ID,
                AppWidgetManager.INVALID_APPWIDGET_ID
            )
            val eventId = intent.getLongExtra(EXTRA_EVENT_ID, -1L)
            val instanceStartMillis = intent.getLongExtra(EXTRA_INSTANCE_START_MILLIS, -1L)

            if (
                appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID &&
                eventId != -1L &&
                instanceStartMillis != -1L
            ) {
                val todayKey = LocalDate.now().toString()
                WidgetCheckStateStore.toggle(context, appWidgetId, eventId, instanceStartMillis, todayKey)
                val manager = AppWidgetManager.getInstance(context)
                manager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.eventList)
                updateWidget(context, manager, appWidgetId)
            }
        }
    }
}

internal fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
    val views = RemoteViews(context.packageName, R.layout.widget_calendar)
    views.setTextViewText(R.id.headerText, CalendarRepository.todayHeader())

    try {
        val events = CalendarRepository.getTodayEvents(context)
        views.setTextViewText(
            R.id.summaryText,
            if (events.isEmpty()) "no events today" else "${events.size} event(s) today"
        )
        views.setViewVisibility(R.id.emptyState, if (events.isEmpty()) android.view.View.VISIBLE else android.view.View.GONE)
        views.setViewVisibility(R.id.eventList, if (events.isEmpty()) android.view.View.GONE else android.view.View.VISIBLE)

        val serviceIntent = Intent(context, EventListRemoteViewsService::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
        }
        views.setRemoteAdapter(R.id.eventList, serviceIntent)

        val clickIntentTemplate = Intent(context, CalendarWidgetProvider::class.java).apply {
            action = ACTION_TOGGLE_EVENT
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        }
        val clickPendingIntentTemplate = PendingIntent.getBroadcast(
            context,
            appWidgetId,
            clickIntentTemplate,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        views.setPendingIntentTemplate(R.id.eventList, clickPendingIntentTemplate)

        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.eventList)
    } catch (e: SecurityException) {
        views.setTextViewText(R.id.summaryText, "calendar permission needed")
        views.setTextViewText(R.id.emptyState, "open the app and grant calendar access")
        views.setViewVisibility(R.id.emptyState, android.view.View.VISIBLE)
        views.setViewVisibility(R.id.eventList, android.view.View.GONE)
    } catch (e: Exception) {
        views.setTextViewText(R.id.summaryText, "widget failed to load")
        views.setTextViewText(R.id.emptyState, "tap the app to refresh")
        views.setViewVisibility(R.id.emptyState, android.view.View.VISIBLE)
        views.setViewVisibility(R.id.eventList, android.view.View.GONE)
    }

    appWidgetManager.updateAppWidget(appWidgetId, views)
}

internal const val ACTION_TOGGLE_EVENT = "com.example.neoncalendarwidget.ACTION_TOGGLE_EVENT"
internal const val EXTRA_EVENT_ID = "extra_event_id"
internal const val EXTRA_INSTANCE_START_MILLIS = "extra_instance_start_millis"
