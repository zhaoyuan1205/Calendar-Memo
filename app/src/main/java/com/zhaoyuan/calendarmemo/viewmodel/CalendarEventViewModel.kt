package com.zhaoyuan.calendarmemo.viewmodel

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhaoyuan.calendarmemo.model.CalendarEvent
import com.zhaoyuan.calendarmemo.model.CalendarEventModel
import com.zhaoyuan.calendarmemo.widget.TodayEventsWidgetProvider
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CalendarEventViewModel : ViewModel() {

    private val calendarEventModel = CalendarEventModel()

    // 事件列表
    private val _events = MutableLiveData<List<CalendarEvent>>()
    val events: LiveData<List<CalendarEvent>> get() = _events

    // 删除状态
    private val _isEventDeleted = MutableLiveData<Boolean>()
    val isEventDeleted: LiveData<Boolean> get() = _isEventDeleted

    // 编辑状态
    private val _isEventUpdated = MutableLiveData<Boolean>()
    val isEventUpdated: LiveData<Boolean> get() = _isEventUpdated

    // 加载事件
    fun loadEvents(context: Context) {
        viewModelScope.launch {
            val eventsList = calendarEventModel.readCalendarEvents(context).map { event ->
                event.copy(formattedStartTime = formatDateTime(event.startTime)) // 格式化时间
            }
            _events.value = eventsList
            TodayEventsWidgetProvider.notifyDataChanged(context)
        }
    }



    // 删除事件
    fun deleteEvent(context: Context, event: CalendarEvent) {
        viewModelScope.launch {
            val success = calendarEventModel.deleteEvent(context, event.id)
            _isEventDeleted.value = success
            if (success) {
                // 事件删除后更新事件列表
                _events.value = _events.value?.filter { it.id != event.id }
                TodayEventsWidgetProvider.notifyDataChanged(context)
            }
        }
    }

    // 更新事件
    // 更新事件
    fun updateEvent(context: Context, event: CalendarEvent) {
        viewModelScope.launch {
            val success = calendarEventModel.updateEvent(context, event.id, event.title, event.startTime)
            _isEventUpdated.value = success
            if (success) {
                // 更新事件列表
                _events.value = _events.value?.map {
                    if (it.id == event.id) {
                        it.copy(title = event.title, startTime = event.startTime)
                    } else {
                        it
                    }
                }
                TodayEventsWidgetProvider.notifyDataChanged(context)
            }
        }
    }

    // 添加事件（来自 AI 返回的 JSON 字符串）
    fun addEventsFromContent(context: Context, contentString: String?) {
        viewModelScope.launch {
            val createdIds = calendarEventModel.addEventsFromContent(context, contentString)

            // 如果有新增事件，则重新加载一次事件列表
            if (createdIds.isNotEmpty()) {
                loadEvents(context)
            }
        }
    }

    fun addEvent(context: Context, event: CalendarEvent) {
        viewModelScope.launch {
            val newId = calendarEventModel.createEvent(context, event.title, event.startTime)
            if (newId != null) {
                loadEvents(context)
            }
        }
    }



    fun formatDateTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy年MM月dd日 HH:mm", Locale.getDefault()) // 中文格式
        return sdf.format(Date(timestamp))
    }
}