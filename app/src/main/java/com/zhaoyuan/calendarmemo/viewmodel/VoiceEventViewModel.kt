package com.zhaoyuan.calendarmemo.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.zhaoyuan.calendarmemo.data.repository.VoiceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class VoiceUiState(
    val isListening: Boolean = false,
    val transcript: String = "",
    val errorMessage: String? = null
)

class VoiceEventViewModel(
    application: Application,
    private val voiceRepository: VoiceRepository
) : AndroidViewModel(application) {

    constructor(application: Application) : this(
        application,
        VoiceRepository()
    )

    private val _uiState = MutableStateFlow(VoiceUiState())
    val uiState: StateFlow<VoiceUiState> = _uiState.asStateFlow()

    fun initialize() {
        voiceRepository.init(getApplication()) { text ->
            _uiState.update { it.copy(transcript = text) }
        }
    }

    fun startRecognition() {
        voiceRepository.startRecognition()
        _uiState.update { it.copy(isListening = true, errorMessage = null) }
    }

    fun stopRecognition() {
        voiceRepository.stopRecognition()
        _uiState.update { it.copy(isListening = false) }
    }

    fun resetTranscript() {
        _uiState.update { it.copy(transcript = "") }
    }

    override fun onCleared() {
        voiceRepository.release()
        super.onCleared()
    }
}
