package com.callrecorder.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.callrecorder.app.ui.theme.AppColors

/**
 * 캘린더 화면 - 추후 구현 예정.
 * 일단 Coming Soon 안내만 표시.
 */
@Composable
fun CalendarScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AppColors.Background),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Filled.CalendarMonth,
            contentDescription = null,
            tint = AppColors.BrandBlueSoft,
            modifier = Modifier.size(72.dp),
        )
        Spacer(Modifier.height(20.dp))
        Text(
            text = "캘린더",
            style = TextStyle(
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.TextPrimary,
            ),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "예약 일정을 한눈에 볼 수 있는\n캘린더가 곧 추가됩니다.",
            style = TextStyle(
                fontSize = 14.sp,
                color = AppColors.TextSecondary,
                lineHeight = 22.sp,
            ),
            textAlign = TextAlign.Center,
        )
    }
}