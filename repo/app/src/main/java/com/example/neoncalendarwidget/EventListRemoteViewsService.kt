package com.example.neoncalendarwidget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.util.TypedValue
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import java.time.LocalDate

class EventListRemoteViewsService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return EventListRemoteViewsFactory(
            applicationContext,
            intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        )
    }
}

private class EventListRemoteViewsFactory(
    private val context: android.content.Context,
    private val appWidgetId: Int
) : RemoteViewsService.RemoteViewsFactory {

    private var events: List<CalendarEventUi> = emptyList()

    override fun onCreate() = Unit

    override fun onDataSetChanged() {
        events = try {
            CalendarRepository.getTodayEvents(context)
        } catch (_: Exception) {
            emptyList()
        }
    }

    override fun onDestroy() {
        events = emptyList()
    }

    override fun getCount(): Int = events.size

    override fun getViewAt(position: Int): RemoteViews {
        if (position !in events.indices) {
            return RemoteViews(context.packageName, R.layout.widget_event_row)
        }

        val event = events[position]
        val checked = WidgetCheckStateStore.isChecked(
            context,
            appWidgetId,
            event.eventId,
            event.startMillis,
            LocalDate.now().toString()
        )

        return RemoteViews(context.packageName, R.layout.widget_event_row).apply {
            setTextViewText(R.id.itemTitle, event.title)
            setTextColor(R.id.itemTitle, if (checked) 0xFF6C6C6C.toInt() else 0xFFE8E8E8.toInt())
            setTextViewTextSize(R.id.itemTitle, TypedValue.COMPLEX_UNIT_SP, if (checked) 12.5f else 14f)
            setImageViewResource(
                R.id.itemStatusDot,
                if (checked) R.drawable.ic_status_done else R.drawable.ic_status_unfinished
            )

            val fillInIntent = Intent().apply {
                putExtra(EXTRA_EVENT_ID, event.eventId)
                putExtra(EXTRA_INSTANCE_START_MILLIS, event.startMillis)
            }
            setOnClickFillInIntent(R.id.itemRoot, fillInIntent)
            setOnClickFillInIntent(R.id.itemStatusDot, fillInIntent)
        }
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long {
        if (position !in events.indices) return position.toLong()
        val e = events[position]
        return (e.eventId * 31L) + e.startMillis
    }

    override fun hasStableIds(): Boolean = true
}
