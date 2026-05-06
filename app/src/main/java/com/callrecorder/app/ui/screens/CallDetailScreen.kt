package com.callrecorder.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import com.callrecorder.app.data.model.Summary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallDetailScreen(
    callId: String,    // Long → String
    onBack: () -> Unit,
    vm: CallDetailViewModel = viewModel(),
) {
    val state by vm.state.collectAsState()
    LaunchedEffect(callId) { vm.load(callId) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("통화 상세") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "뒤로")
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when {
                state.loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                state.error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("불러오기 실패: ${state.error}")
                }
                state.detail != null -> DetailContent(state.detail!!)
            }
        }
    }
}

@Composable
private fun DetailContent(detail: com.callrecorder.app.data.model.CallDetail) {
    val call = detail.call
    val summary = detail.summary

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // 통화 메타
        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        ) {
            Column(Modifier.padding(20.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier.size(48.dp).clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.4f)),
                        contentAlignment = Alignment.Center,
                    ) { Text("📞", fontSize = 22.sp) }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(
                            call.callerNumber ?: "알 수 없음",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "${(call.duration ?: 0) / 60}분 ${(call.duration ?: 0) % 60}초 통화",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }

        when {
            // 1) 목록 응답의 요약 문자열이 있으면 바로 표시 (웹과 동일)
            !call.summary.isNullOrBlank() -> {
                CallSummaryCard(
                    summaryText = call.summary.orEmpty(),
                    category = call.category,
                    sentiment = call.sentiment,
                )
            }
            // 2) 별도 Summary 객체가 있으면 그것 사용 (백엔드가 풍부한 요약 줄 때)
            summary != null -> {
                SummaryCard(summary)
            }
            // 3) status가 에러면 명시적 안내
            call.status.equals("error", ignoreCase = true) -> {
                Card(shape = RoundedCornerShape(16.dp)) {
                    Column(Modifier.padding(20.dp)) {
                        Text(
                            "❌ 요약을 만들지 못했어요",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "통화가 너무 짧거나 음성이 명확하지 않을 수 있어요.",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
            // 4) 진짜로 처리 중인 케이스 (안내 문구 + 새로고침 유도)
            else -> {
                Card(shape = RoundedCornerShape(16.dp)) {
                    Column(
                        Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 3.dp)
                        Spacer(Modifier.height(12.dp))
                        Text("요약을 만드는 중이에요", style = MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "잠시 후 새로고침해주세요",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        if (!detail.transcript.isNullOrBlank()) {
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(20.dp)) {
                    Text("📝 통화 내용", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(12.dp))
                    Text(
                        detail.transcript,
                        style = MaterialTheme.typography.bodyMedium,
                        lineHeight = 22.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryCard(s: Summary) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(20.dp)) {
            Text(s.title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(6.dp))
            Text(
                s.gist,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 24.sp,
            )

            if (s.actionItems.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))
                Text("✅ 해야 할 일", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                s.actionItems.forEach { item ->
                    Row(Modifier.padding(vertical = 4.dp)) {
                        Text("•  ", fontWeight = FontWeight.Bold)
                        Text(item, style = MaterialTheme.typography.bodyMedium, lineHeight = 22.sp)
                    }
                }
            }

            if (s.keyPoints.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))
                Text("📌 핵심 포인트", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                s.keyPoints.forEach { item ->
                    Row(Modifier.padding(vertical = 4.dp)) {
                        Text("•  ", fontWeight = FontWeight.Bold)
                        Text(item, style = MaterialTheme.typography.bodyMedium, lineHeight = 22.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun CallSummaryCard(
    summaryText: String,
    category: String?,
    sentiment: String?,
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Column(Modifier.padding(20.dp)) {
            if (!category.isNullOrBlank() || !sentiment.isNullOrBlank()) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (!category.isNullOrBlank()) {
                        AssistChip(
                            onClick = {},
                            label = { Text(category) },
                        )
                    }
                    if (!sentiment.isNullOrBlank()) {
                        AssistChip(
                            onClick = {},
                            label = {
                                Text(
                                    when (sentiment) {
                                        "positive" -> "😊 긍정"
                                        "negative" -> "😟 부정"
                                        else -> "😐 중립"
                                    }
                                )
                            },
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            Text(
                "📋 통화 요약",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                summaryText,
                style = MaterialTheme.typography.bodyLarge,
                lineHeight = 24.sp,
            )
        }
    }
}