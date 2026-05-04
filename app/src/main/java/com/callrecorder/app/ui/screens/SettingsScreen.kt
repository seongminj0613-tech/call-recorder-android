package com.callrecorder.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onChangeStore: () -> Unit,
    onLoggedOut: () -> Unit,
    auth: AuthViewModel = viewModel(),
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("설정") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "뒤로") }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            ListItem(
                leadingContent = { Icon(Icons.Default.Store, null) },
                headlineContent = { Text("가게 변경") },
                modifier = Modifier.fillMaxWidth(),
                trailingContent = { Text("›") },
                tonalElevation = 0.dp,
            )
            HorizontalDivider()
            ListItem(
                leadingContent = { Icon(Icons.Default.ExitToApp, null) },
                headlineContent = { Text("로그아웃") },
                modifier = Modifier.fillMaxWidth(),
                tonalElevation = 0.dp,
            )

            // (간단화 - clickable은 ListItem 자체에 직접 못 붙으므로 실제 구현 시 Surface로 감싸세요)
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = onChangeStore,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            ) { Text("가게 관리 화면으로") }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { auth.logout(); onLoggedOut() },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            ) { Text("로그아웃") }
        }
    }
}
