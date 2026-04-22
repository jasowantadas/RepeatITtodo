package com.example.neoncalendarwidget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import java.time.LocalDate

/**
 * Supplies rows to the widget's ListView so the list can scroll.
 * Registered in AndroidManifest.xml with BIND_REMOTEVIEWS permission.
 */
class EventsRemoteViewsService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory =
        EventsRemoteViewsFactory(applicationContext, intent)
}

private class EventsRemoteViewsFactory(
    private val context: Context,
    intent: Intent
) : RemoteViewsService.RemoteViewsFactory {

    private val appWidgetId = intent.getIntExtra(
        AppWidgetManager.EXTRA_APPWIDGET_ID,
        AppWidgetManager.INVALID_APPWIDGET_ID
    )
    private var events: List<CalendarEventUi> = emptyList()

    override fun onCreate() {}

    override fun onDataSetChanged() {
        // Called on the background thread; safe to do IO here.
        events = try {
            CalendarRepository.getTodayEvents(context)
        } catch (e: Exception) {
            emptyList()
        }
    }

    override fun onDestroy() {}

    override fun getCount(): Int = events.size

    override fun getViewAt(position: Int): RemoteViews {
        val event    = events.getOrNull(position)
            ?: return RemoteViews(context.packageName, R.layout.widget_event_row)
        val todayKey = LocalDate.now().toString()
        val checked  = WidgetCheckStateStore.isChecked(
            context, appWidgetId, event.eventId, event.startMillis, todayKey
        )

        return RemoteViews(context.packageName, R.layout.widget_event_row).apply {
            setTextViewText(R.id.titleRow, event.title)
            setTextColor(
                R.id.titleRow,
                if (checked) 0xFF6C6C6C.toInt() else 0xFFE8E8E8.toInt()
            )
            setImageViewResource(
                R.id.statusDotRow,
                if (checked) R.drawable.ic_status_done else R.drawable.ic_status_unfinished
            )

            // Fill-in intent carries the data; CalendarWidgetProvider's
            // onReceive handles the actual toggle via the pending-intent template.
            val fillIntent = Intent().apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                putExtra(EXTRA_EVENT_ID, event.eventId)
                putExtra(EXTRA_INSTANCE_START_MILLIS, event.startMillis)
            }
            setOnClickFillInIntent(R.id.rowContainer, fillIntent)
        }
    }

    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount(): Int = 1
    override fun getItemId(position: Int): Long =
        events.getOrNull(position)?.eventId ?: position.toLong()
    override fun hasStableIds(): Boolean = true
}
