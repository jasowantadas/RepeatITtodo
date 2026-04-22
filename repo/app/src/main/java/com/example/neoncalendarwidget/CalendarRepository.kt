package com.example.neoncalendarwidget

import android.content.Context
import android.provider.CalendarContract
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

data class CalendarEventUi(
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
        val end = LocalDate.now(zone).plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

        // Use Instances instead of Events — correctly handles recurring events,
        // all-day events, and timezone boundaries
        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon()
            .appendPath(start.toString())
            .appendPath(end.toString())
            .build()

        val projection = arrayOf(
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.ALL_DAY
        )

        val sortOrder = "${CalendarContract.Instances.BEGIN} ASC"
        val results = mutableListOf<CalendarEventUi>()

        context.contentResolver.query(uri, projection, null, null, sortOrder)?.use { cursor ->
            val titleIndex = cursor.getColumnIndexOrThrow(CalendarContract.Instances.TITLE)
            val startIndex = cursor.getColumnIndexOrThrow(CalendarContract.Instances.BEGIN)
            val allDayIndex = cursor.getColumnIndexOrThrow(CalendarContract.Instances.ALL_DAY)

            while (cursor.moveToNext()) {
                val title = cursor.getString(titleIndex)?.takeIf { it.isNotBlank() } ?: "Untitled event"
                val startMillis = cursor.getLong(startIndex)
                val allDay = cursor.getInt(allDayIndex) == 1

                val timeLabel = if (allDay) {
                    "All day"
                } else {
                    val instant = Instant.ofEpochMilli(startMillis)
                    val localTime = instant.atZone(zone).toLocalTime()
                    localTime.format(DateTimeFormatter.ofPattern("h:mm a"))
                }

                results.add(CalendarEventUi(title = title, timeLabel = timeLabel))
                if (results.size >= 4) break
            }
        }

        return results
    }
}
