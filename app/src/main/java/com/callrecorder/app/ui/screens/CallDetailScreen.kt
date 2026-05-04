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
                            call.counterpartNumber ?: "알 수 없음",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "${call.durationSeconds / 60}분 ${call.durationSeconds % 60}초 통화",
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
            }
        }

        if (summary == null) {
            Card(shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(Modifier.size(28.dp), strokeWidth = 3.dp)
                    Spacer(Modifier.height(12.dp))
                    Text("요약을 만드는 중이에요", style = MaterialTheme.typography.bodyLarge)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "보통 1~2분 정도 걸려요",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            SummaryCard(summary)
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