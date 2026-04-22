package com.example.neoncalendarwidget

import android.content.Context
import android.provider.CalendarContract
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class CalendarEventUi(
    val eventId: Long,
    val startMillis: Long,
    val title: String,
    val timeLabel: String
)

object CalendarRepository {

    fun todayHeader(): String {
        val now = LocalDate.now()
        return now.format(DateTimeFormatter.ofPattern("EEEE, dd MMM"))
    }

    fun getTodayEvents(context: Context): List<CalendarEventUi> {
        val zone = ZoneId.systemDefault()
        val start = LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli()
        val end   = LocalDate.now(zone).plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon()
            .appendPath(start.toString())
            .appendPath(end.toString())
            .build()

        val projection = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.ALL_DAY
        )

        // FIX: The Instances API returns events that OVERLAP the window, so a
        // multi-day/all-day event that started yesterday (UTC midnight < local
        // midnight) would otherwise bleed in. BEGIN >= start ensures only events
        // that start today in local time are included.
        val selection = "${CalendarContract.Instances.BEGIN} >= ?"
        val selectionArgs = arrayOf(start.toString())
        val sortOrder = "${CalendarContract.Instances.BEGIN} ASC"

        val results = mutableListOf<CalendarEventUi>()

        context.contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
            val eventIdIndex = cursor.getColumnIndexOrThrow(CalendarContract.Instances.EVENT_ID)
            val titleIndex   = cursor.getColumnIndexOrThrow(CalendarContract.Instances.TITLE)
            val startIndex   = cursor.getColumnIndexOrThrow(CalendarContract.Instances.BEGIN)
            val allDayIndex  = cursor.getColumnIndexOrThrow(CalendarContract.Instances.ALL_DAY)

            while (cursor.moveToNext()) {
                val eventId     = cursor.getLong(eventIdIndex)
                val title       = cursor.getString(titleIndex)?.takeIf { it.isNotBlank() } ?: "Untitled event"
                val startMillis = cursor.getLong(startIndex)
                val allDay      = cursor.getInt(allDayIndex) == 1

                val timeLabel = if (allDay) {
                    "All day"
                } else {
                    val instant   = Instant.ofEpochMilli(startMillis)
                    val localTime = instant.atZone(zone).toLocalTime()
                    localTime.format(DateTimeFormatter.ofPattern("h:mm a"))
                }

                // FIX: removed the hard cap of 4 — all today's events are returned.
                // The ListView in the widget handles display and scrolling.
                results.add(
                    CalendarEventUi(
                        eventId     = eventId,
                        startMillis = startMillis,
                        title       = title,
                        timeLabel   = timeLabel
                    )
                )
            }
        }

        return results
    }
}
