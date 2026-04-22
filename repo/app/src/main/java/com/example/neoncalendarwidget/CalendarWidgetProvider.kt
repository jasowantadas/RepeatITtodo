package com.example.neoncalendarwidget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Intent
import android.content.Context
import android.app.PendingIntent
import android.net.Uri
import android.widget.RemoteViews
import java.time.LocalDate

class CalendarWidgetProvider : AppWidgetProvider() {
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { id -> updateWidget(context, appWidgetManager, id) }
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

            if (appWidgetId != AppWidgetManager.INVALID_APPWIDGET_ID &&
                eventId != -1L &&
                instanceStartMillis != -1L
            ) {
                val todayKey = LocalDate.now().toString()
                WidgetCheckStateStore.toggle(context, appWidgetId, eventId, instanceStartMillis, todayKey)

                // Tell the RemoteViewsFactory to reload its data set, then redraw.
                AppWidgetManager.getInstance(context)
                    .notifyAppWidgetViewDataChanged(appWidgetId, R.id.eventList)
                updateWidget(context, AppWidgetManager.getInstance(context), appWidgetId)
            }
        }
    }
}

internal fun updateWidget(context: Context, appWidgetManager: AppWidgetManager, appWidgetId: Int) {
    val views = RemoteViews(context.packageName, R.layout.widget_calendar)

    // Header
    views.setTextViewText(R.id.headerText, CalendarRepository.todayHeader())

    try {
        // Quick count for the summary line (fetched on the main thread here;
        // the heavy lifting happens inside EventsRemoteViewsFactory.onDataSetChanged).
        val events = CalendarRepository.getTodayEvents(context)
        views.setTextViewText(
            R.id.summaryText,
            if (events.isEmpty()) "no events today" else "${events.size} event(s) today"
        )

        // Wire the ListView to EventsRemoteViewsService.
        // Each widget instance gets a unique data URI so Android doesn't
        // reuse the wrong factory across multiple widget instances.
        val serviceIntent = Intent(context, EventsRemoteViewsService::class.java).apply {
            putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            data = Uri.fromParts("widget", appWidgetId.toString(), null)
        }
        views.setRemoteAdapter(R.id.eventList, serviceIntent)
        views.setEmptyView(R.id.eventList, R.id.emptyState)

        // Pending-intent template: ACTION_TOGGLE_EVENT broadcast.
        // The fill-in intent from each row (see EventsRemoteViewsFactory)
        // supplies EVENT_ID and INSTANCE_START_MILLIS at click time.
        val toggleTemplate = PendingIntent.getBroadcast(
            context,
            appWidgetId,
            Intent(context, CalendarWidgetProvider::class.java).apply {
                action = ACTION_TOGGLE_EVENT
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        )
        views.setPendingIntentTemplate(R.id.eventList, toggleTemplate)

    } catch (e: SecurityException) {
        views.setTextViewText(R.id.summaryText, "calendar permission needed")
        views.setTextViewText(R.id.emptyState, "open the app and grant calendar access")
    } catch (e: Exception) {
        views.setTextViewText(R.id.summaryText, "widget failed to load")
        views.setTextViewText(R.id.emptyState, "tap the app to refresh")
    }

    appWidgetManager.updateAppWidget(appWidgetId, views)
}
