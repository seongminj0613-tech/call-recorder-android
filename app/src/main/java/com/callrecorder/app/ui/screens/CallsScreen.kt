package com.callrecorder.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.callrecorder.app.data.local.RecordingEntity
import com.callrecorder.app.data.local.RecordingStatus
import com.callrecorder.app.data.model.Call
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallsScreen(
    onCallClick: (String) -> Unit,    // Long → String
    onSettings: () -> Unit,
    vm: CallsViewModel = viewModel(),
) {
    val state by vm.state.collectAsState()
    val locals by vm.localRecordings.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("통화 기록", fontWeight = FontWeight.Bold) },
                actions = {
                    IconButton(onClick = { vm.refresh() }) { Icon(Icons.Default.Refresh, "새로고침") }
                    IconButton(onClick = onSettings) { Icon(Icons.Default.Settings, "설정") }
                },
            )
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // 업로드 진행 중인 로컬 녹음 (최상단)
            val active = locals.filter { it.status in setOf(
                RecordingStatus.PENDING, RecordingStatus.UPLOADING,
                RecordingStatus.UPLOADED, RecordingStatus.PROCESSING,
                RecordingStatus.FAILED,
            ) }
            if (active.isNotEmpty()) {
                item {
                    SectionHeader("처리 중", "${active.size}건")
                }
                items(active, key = { "local-${it.id}" }) { LocalRow(it) }
                item { Spacer(Modifier.height(8.dp)) }
            }

            // 서버에서 받은 완료된 통화
            item { SectionHeader("최근 통화", if (state.calls.isEmpty()) "" else "${state.calls.size}건") }

            if (state.loading && state.calls.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
            } else if (state.calls.isEmpty()) {
                item { EmptyState() }
            } else {
                items(state.calls, key = { it.id }) {
                    CallRow(it, onClick = { onCallClick(it.id) })
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, hint: String) {
    Row(
        Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(hint, style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun EmptyState() {
    Column(
        Modifier.fillMaxWidth().padding(vertical = 60.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("☎️", fontSize = 56.sp)
        Spacer(Modifier.height(12.dp))
        Text("아직 처리된 통화가 없어요", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            "통화가 끝나면 자동으로 여기에 정리됩니다",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun LocalRow(rec: RecordingEntity) {
    val (label, color) = when (rec.status) {
        RecordingStatus.PENDING -> "대기 중" to Color(0xFF8C8C8C)
        RecordingStatus.UPLOADING -> "업로드 중…" to MaterialTheme.colorScheme.primary
        RecordingStatus.UPLOADED -> "업로드 완료" to Color(0xFF4A8A3A)
        RecordingStatus.PROCESSING -> "분석 중…" to MaterialTheme.colorScheme.primary
        RecordingStatus.FAILED -> "실패 - 재시도 예정" to MaterialTheme.colorScheme.error
        else -> rec.status to MaterialTheme.colorScheme.onSurfaceVariant
    }
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(40.dp).clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                if (rec.status == RecordingStatus.UPLOADING || rec.status == RecordingStatus.PROCESSING) {
                    CircularProgressIndicator(
                        Modifier.size(20.dp), strokeWidth = 2.dp, color = color,
                    )
                } else Text("📞")
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    rec.counterpartNumber ?: rec.fileName,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                )
                Text(
                    label,
                    style = MaterialTheme.typography.bodyMedium,
                    color = color,
                )
            }
            Text(
                "${rec.durationSeconds / 60}분 ${rec.durationSeconds % 60}초",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun CallRow(call: Call, onClick: () -> Unit) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier.size(44.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) { Text("📞") }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    call.counterpartNumber ?: "알 수 없음",
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    formatTime(call.callStartedAt),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                StatusBadge(call.status)
                Spacer(Modifier.height(4.dp))
                Text(
                    "${call.durationSeconds / 60}분",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun StatusBadge(status: String) {
    val (label, color) = when (status) {
        "DONE" -> "요약 완료" to Color(0xFF4A8A3A)
        "PROCESSING" -> "분석 중" to Color(0xFFE08A1A)
        "FAILED" -> "실패" to Color(0xFFC44545)
        else -> status to Color(0xFF8C8C8C)
    }
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = color.copy(alpha = 0.15f),
    ) {
        Text(
            label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontSize = 11.sp,
        )
    }
}

private fun formatTime(iso: String): String = try {
    val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    val d = parser.parse(iso.substringBefore('Z').substringBefore('.'))
    SimpleDateFormat("M월 d일 (E) HH:mm", Locale.KOREAN).apply {
        timeZone = TimeZone.getDefault()
    }.format(d ?: Date())
} catch (e: Exception) { iso }