package com.zhaoyuan.calendarmemo.voice

import android.content.Context
import android.media.*
import android.util.Log
import com.k2fsa.sherpa.onnx.*
import kotlin.ranges.until
import kotlin.text.isNotEmpty

class StreamingAsrEngine(
    private val context: Context,
    private val callback: (String) -> Unit
) {

    private var recognizer: OnlineRecognizer? = null
    private var stream: OnlineStream? = null
    private var recordThread: Thread? = null
    private var running = false

    public var initialized = false


    private val sampleRate = 16000
    private val bufferSize =
        AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )

    private val audioRecord: AudioRecord by lazy {
        Log.d("ASR", "Creating AudioRecord, bufferSize=$bufferSize")
        AudioRecord(
            MediaRecorder.AudioSource.MIC,
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize
        )
    }

    fun init() {

        val modelDir = "model"

        val transducerConfig = OnlineTransducerModelConfig(
            encoder = "$modelDir/encoder-epoch-12-avg-4-chunk-16-left-128.int8.onnx",
            decoder = "$modelDir/decoder-epoch-12-avg-4-chunk-16-left-128.int8.onnx",
            joiner = "$modelDir/joiner-epoch-12-avg-4-chunk-16-left-128.int8.onnx"
        )

        val modelConfig = OnlineModelConfig(
            transducer = transducerConfig,
            tokens = "$modelDir/tokens.txt",
            numThreads = 4,
            provider = "cpu",
            modelType = "transducer"
        )

        val featConfig = FeatureConfig(
            sampleRate = sampleRate,
            featureDim = 80
        )

        val recognizerConfig = OnlineRecognizerConfig(
            modelConfig = modelConfig,
            featConfig = featConfig,
            enableEndpoint = true
        )

        recognizer = OnlineRecognizer(context.assets, recognizerConfig)

        if (recognizer == null) {
            return
        }

        stream = recognizer!!.createStream()


        initialized = (recognizer != null && stream != null)

    }

    fun start() {
        Log.d("ASR", "===== start() called =====")

        if (running) {
            return
        }

        if (recognizer == null) {
            return
        }
        if (stream == null) {
            return
        }

        running = true
        audioRecord.startRecording()

        recordThread = Thread {

            val buffer = ShortArray(512)

            while (running) {
                val n = audioRecord.read(buffer, 0, buffer.size)


                if (n > 0) {
                    val floatBuffer = FloatArray(n)
                    for (i in 0 until n) {
                        floatBuffer[i] = buffer[i] / 32768.0f
                    }

                    val s = stream
                    val r = recognizer

                    if (s == null || r == null) {
                        continue
                    }

                    s.acceptWaveform(floatBuffer, sampleRate)

                    // decode loop
                    while (r.isReady(s)) {
                        Log.d("ASR", "decode()...")
                        r.decode(s)
                    }

                    val result = r.getResult(s)

                    val text = result.text
                    if (text.isNotEmpty()) {
                        callback(text)
                    }
                }
            }

            audioRecord.stop()
        }

        recordThread?.start()
    }

    fun stop() {
        running = false

        // 等录音线程安全退出
        recordThread?.join()

        try {
            audioRecord.stop()
        } catch (_: Exception) {}

        // 释放旧 stream（清除旧识别上下文）
        try {
            stream?.release()
        } catch (_: Exception) {}

        // 创建新的 stream（下一次 start 是全新的）
        stream = recognizer?.createStream()

    }


    fun release() {
        Log.d("ASR", "===== release() called =====")
        stop()

        recognizer?.release()

        recognizer = null
        stream = null
    }
}
