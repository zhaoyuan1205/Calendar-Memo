package com.zhaoyuan.calendarmemo.data.repository

import android.content.Context
import com.zhaoyuan.calendarmemo.model.CalendarEvent
import com.zhaoyuan.calendarmemo.model.CalendarEventModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class CalendarRepository(
    private val calendarEventModel: CalendarEventModel = CalendarEventModel()
) {

    suspend fun getEvents(context: Context): List<CalendarEvent> = withContext(Dispatchers.IO) {
        calendarEventModel.readCalendarEvents(context)
    }

    suspend fun deleteEvent(context: Context, eventId: Long): Boolean = withContext(Dispatchers.IO) {
        calendarEventModel.deleteEvent(context, eventId)
    }

    suspend fun updateEvent(
        context: Context,
        eventId: Long,
        newTitle: String,
        newStartTime: Long
    ): Boolean = withContext(Dispatchers.IO) {
        calendarEventModel.updateEvent(context, eventId, newTitle, newStartTime)
    }

    suspend fun addEventsFromContent(context: Context, contentString: String?): List<Long> =
        withContext(Dispatchers.IO) {
            calendarEventModel.addEventsFromContent(context, contentString)
        }

    suspend fun createEvent(context: Context, title: String, startTime: Long): Long? =
        withContext(Dispatchers.IO) {
            calendarEventModel.createEvent(context, title, startTime)
        }
}

