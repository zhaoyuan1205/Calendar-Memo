//package com.zhaoyuan.calendarmemo
//
//import android.Manifest
//import android.content.Context
//import android.net.Uri
//import android.os.Bundle
//import android.provider.CalendarContract
//import android.widget.Toast
//import androidx.activity.ComponentActivity
//import androidx.activity.compose.rememberLauncherForActivityResult
//import androidx.activity.compose.setContent
//import androidx.activity.enableEdgeToEdge
//import androidx.activity.result.contract.ActivityResultContracts
//import androidx.compose.foundation.background
//import androidx.compose.foundation.layout.Box
//import androidx.compose.foundation.layout.Column
//import androidx.compose.foundation.layout.PaddingValues
//import androidx.compose.foundation.layout.Row
//import androidx.compose.foundation.layout.Spacer
//import androidx.compose.foundation.layout.fillMaxHeight
//import androidx.compose.foundation.layout.fillMaxSize
//import androidx.compose.foundation.layout.fillMaxWidth
//import androidx.compose.foundation.layout.padding
//import androidx.compose.foundation.layout.size
//import androidx.compose.foundation.layout.width
//import androidx.compose.foundation.lazy.LazyColumn
//import androidx.compose.foundation.lazy.items
//import androidx.compose.foundation.shape.CircleShape
//import androidx.compose.material.icons.Icons
//import androidx.compose.material.icons.filled.Add
//import androidx.compose.material.icons.filled.Delete
//import androidx.compose.material.icons.filled.Edit
//import androidx.compose.material.icons.filled.Refresh
//import androidx.compose.material3.ExperimentalMaterial3Api
//import androidx.compose.material3.FabPosition
//import androidx.compose.material3.FloatingActionButton
//import androidx.compose.material3.Icon
//import androidx.compose.material3.IconButton
//import androidx.compose.material3.MaterialTheme
//import androidx.compose.material3.Scaffold
//import androidx.compose.material3.Text
//import androidx.compose.material3.TextButton
//import androidx.compose.material3.TopAppBar
//import androidx.compose.runtime.Composable
//import androidx.compose.runtime.LaunchedEffect
//import androidx.compose.runtime.getValue
//import androidx.compose.runtime.mutableStateOf
//import androidx.compose.runtime.remember
//import androidx.compose.ui.Modifier
//import androidx.compose.ui.platform.LocalContext
//import androidx.compose.ui.tooling.preview.Preview
//import com.zhaoyuan.calendarmemo.ui.theme.CalendarMemoTheme
//import java.util.Calendar
//import java.util.Date
//import androidx.compose.runtime.setValue
//import androidx.compose.ui.Alignment
//import androidx.compose.ui.graphics.Color
//import androidx.compose.ui.unit.dp
//import java.text.SimpleDateFormat
//import java.util.Locale
//
//
//class MainActivity : ComponentActivity() {
//    override fun onCreate(savedInstanceState: Bundle?) {
//        super.onCreate(savedInstanceState)
//        enableEdgeToEdge()
//        setContent {
//            CalendarMemoTheme {
//                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
//                    CalendarEventScreen()
//                }
//            }
//        }
//    }
//}
//
///**
// * 日历事件数据类，增加提醒字段
// */
//data class CalendarEvent(
//    val id: Long,
//    val title: String,
//    val startTime: Long,
//    val hasReminder: Boolean = false,      // 是否有提醒
//    val reminderMinutes: List<Int> = emptyList() // 提前多少分钟提醒
//)
//
///**
// * Compose UI
// */
//
//@Composable
//fun CalendarEventScreen() {
//    val context = LocalContext.current
//
//    val permissions = arrayOf(
//        Manifest.permission.READ_CALENDAR,
//        Manifest.permission.WRITE_CALENDAR
//    )
//
//    // 注意：需要导入 getValue/setValue，以便使用 `by`
//    var events by remember { mutableStateOf(listOf<CalendarEvent>()) }
//
//    val launcher = rememberLauncherForActivityResult(
//        contract = ActivityResultContracts.RequestMultiplePermissions()
//    ) { result ->
//        val granted = result.values.all { it }
//        if (granted) {
//            Toast.makeText(context, "权限已授予，正在读取日程…", Toast.LENGTH_SHORT).show()
//            // 这里给状态赋新值
//            events = readCalendarEvents(context)
//        } else {
//            Toast.makeText(context, "权限被拒绝", Toast.LENGTH_SHORT).show()
//        }
//    }
//
//    LaunchedEffect(Unit) {
//        launcher.launch(permissions)
//    }
//
//    val onRefresh: () -> Unit = {
//        launcher.launch(permissions)
//    }
//    val onDelete: (CalendarEvent) -> Unit = { event ->
//        val success = deleteCalendarEvent(context, event.id)
//        if (success) {
//            events = events.filter { it.id != event.id } // 更新列表
//            Toast.makeText(context, "已删除日程：${event.title}", Toast.LENGTH_SHORT).show()
//        } else {
//            Toast.makeText(context, "删除失败", Toast.LENGTH_SHORT).show()
//        }
//    }
//
//    CalendarEventView(events, onRefresh = onRefresh, onDelete = onDelete)
//
//}
//
///**
// * 从系统日历读取前后 15 天事件，并获取提醒信息
// */
//fun readCalendarEvents(context: Context): List<CalendarEvent> {
//    val contentResolver = context.contentResolver
//
//    val events = mutableListOf<CalendarEvent>()
//
//    val startCalendar = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -15) }
//    val endCalendar = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, +15) }
//
//    val uri: Uri = CalendarContract.Events.CONTENT_URI
//    val projection = arrayOf(
//        CalendarContract.Events._ID,
//        CalendarContract.Events.TITLE,
//        CalendarContract.Events.DTSTART,
//        CalendarContract.Events.HAS_ALARM,
//    )
//
//    val selection =
//        "${CalendarContract.Events.DTSTART} >= ? AND ${CalendarContract.Events.DTEND} <= ? AND ${CalendarContract.Events.DELETED} = 0"
//    val selectionArgs = arrayOf(
//        startCalendar.timeInMillis.toString(),
//        endCalendar.timeInMillis.toString()
//    )
//
//    val cursor = contentResolver.query(
//        uri,
//        projection,
//        selection,
//        selectionArgs,
//        "${CalendarContract.Events.DTSTART} ASC"
//    )
//
//    cursor?.use { c ->
//        while (c.moveToNext()) {
//            val id = c.getLong(c.getColumnIndexOrThrow(CalendarContract.Events._ID))
//            val title =
//                c.getString(c.getColumnIndexOrThrow(CalendarContract.Events.TITLE)) ?: "无标题"
//            val start = c.getLong(c.getColumnIndexOrThrow(CalendarContract.Events.DTSTART))
//            val hasAlarm = c.getInt(c.getColumnIndexOrThrow(CalendarContract.Events.HAS_ALARM)) == 1
//
//
//            // 查询该事件的提醒分钟
//            val reminderMinutes = mutableListOf<Int>()
//            if (hasAlarm) {
//                val reminderCursor = contentResolver.query(
//                    CalendarContract.Reminders.CONTENT_URI,
//                    arrayOf(CalendarContract.Reminders.MINUTES, CalendarContract.Reminders.METHOD),
//                    "${CalendarContract.Reminders.EVENT_ID} = ?",
//                    arrayOf(id.toString()),
//                    null
//                )
//                reminderCursor?.use { rc ->
//                    while (rc.moveToNext()) {
//                        val minutes =
//                            rc.getInt(rc.getColumnIndexOrThrow(CalendarContract.Reminders.MINUTES))
//                        reminderMinutes.add(minutes)
//                    }
//                }
//            }
//
//            events.add(
//                CalendarEvent(
//                    id = id,
//                    title = title,
//                    startTime = start,
//                    hasReminder = hasAlarm,
//                    reminderMinutes = reminderMinutes
//                )
//            )
//        }
//    }
//
//    return events
//}
//
//fun formatDateTime(timestamp: Long): String {
//    val sdf = SimpleDateFormat("yyyy年MM月dd日 HH:mm", Locale.getDefault()) // 中文格式
//    return sdf.format(Date(timestamp))
//}
//
//fun deleteCalendarEvent(context: Context, eventId: Long): Boolean {
//    return try {
//        val uri = CalendarContract.Events.CONTENT_URI
//        val rows = context.contentResolver.delete(
//            uri,
//            "${CalendarContract.Events._ID} = ?",
//            arrayOf(eventId.toString())
//        )
//        rows > 0
//    } catch (e: SecurityException) {
//        e.printStackTrace()
//        false
//    }
//}
//
//
//@OptIn(ExperimentalMaterial3Api::class)
//@Composable
//fun CalendarEventView(
//    events: List<CalendarEvent>,
//    onRefresh: (() -> Unit)? = {},
//    onDelete: ((CalendarEvent) -> Unit)? = {},
//    onEdit: (() -> Unit)? = {}
//) {
//    Scaffold(
//        topBar = {
//            TopAppBar(
//                title = { Text("日历事件") },
//                actions = {
//                    if (onRefresh != null) {
//                        TextButton(onClick = onRefresh) {
//                            Icon(Icons.Filled.Refresh, contentDescription = null)
//                            Spacer(Modifier.width(4.dp))
//                            Text("更新事件")
//                        }
//                    }
//                }
//            )
//        },
//        floatingActionButton = {
//            FloatingActionButton(
//                onClick = { /* TODO: 打开新增账单页 */ },
//                containerColor = MaterialTheme.colorScheme.primary
//            ) {
//                Icon(
//                    Icons.Filled.Add,
//                    contentDescription = "Add",
//                    tint = Color.White,
//                    modifier = Modifier.size(40.dp)
//                )
//            }
//        },
//        floatingActionButtonPosition = FabPosition.Center,
//    ) { padding ->
//
//        if (events.isEmpty()) {
//            // 列表为空时显示居中提示
//            Box(
//                modifier = Modifier.fillMaxSize(),
//                contentAlignment = Alignment.Center
//            ) {
//                Text(text = "暂无日程信息", color = Color.Gray)
//            }
//        } else {
//
//            Box(
//                modifier = Modifier
//                    .fillMaxWidth()
//                    .padding(padding)
//            ) {
//                // 竖线
//                Box(
//                    modifier = Modifier
//                        .padding(start = 20.dp, bottom = 30.dp)
//                        .width(4.dp)
//                        .fillMaxHeight()
//                        .background(Color.Black)
//                )
//
//                LazyColumn(
//                    modifier = Modifier
//                        .fillMaxSize()
//                        .padding(start = 16.dp, bottom = 20.dp),
//                    contentPadding = PaddingValues(bottom = 200.dp)
//                ) {
//                    items(events) { event ->
//                        CalendarEventItem(event, onDelete = onDelete)
//                    }
//                }
//
//            }
//        }
//
//    }
//}
//
//
///**
// * 单个事件的 UI，显示提醒信息
// */
//@Composable
//fun CalendarEventItem(
//    event: CalendarEvent,
//    onDelete: ((CalendarEvent) -> Unit)? = {},
//    onEdit: (() -> Unit)? = {}
//) {
//
//}
//
//@Preview(showBackground = true)
//@Composable
//fun GreetingPreview() {
//    CalendarMemoTheme {
//
//        val sampleEvents = listOf(
//            CalendarEvent(1, "会议", System.currentTimeMillis(), true),
//            CalendarEvent(2, "午餐", System.currentTimeMillis() + 7200000, false),
//            CalendarEvent(3, "锻炼", System.currentTimeMillis() + 14400000, true)
//        )
//        CalendarMemoTheme {
//            CalendarEventView(sampleEvents, null)
////            CalendarEventItem(CalendarEvent(1, "会议", System.currentTimeMillis(), System.currentTimeMillis() + 3600000, true))
//        }
//    }
//}