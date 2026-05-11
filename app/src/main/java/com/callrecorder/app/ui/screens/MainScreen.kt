package com.callrecorder.app.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.callrecorder.app.ui.theme.AppColors

/**
 * 메인 컨테이너 — 하단 탭바와 4개 탭(홈/통화/캘린더/설정)을 관리.
 *
 * 상세 화면(예: 통화 요약 상세)으로 이동할 때는 NavController의 push가 아니라
 * 외부 콜백(onCallClick)을 통해 상위 NavHost가 처리한다.
 * 탭 자체는 단순 상태(BottomTab)로만 관리.
 */
@Composable
fun MainScreen(
    onCallClick: (String) -> Unit,
    onLoggedOut: () -> Unit,
    onChangeStore: () -> Unit,
) {
    var selected by remember { mutableStateOf(BottomTab.HOME) }

    Scaffold(
        containerColor = AppColors.Background,
        bottomBar = {
            BottomTabBar(
                selected = selected,
                onSelect = { selected = it },
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            when (selected) {
                BottomTab.HOME -> HomeScreen(
                    onCallClick = onCallClick,
                    onSettings = { selected = BottomTab.SETTINGS },
                )
                BottomTab.CALLS -> CallSummaryListScreen(
                    onCallClick = onCallClick,
                )
                BottomTab.CALENDAR -> CalendarScreen()
                BottomTab.SETTINGS -> SettingsScreen(
                    onBack = { selected = BottomTab.HOME },
                    onChangeStore = onChangeStore,
                    onLoggedOut = onLoggedOut,
                )
            }
        }
    }
}

enum class BottomTab(val label: String, val icon: ImageVector) {
    HOME("홈", Icons.Filled.Home),
    CALLS("통화", Icons.Filled.Mic),
    CALENDAR("캘린더", Icons.Filled.CalendarMonth),
    SETTINGS("설정", Icons.Filled.Settings),
}

@Composable
private fun BottomTabBar(
    selected: BottomTab,
    onSelect: (BottomTab) -> Unit,
) {
    NavigationBar(
        containerColor = AppColors.Background,
        tonalElevation = 0.dp,
    ) {
        BottomTab.values().forEach { tab ->
            val isSelected = tab == selected
            NavigationBarItem(
                selected = isSelected,
                onClick = { onSelect(tab) },
                icon = {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.label,
                    )
                },
                label = {
                    Text(
                        tab.label,
                        style = TextStyle(
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                        ),
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = AppColors.BrandBlue,
                    selectedTextColor = AppColors.BrandBlue,
                    unselectedIconColor = TabUnselected,
                    unselectedTextColor = TabUnselected,
                    indicatorColor = Color.Transparent,
                ),
            )
        }
    }
}

private val TabUnselected = Color(0xFFA5A4AD)