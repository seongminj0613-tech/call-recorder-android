package com.callrecorder.app.ui.screens

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.callrecorder.app.service.RecordingObserverService
import com.callrecorder.app.worker.UploadWorker
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState

@OptIn(ExperimentalPermissionsApi::class)
@Composable
fun PermissionScreen(onGranted: () -> Unit) {
    val context = LocalContext.current

    val perms = buildList {
        if (Build.VERSION.SDK_INT >= 33) {
            add(Manifest.permission.READ_MEDIA_AUDIO)
            add(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
    }

    val permsState = rememberMultiplePermissionsState(perms)

    LaunchedEffect(permsState.allPermissionsGranted) {
        if (permsState.allPermissionsGranted) {
            // 옵저버 + 주기적 워커 시작
            RecordingObserverService.start(context)
            UploadWorker.enqueuePeriodic(context)
            onGranted()
        }
    }

    Scaffold { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
        ) {
            Spacer(Modifier.height(20.dp))
            Text(
                "권한이 필요해요",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "통화 녹음 파일을 읽어 자동으로 요약하기 위해\n아래 권한이 필요합니다.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 22.sp,
            )

            Spacer(Modifier.height(28.dp))

            PermissionItem(
                icon = "🎙️",
                title = "녹음 파일 접근",
                desc = "휴대폰에 저장된 통화 녹음 파일을 찾기 위해 필요해요",
            )
            PermissionItem(
                icon = "🔔",
                title = "알림",
                desc = "요약이 완성되면 알려드려요",
            )
            PermissionItem(
                icon = "🔋",
                title = "배터리 최적화 제외 (선택)",
                desc = "백그라운드에서 새 녹음을 놓치지 않으려면 권장",
            )

            Spacer(Modifier.weight(1f))

            Button(
                onClick = { permsState.launchMultiplePermissionRequest() },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text("권한 허용하기", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            }

            Spacer(Modifier.height(8.dp))
            TextButton(
                onClick = {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                    runCatching { context.startActivity(intent) }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("배터리 최적화 설정 열기")
            }
            Spacer(Modifier.height(12.dp))
        }
    }
}

@Composable
private fun PermissionItem(icon: String, title: String, desc: String) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(icon, fontSize = 28.sp)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(2.dp))
                Text(
                    desc,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
