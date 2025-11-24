package com.zhaoyuan.calendarmemo.viewmodel

import com.zhaoyuan.calendarmemo.model.CalendarEvent

data class CalendarUiState(
    val isLoading: Boolean = false,
    val events: List<CalendarEvent> = emptyList(),
    val errorMessage: String? = null
)

