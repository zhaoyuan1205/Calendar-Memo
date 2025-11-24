package com.zhaoyuan.calendarmemo.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import com.zhaoyuan.calendarmemo.voice.StreamingAsrEngine

class VoiceEventViewModel(application: Application) : AndroidViewModel(application) {

    private var _engine: StreamingAsrEngine? = null
    val engine get() = _engine

    fun initEngine(onResult: (String) -> Unit) {
        try {
            _engine = StreamingAsrEngine(getApplication(), onResult)
            _engine?.init()
        } catch (e: Exception) {
            Log.e("ASR", "初始化失败", e)
        }
    }

    fun startAsr() {
        try {
            _engine?.start()
        } catch (e: Exception) {
            Log.e("ASR", "启动失败", e)
        }
    }

    fun stopAsr() {
        try {
            _engine?.stop()
        } catch (e: Exception) {
            Log.e("ASR", "停止失败", e)
        }
    }

    override fun onCleared() {
        _engine?.release()
        super.onCleared()
    }
}
