// CalendarEventScreen.kt
package com.zhaoyuan.calendarmemo

import android.Manifest
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.zhaoyuan.calendarmemo.model.CalendarEvent
import com.zhaoyuan.calendarmemo.ui.theme.CalendarMemoTheme
import com.zhaoyuan.calendarmemo.viewmodel.CalendarEventViewModel
import com.zhaoyuan.calendarmemo.viewmodel.CalendarUiState
import com.zhaoyuan.calendarmemo.viewmodel.VoiceEventViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CalendarMemoTheme {
                CalendarEventScreen()
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarEventScreen() {
    val context = LocalContext.current
    val calenderViewModel: CalendarEventViewModel = viewModel()
    val voiceViewModel: VoiceEventViewModel = viewModel()
    val calendarUiState by calenderViewModel.uiState.collectAsState()
    val voiceUiState by voiceViewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    var isVoiceSheetVisible by remember { mutableStateOf(false) }

    // 控制弹窗是否显示
    var showDialog by remember { mutableStateOf(false) }
    var selectedEvent by remember { mutableStateOf<CalendarEvent?>(null) }

    // 请求权限
    val launcher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
            val granted = result.values.all { it }
            if (granted) {
                calenderViewModel.loadEvents()
            } else {
                Toast.makeText(context, "权限被拒绝", Toast.LENGTH_SHORT).show()
            }
        }

    //初始启动
    LaunchedEffect(Unit) {
        launcher.launch(
            arrayOf(
                Manifest.permission.READ_CALENDAR,
                Manifest.permission.WRITE_CALENDAR,
                Manifest.permission.RECORD_AUDIO
            )
        )
        withContext(Dispatchers.IO) {
            voiceViewModel.initialize()
        }
    }

    val onRefresh: () -> Unit = {
        calenderViewModel.loadEvents()
    }

    val onDelete: (CalendarEvent) -> Unit = { event ->
        calenderViewModel.deleteEvent(event)
    }

    val onEdit: (CalendarEvent) -> Unit = { event ->
        selectedEvent = event
        showDialog = true
    }
    val onCreateNewEvent: () -> Unit = {
        val now = System.currentTimeMillis()
        selectedEvent = CalendarEvent(
            id = -1L,
            title = "新事件",
            startTime = now,
            formattedStartTime = calenderViewModel.formatDateTime(now)
        )
        showDialog = true
    }
    val showVoiceSheet: () -> Unit = {
        isVoiceSheetVisible = true
        scope.launch {
            sheetState.show()
        }
    }
    val hideVoiceSheet: () -> Unit = {
        scope.launch {
            sheetState.hide()
            isVoiceSheetVisible = false
        }
    }

    val onVoiceStart: () -> Unit = {
        showVoiceSheet()
        voiceViewModel.startRecognition()
    }


    val onVoiceStop: () -> Unit = {
        voiceViewModel.stopRecognition()
        hideVoiceSheet()

        val transcript = voiceUiState.transcript
        if (transcript.isNotBlank()) {
            calenderViewModel.generateEventsFromPrompt(transcript)
            voiceViewModel.resetTranscript()
        }
    }


    // 编辑事件后的保存逻辑
    val onSave: (CalendarEvent) -> Unit = { updatedEvent ->
        if (updatedEvent.id < 0) {
            calenderViewModel.addEvent(updatedEvent)
        } else {
            calenderViewModel.updateEvent(updatedEvent)
        }
        showDialog = false
    }

    CalendarEventView(
        uiState = calendarUiState,
        onDelete = onDelete,
        onRefresh = onRefresh,
        onEdit = onEdit,
        onCreateEvent = onCreateNewEvent,
        onVoiceStart = onVoiceStart,
        onVoiceStop = onVoiceStop
    )

    selectedEvent?.let { event ->
        EventDialog(
            isDialogOpen = showDialog,
            onDismiss = { showDialog = false },
            event = event,
            onSave = onSave
        )
    }

    if (isVoiceSheetVisible) {
        VoiceRecognitionSheet(
            sheetState = sheetState,
            onDismiss = {
                hideVoiceSheet()
                voiceViewModel.stopRecognition()
            },
            transcript = voiceUiState.transcript,
            isListening = voiceUiState.isListening
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarEventView(
    uiState: CalendarUiState,
    onDelete: (CalendarEvent) -> Unit,
    onRefresh: () -> Unit, // 添加 onRefresh 参数
    onEdit: (CalendarEvent) -> Unit,
    onCreateEvent: () -> Unit,
    onVoiceStart: () -> Unit,
    onVoiceStop: () -> Unit
) {
    val listState = rememberLazyListState()
    val events = uiState.events

    LaunchedEffect(events) {
        if (events.isNotEmpty()) {
            val now = System.currentTimeMillis()
            val index = events.indexOfFirst { it.startTime >= now }.let { found ->
                when {
                    found >= 0 -> found
                    else -> events.lastIndex
                }
            }
            listState.scrollToItem(index.coerceAtLeast(0))
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("日历事件") },
                actions = {
                    TextButton(onClick = onRefresh) {
                        Icon(Icons.Filled.Refresh, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("更新事件")
                    }
                }
            )
        },
        floatingActionButton = {
            CustomFAB(
                onTap = {
                    onCreateEvent()
                },
                onLongPress = {
                    onVoiceStart()
                },
                onLongPressEnd = {
                    onVoiceStop()
                }
            )
        },
        floatingActionButtonPosition = FabPosition.Center,
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (uiState.isLoading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                uiState.errorMessage?.let { message ->
                    Text(
                        text = message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    )
                }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    if (events.isEmpty() && !uiState.isLoading) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("暂无日程信息")
                        }
                    } else if (events.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .padding(start = 20.dp, bottom = 30.dp)
                                .width(4.dp)
                                .fillMaxHeight()
                                .background(Color.Black)
                        )
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(start = 16.dp, bottom = 20.dp),
                            state = listState,
                            contentPadding = PaddingValues(bottom = 200.dp)
                        ) {
                            items(events) { event ->
                                CalendarEventItem(event, onDelete = onDelete, onEdit = onEdit)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VoiceRecognitionSheet(
    sheetState: SheetState,
    onDismiss: () -> Unit,
    transcript: String,
    isListening: Boolean
) {
    val configuration = LocalConfiguration.current
    val maxHeight = configuration.screenHeightDp.dp * 0.5f

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 300.dp, max = maxHeight)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (isListening) "正在语音识别" else "处理中",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(16.dp))
            if (isListening) {
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
            }
            Text(
                text = if (transcript.isBlank()) "请开始讲话..." else transcript,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
fun CalendarEventItem(
    event: CalendarEvent,
    onDelete: (CalendarEvent) -> Unit,
    onEdit: (CalendarEvent) -> Unit
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        // 圆点
        Box(
            modifier = Modifier
                .size(12.dp)
                .fillMaxHeight()
                .align(Alignment.CenterVertically) // 垂直居中
                .background(Color.Black, shape = CircleShape),
        )

        Column(
            modifier = Modifier
                .padding(6.dp)
                .fillMaxWidth()
                .background(
                    MaterialTheme.colorScheme.surfaceVariant,
                    shape = MaterialTheme.shapes.medium
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Column(
                    Modifier
                        .padding(6.dp)
                        .weight(1f)
                        .align(Alignment.CenterVertically)
                ) {
                    Text(text = event.title)
                    Text(text = event.formattedStartTime)
//                    if (event.hasReminder) {
//                        val minutesText = if (event.reminderMinutes.isNotEmpty()) {
//                            event.reminderMinutes.joinToString(", ") { "$it 分钟前" }
//                        } else {
//                            "有提醒"
//                        }
//                        Text(text = "提醒：$minutesText", color = Color.Red)
//                    } else {
//                        Text(text = "提醒：无")
//                    }
//            Divider(modifier = Modifier.padding(vertical = 8.dp))
                }
                Row(Modifier.align(Alignment.CenterVertically)) {
                    IconButton(onClick = {
                        onEdit.invoke(event)
                    }) {
                        Icon(
                            Icons.Filled.Edit,
                            contentDescription = "编辑"
                        )
                    }
                    IconButton(onClick = { onDelete.invoke(event) }) {
                        Icon(
                            Icons.Filled.Delete,
                            contentDescription = "删除"
                        )
                    }
                }

            }

        }
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CustomFAB(
    onTap: () -> Unit,
    onLongPress: () -> Unit,
    onLongPressEnd: () -> Unit
) {
    var isLongPress by remember { mutableStateOf(false) }

    Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .combinedClickable(
                onClick = { onTap() },
                onLongClick = {
                    isLongPress = true
                    onLongPress()
                }
            )
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        // 检测手指抬起
                        if (event.changes.all { !it.pressed } && isLongPress) {
                            isLongPress = false
                            onLongPressEnd()
                        }
                    }
                }
            }
    ) {
        Icon(
            Icons.Default.Add,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimary,
            modifier = Modifier.padding(16.dp)
        )
    }
}


@Preview(showBackground = true)
@Composable
fun CalendarEventViewPreview() {
    CalendarMemoTheme {
        val sampleEvents = listOf(
            CalendarEvent(
                id = 1L,
                title = "项目启动会",
                startTime = System.currentTimeMillis(),
                formattedStartTime = "2025-11-24 09:00",
                hasReminder = true,
                reminderMinutes = listOf(10)
            ),
            CalendarEvent(
                id = 2L,
                title = "牙医预约",
                startTime = System.currentTimeMillis() + 3_600_000,
                formattedStartTime = "2025-11-24 11:00",
                hasReminder = false
            )
        )
        CalendarEventView(
            uiState = CalendarUiState(events = sampleEvents),
            onDelete = {},
            onRefresh = {},
            onEdit = {},
            onCreateEvent = {},
            onVoiceStart = {},
            onVoiceStop = {}
        )
    }
}
