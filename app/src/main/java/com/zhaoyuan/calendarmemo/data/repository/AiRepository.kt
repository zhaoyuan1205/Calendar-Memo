package com.zhaoyuan.calendarmemo.data.repository

import com.zhaoyuan.calendarmemo.network.DeepSeekApi
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class AiRepository(
    private val api: DeepSeekApi = DeepSeekApi()
) {

    suspend fun generateEvents(prompt: String): Result<String?> =
        suspendCancellableCoroutine { continuation ->
            api.requestContent(prompt) { success, result ->
                if (continuation.isActive) {
                    continuation.resume(
                        if (success) Result.success(result)
                        else Result.failure(IllegalStateException(result))
                    )
                }
            }
        }
}

