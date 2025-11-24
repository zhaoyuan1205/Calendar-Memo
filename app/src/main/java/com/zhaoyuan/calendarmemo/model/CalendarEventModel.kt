// CalendarEventModel.kt
package com.zhaoyuan.calendarmemo.model

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.CalendarContract
import java.util.Calendar

data class CalendarEvent(
    val id: Long,
    val title: String,
    val startTime: Long,
    val formattedStartTime: String = "",
    val hasReminder: Boolean = false,
    val reminderMinutes: List<Int> = emptyList()
)

class CalendarEventModel {

    fun readCalendarEvents(context: Context): List<CalendarEvent> {
        val contentResolver = context.contentResolver
        val events = mutableListOf<CalendarEvent>()

        val startCalendar = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -15) }
        val endCalendar = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, 15) }

        val uri: Uri = CalendarContract.Events.CONTENT_URI
        val projection = arrayOf(
            CalendarContract.Events._ID,
            CalendarContract.Events.TITLE,
            CalendarContract.Events.DTSTART,
            CalendarContract.Events.HAS_ALARM,
        )

        val selection = "${CalendarContract.Events.DTSTART} >= ? AND ${CalendarContract.Events.DTSTART} <= ? AND ${CalendarContract.Events.DELETED} = 0"
        val selectionArgs = arrayOf(
            startCalendar.timeInMillis.toString(),
            endCalendar.timeInMillis.toString()
        )

        val cursor = contentResolver.query(
            uri,
            projection,
            selection,
            selectionArgs,
            "${CalendarContract.Events.DTSTART} ASC"
        )

        cursor?.use { c ->
            while (c.moveToNext()) {
                val id = c.getLong(c.getColumnIndexOrThrow(CalendarContract.Events._ID))
                val title = c.getString(c.getColumnIndexOrThrow(CalendarContract.Events.TITLE)) ?: "无标题"
                val start = c.getLong(c.getColumnIndexOrThrow(CalendarContract.Events.DTSTART))
                val hasAlarm = c.getInt(c.getColumnIndexOrThrow(CalendarContract.Events.HAS_ALARM)) == 1

                val reminderMinutes = mutableListOf<Int>()
                if (hasAlarm) {
                    val reminderCursor = contentResolver.query(
                        CalendarContract.Reminders.CONTENT_URI,
                        arrayOf(CalendarContract.Reminders.MINUTES),
                        "${CalendarContract.Reminders.EVENT_ID} = ?",
                        arrayOf(id.toString()),
                        null
                    )
                    reminderCursor?.use { rc ->
                        while (rc.moveToNext()) {
                            val minutes = rc.getInt(rc.getColumnIndexOrThrow(CalendarContract.Reminders.MINUTES))
                            reminderMinutes.add(minutes)
                        }
                    }
                }

                events.add(
                    CalendarEvent(
                        id = id,
                        title = title,
                        startTime = start,
                        hasReminder = hasAlarm,
                        reminderMinutes = reminderMinutes
                    )
                )
            }
        }

        return events
    }

    fun deleteEvent(context: Context, eventId: Long): Boolean {
        return try {
            val uri = CalendarContract.Events.CONTENT_URI
            val rows = context.contentResolver.delete(
                uri,
                "${CalendarContract.Events._ID} = ?",
                arrayOf(eventId.toString())
            )
            rows > 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun updateEvent(context: Context, eventId: Long, newTitle: String, newStartTime: Long): Boolean {
        return try {
            val uri = CalendarContract.Events.CONTENT_URI
            val values = ContentValues().apply {
                put(CalendarContract.Events.TITLE, newTitle)
                put(CalendarContract.Events.DTSTART, newStartTime)
                put(CalendarContract.Events.DTEND, newStartTime + 3600000) // 假设事件持续一个小时
            }
            val rowsUpdated = context.contentResolver.update(
                uri,
                values,
                "${CalendarContract.Events._ID} = ?",
                arrayOf(eventId.toString())
            )
            rowsUpdated > 0
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun addEventsFromContent(context: Context, contentString: String?): List<Long> {
        val createdIds = mutableListOf<Long>()

        try {
            // 1. 解析 JSON 内容
            val jsonArray = org.json.JSONArray(contentString)

            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val title = obj.getString("title")
                val timeStr = obj.getString("time") // "2025-11-20 09:00"

                // 2. 将 "yyyy-MM-dd HH:mm" 解析为时间戳
                val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
                val localDateTime = java.time.LocalDateTime.parse(timeStr, formatter)
                val millis = localDateTime
                    .atZone(java.time.ZoneId.of("Asia/Shanghai"))
                    .toInstant()
                    .toEpochMilli()

                // 3. 插入日历事件
                val values = ContentValues().apply {
                    put(CalendarContract.Events.DTSTART, millis)
                    put(CalendarContract.Events.DTEND, millis + 60 * 60 * 1000) // 默认持续 1 小时
                    put(CalendarContract.Events.TITLE, title)
                    put(CalendarContract.Events.CALENDAR_ID, 1) // 默认使用主日历
                    put(CalendarContract.Events.EVENT_TIMEZONE, "Asia/Shanghai")
                }

                val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)

                if (uri != null) {
                    val eventId = uri.lastPathSegment?.toLongOrNull()
                    if (eventId != null) {
                        createdIds.add(eventId)

                        // 4. 默认添加一个提醒：提前 5 分钟
                        val reminderValues = ContentValues().apply {
                            put(CalendarContract.Reminders.EVENT_ID, eventId)
                            put(CalendarContract.Reminders.MINUTES, 5)
                            put(CalendarContract.Reminders.METHOD, CalendarContract.Reminders.METHOD_ALERT)
                        }
                        context.contentResolver.insert(CalendarContract.Reminders.CONTENT_URI, reminderValues)
                    }
                }
            }

        } catch (e: Exception) {
            e.printStackTrace()
        }

        return createdIds
    }

    fun createEvent(context: Context, title: String, startTime: Long): Long? {
        return try {
            val values = ContentValues().apply {
                put(CalendarContract.Events.DTSTART, startTime)
                put(CalendarContract.Events.DTEND, startTime + 60 * 60 * 1000)
                put(CalendarContract.Events.TITLE, title)
                put(CalendarContract.Events.CALENDAR_ID, 1)
                put(CalendarContract.Events.EVENT_TIMEZONE, "Asia/Shanghai")
            }
            val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
            uri?.lastPathSegment?.toLongOrNull()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

}
