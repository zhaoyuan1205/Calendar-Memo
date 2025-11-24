package com.zhaoyuan.calendarmemo.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.zhaoyuan.calendarmemo.data.repository.AiRepository
import com.zhaoyuan.calendarmemo.data.repository.CalendarRepository
import com.zhaoyuan.calendarmemo.model.CalendarEvent
import com.zhaoyuan.calendarmemo.widget.TodayEventsWidgetProvider
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class CalendarEventViewModel(
    application: Application,
    private val calendarRepository: CalendarRepository,
    private val aiRepository: AiRepository
) : AndroidViewModel(application) {

    constructor(application: Application) : this(
        application,
        CalendarRepository(),
        AiRepository()
    )

    private val appContext = application.applicationContext

    private val _uiState = MutableStateFlow(CalendarUiState())
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    fun loadEvents() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            runCatching {
                calendarRepository.getEvents(appContext).map { event ->
                    event.copy(formattedStartTime = formatDateTime(event.startTime))
                }
            }.onSuccess { events ->
                _uiState.update { it.copy(isLoading = false, events = events, errorMessage = null) }
                notifyWidget()
            }.onFailure { throwable ->
                _uiState.update { it.copy(isLoading = false, errorMessage = throwable.message) }
            }
        }
    }

    fun deleteEvent(event: CalendarEvent) {
        viewModelScope.launch {
            val success = calendarRepository.deleteEvent(appContext, event.id)
            if (success) {
                _uiState.update { state ->
                    state.copy(events = state.events.filterNot { it.id == event.id })
                }
                notifyWidget()
            }
        }
    }

    fun updateEvent(event: CalendarEvent) {
        viewModelScope.launch {
            val success = calendarRepository.updateEvent(
                appContext,
                event.id,
                event.title,
                event.startTime
            )
            if (success) {
                _uiState.update { state ->
                    state.copy(
                        events = state.events.map {
                            if (it.id == event.id) {
                                it.copy(
                                    title = event.title,
                                    startTime = event.startTime,
                                    formattedStartTime = formatDateTime(event.startTime)
                                )
                            } else {
                                it
                            }
                        }
                    )
                }
                notifyWidget()
            }
        }
    }

    fun addEvent(event: CalendarEvent) {
        viewModelScope.launch {
            val newId = calendarRepository.createEvent(appContext, event.title, event.startTime)
            if (newId != null) {
                loadEvents()
            }
        }
    }

    fun addEventsFromContent(contentString: String?) {
        viewModelScope.launch {
            val createdIds = calendarRepository.addEventsFromContent(appContext, contentString)
            if (createdIds.isNotEmpty()) {
                loadEvents()
            }
        }
    }

    fun generateEventsFromPrompt(prompt: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = aiRepository.generateEvents(prompt)
            result.fold(
                onSuccess = { content ->
                    addEventsFromContent(content)
                },
                onFailure = { throwable ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = throwable.message) }
                }
            )
        }
    }

    private fun notifyWidget() {
        TodayEventsWidgetProvider.notifyDataChanged(appContext)
    }

    fun formatDateTime(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy年MM月dd日 HH:mm", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}