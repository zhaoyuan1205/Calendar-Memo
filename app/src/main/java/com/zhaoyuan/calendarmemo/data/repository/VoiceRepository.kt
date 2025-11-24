package com.zhaoyuan.calendarmemo.data.repository

import android.content.Context
import com.zhaoyuan.calendarmemo.voice.StreamingAsrEngine

class VoiceRepository {

    private var engine: StreamingAsrEngine? = null

    fun init(context: Context, onResult: (String) -> Unit) {
        if (engine == null) {
            engine = StreamingAsrEngine(context, onResult).also { it.init() }
        }
    }

    fun startRecognition() {
        engine?.start()
    }

    fun stopRecognition() {
        engine?.stop()
    }

    fun release() {
        engine?.release()
        engine = null
    }
}

