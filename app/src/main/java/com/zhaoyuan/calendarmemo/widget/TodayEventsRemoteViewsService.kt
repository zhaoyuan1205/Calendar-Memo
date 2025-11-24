package com.zhaoyuan.calendarmemo.widget

import android.content.Context
import android.content.Intent
import android.os.Binder
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.zhaoyuan.calendarmemo.R
import com.zhaoyuan.calendarmemo.model.CalendarEvent
import com.zhaoyuan.calendarmemo.model.CalendarEventModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class TodayEventsRemoteViewsService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return TodayEventsRemoteViewsFactory(applicationContext)
    }
}

private class TodayEventsRemoteViewsFactory(
    private val context: Context
) : RemoteViewsService.RemoteViewsFactory {

    private val items = mutableListOf<CalendarEvent>()
    private val calendarEventModel = CalendarEventModel()
    private val timeFormatter = SimpleDateFormat("HH:mm", Locale.getDefault())

    override fun onCreate() = Unit

    override fun onDataSetChanged() {
        val identityToken = Binder.clearCallingIdentity()
        try {
            val allEvents = calendarEventModel.readCalendarEvents(context)
            val todayStart = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val todayEnd = todayStart + 24 * 60 * 60 * 1000
            val todayEvents = allEvents.filter { event ->
                event.startTime in todayStart until todayEnd
            }
            items.clear()
            items.addAll(todayEvents)
        } finally {
            Binder.restoreCallingIdentity(identityToken)
        }
    }

    override fun onDestroy() {
        items.clear()
    }

    override fun getCount(): Int = items.size

    override fun getViewAt(position: Int): RemoteViews {
        val event = items[position]
        return RemoteViews(context.packageName, R.layout.widget_today_event_item).apply {
            setTextViewText(R.id.widget_event_title, event.title)
            setTextViewText(R.id.widget_event_time, timeFormatter.format(event.startTime))
        }
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = items[position].id

    override fun hasStableIds(): Boolean = true
}

