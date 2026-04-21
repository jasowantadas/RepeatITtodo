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
        val resolver = context.contentResolver
        val zone = ZoneId.systemDefault()
        val start = LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli()
        val end = LocalDate.now(zone).plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()

        val projection = arrayOf(
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.ALL_DAY
        )

        val selection = "((${CalendarContract.Events.DTSTART} < ?) AND (${CalendarContract.Events.DTEND} > ?))"
        val selectionArgs = arrayOf(end.toString(), start.toString())
        val sortOrder = "${CalendarContract.Events.DTSTART} ASC"

        val results = mutableListOf<CalendarEventUi>()

        resolver.query(
            CalendarContract.Events.CONTENT_URI,
            projection,
            selection,
            selectionArgs,
            sortOrder
        )?.use { cursor ->
            val titleIndex = cursor.getColumnIndexOrThrow(CalendarContract.Events.TITLE)
            val startIndex = cursor.getColumnIndexOrThrow(CalendarContract.Events.DTSTART)
            val allDayIndex = cursor.getColumnIndexOrThrow(CalendarContract.Events.ALL_DAY)

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
