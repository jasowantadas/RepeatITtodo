# Neon Calendar Widget

A native Android home-screen widget that shows **today's calendar events only** in a dark neon / Nothing-style layout.

## What it does
- Reads calendar events for today only
- Shows up to 4 events
- Displays an empty state when there are no events
- No KWGT required

## Build notes
- Open this folder in Android Studio
- Grant **Calendar** permission after installing
- Add the widget from the Android widget picker

## Important
Android app widgets use `RemoteViews`, and calendar data comes from the Calendar Provider. See Android docs:
- App widgets / RemoteViews: https://developer.android.com/reference/android/appwidget/AppWidgetHostView
- Calendar Provider: https://developer.android.com/identity/providers/calendar-provider
