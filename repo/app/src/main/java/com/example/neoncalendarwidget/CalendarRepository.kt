package com.example.neoncalendarwidget

import android.content.Context
import android.provider.CalendarContract
import android.text.format.Time
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.TimeZone

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
        val today = LocalDate.now(zone)
        val start = today.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = today.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val offsetSeconds = TimeZone.getDefault().getOffset(start).div(1000)
        val todayJulianDay = Time.getJulianDay(start, offsetSeconds)

        // Use Instances instead of Events — correctly handles recurring events,
        // all-day events, and timezone boundaries
        val uri = CalendarContract.Instances.CONTENT_URI.buildUpon()
            .appendPath(start.toString())
            .appendPath(end.toString())
            .build()

        val projection = arrayOf(
            CalendarContract.Instances.EVENT_ID,
            CalendarContract.Instances.TITLE,
            CalendarContract.Instances.BEGIN,
            CalendarContract.Instances.START_DAY,
            CalendarContract.Instances.ALL_DAY
        )

        val sortOrder = "${CalendarContract.Instances.BEGIN} ASC"
        val results = mutableListOf<CalendarEventUi>()
        val selection = "${CalendarContract.Instances.START_DAY} = ?"
        val selectionArgs = arrayOf(todayJulianDay.toString())

        context.contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
            val eventIdIndex = cursor.getColumnIndexOrThrow(CalendarContract.Instances.EVENT_ID)
            val titleIndex = cursor.getColumnIndexOrThrow(CalendarContract.Instances.TITLE)
            val startIndex = cursor.getColumnIndexOrThrow(CalendarContract.Instances.BEGIN)
            val allDayIndex = cursor.getColumnIndexOrThrow(CalendarContract.Instances.ALL_DAY)

            while (cursor.moveToNext()) {
                val eventId = cursor.getLong(eventIdIndex)
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

                results.add(
                    CalendarEventUi(
                        eventId = eventId,
                        startMillis = startMillis,
                        title = title,
                        timeLabel = timeLabel
                    )
                )
            }
        }

        return results
    }
}
