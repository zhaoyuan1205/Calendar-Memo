package com.zhaoyuan.calendarmemo

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.input.TextFieldValue
import com.zhaoyuan.calendarmemo.model.CalendarEvent
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun EventDialog(
    isDialogOpen: Boolean,
    onDismiss: () -> Unit,
    event: CalendarEvent,
    onSave: (CalendarEvent) -> Unit // 回调保存后的事件
) {
    var newTitle by remember(event.id) { mutableStateOf(TextFieldValue(event.title)) }

    var selectedDateTime by remember(event.id) { mutableStateOf(event.startTime) }

    val context = LocalContext.current

    // 格式化日期和时间的字符串
    val formattedDate = formatDate(selectedDateTime, "yyyy年MM月dd日")
    val formattedTime = formatDate(selectedDateTime, "HH:mm")

    if (isDialogOpen) {
        AlertDialog(
            onDismissRequest = { onDismiss() },
            title = { Text("编辑事件") },
            text = {
                Column {
                    // 标题输入框
                    TextField(
                        value = newTitle,
                        onValueChange = { newTitle = it },
                        label = { Text("标题") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    // 年月日选择框
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        TextField(
                            value = formattedDate,
                            onValueChange = {},
                            label = { Text("选择日期") },
                            readOnly = true,
                            enabled = false,
                            modifier = Modifier
                                .weight(1f)
                                .clickable(
                                    onClick = {
                                        val currentDate = Calendar.getInstance().apply { timeInMillis = selectedDateTime }
                                        val datePickerDialog = DatePickerDialog(
                                            context,
                                            { _, year, month, dayOfMonth ->
                                                val updatedCalendar = Calendar.getInstance().apply {
                                                    timeInMillis = selectedDateTime
                                                    set(Calendar.YEAR, year)
                                                    set(Calendar.MONTH, month)
                                                    set(Calendar.DAY_OF_MONTH, dayOfMonth)
                                                }
                                                selectedDateTime = updatedCalendar.timeInMillis
                                            },
                                            currentDate.get(Calendar.YEAR),
                                            currentDate.get(Calendar.MONTH),
                                            currentDate.get(Calendar.DAY_OF_MONTH)
                                        )
                                        datePickerDialog.show()
                                    }
                                ),
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))

                    // 时分选择框
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        TextField(
                            value = formattedTime,
                            onValueChange = {},
                            label = { Text("选择时间") },
                            readOnly = true,
                            enabled = false,
                            modifier = Modifier
                                .weight(1f)
                                .clickable(
                                    onClick = {
                                        val currentTime = Calendar.getInstance().apply { timeInMillis = selectedDateTime }
                                        val timePickerDialog = TimePickerDialog(
                                            context,
                                            { _, hourOfDay, minute ->
                                                val updatedCalendar = Calendar.getInstance().apply {
                                                    timeInMillis = selectedDateTime
                                                    set(Calendar.HOUR_OF_DAY, hourOfDay)
                                                    set(Calendar.MINUTE, minute)
                                                }
                                                selectedDateTime = updatedCalendar.timeInMillis
                                            },
                                            currentTime.get(Calendar.HOUR_OF_DAY),
                                            currentTime.get(Calendar.MINUTE),
                                            true
                                        )
                                        timePickerDialog.show()
                                    }
                                )
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    onSave(event.copy(title = newTitle.text, startTime = selectedDateTime)) // 保存并回调
                    onDismiss() // 关闭弹窗
                }) {
                    Text("保存")
                }
            },
            dismissButton = {
                TextButton(onClick = { onDismiss() }) {
                    Text("取消")
                }
            }
        )
    }
}

// 格式化日期
fun formatDate(timestamp: Long, pattern: String): String {
    val sdf = SimpleDateFormat(pattern, Locale.getDefault()) // 根据不同的格式
    return sdf.format(Date(timestamp))
}

@Preview(showBackground = true)
@Composable
fun EditEventDialogPreview() {
    EventDialog(
        isDialogOpen = true,
        onDismiss = { /* Handle dismiss */ },
        event = CalendarEvent(1, "Sample Event", System.currentTimeMillis()),
        onSave = { event ->  }
    )
}
