package com.zhaoyuan.calendarmemo.network

import okhttp3.*
import com.google.gson.JsonParser
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import java.io.IOException
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class DeepSeekApi() {

    private val client = OkHttpClient()
    private val apiKey = "sk-2798d156255548c7854b48a73a978e5b"
    private val url = "https://api.deepseek.com/chat/completions"

    data class Message(val role: String, val content: String)


    fun requestContent(
        userPrompt: String,
        callback: (success: Boolean, content: String?) -> Unit
    ) {

        val currentDate = LocalDate.now()
        val formatter = DateTimeFormatter.ofPattern("yyyy年MM月dd日")

        val timeString = currentDate.format(formatter)

        val bodyJson = """
            {
                "model": "deepseek-chat",
                "messages": [
                    {"role":"system","content":"你是一个日历提醒机器人，我需要你根据我下面所写的内容生成日历插入参数。参数只包含标题(title)和时间(time)。时间请基于我提供的当前时间$timeString 进行推算。请严格按照此格式输出：[{\"title\":\"xxx\",\"time\":\"yyyy-MM-dd HH:mm\"},{\"title\":\"xxx\",\"time\":\"yyyy-MM-dd HH:mm\"}]"},
                    {"role":"user","content":${jsonString(userPrompt)}}
                ],
                "stream": false
            }
        """.trimIndent()

        val requestBody = RequestBody.create(
            "application/json; charset=utf-8".toMediaTypeOrNull(),
            bodyJson
        )

        val request = Request.Builder()
            .url(url)
            .addHeader("Content-Type", "application/json")
            .addHeader("Authorization", "Bearer $apiKey")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {

            override fun onFailure(call: Call, e: IOException) {
                callback(false, e.message)
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                if (body == null) {
                    callback(false, null)
                    return
                }

                try {
                    val json = JsonParser.parseString(body).asJsonObject

                    // 100% 与你给的返回结构一致
                    val content = json
                        .getAsJsonArray("choices")[0]
                        .asJsonObject
                        .getAsJsonObject("message")
                        .get("content")
                        .asString

                    callback(true, content)

                } catch (e: Exception) {
                    callback(false, e.message)
                }
            }
        })
    }

    private fun jsonString(s: String): String = "\"" + s.replace("\"", "\\\"") + "\""
}
