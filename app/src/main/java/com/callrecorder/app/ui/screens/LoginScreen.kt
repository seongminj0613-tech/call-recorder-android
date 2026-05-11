package com.callrecorder.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.callrecorder.app.ui.theme.AppColors

/**
 * 로그인 화면 (시안 2).
 * - 상단: 작은 폰 아이콘 + "AI 통화 비서"
 * - 중앙: "로그인하고 시작하기" + 안내 문구
 * - 하단: 카카오 노란 버튼 + 약관 안내
 *
 * 비즈니스 로직(카카오 SDK 호출, 토큰 저장 등)은 기존 AuthViewModel 그대로 사용.
 */
@Composable
fun LoginScreen(
    onLoggedIn: () -> Unit,
    vm: AuthViewModel = viewModel(),
) {
    val context = LocalContext.current
    val state by vm.state.collectAsState()

    LaunchedEffect(state.success) { if (state.success) onLoggedIn() }

    Scaffold(
        containerColor = AppColors.Background,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.Background)
                .padding(padding)
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // 상단 여백
            Spacer(Modifier.weight(1f))

            // ===== 작은 폰 아이콘 (인트로보다 작고 그림자 없음) =====
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(AppColors.Surface, RoundedCornerShape(14.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.PhoneAndroid,
                    contentDescription = null,
                    tint = AppColors.BrandBlue,
                    modifier = Modifier.size(28.dp),
                )
            }

            Spacer(Modifier.height(12.dp))

            // ===== 서비스명 =====
            Text(
                text = "AI 통화 비서",
                style = TextStyle(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary,
                ),
            )

            Spacer(Modifier.height(28.dp))

            // ===== 로그인 헤드라인 =====
            Text(
                text = "로그인하고 시작하기",
                style = TextStyle(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary,
                ),
            )

            Spacer(Modifier.height(12.dp))

            // ===== 안내 문구 =====
            Text(
                text = "비즈니스 카카오 계정으로 1초만에 시작하세요.\n번거로운 가입 절차가 없습니다.",
                style = TextStyle(
                    fontSize = 13.sp,
                    color = AppColors.TextSecondary,
                    lineHeight = 20.sp,
                ),
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.weight(1.4f))

            // ===== 카카오 시작하기 버튼 =====
            Button(
                onClick = { vm.loginWithKakao(context) },
                enabled = !state.loading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AppColors.KakaoYellow,
                    contentColor = AppColors.KakaoBlack,
                    disabledContainerColor = AppColors.KakaoYellow.copy(alpha = 0.6f),
                    disabledContentColor = AppColors.KakaoBlack.copy(alpha = 0.6f),
                ),
            ) {
                if (state.loading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = AppColors.KakaoBlack,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Icon(
                        imageVector = Icons.Filled.ChatBubble,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "카카오로 시작하기",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            // ===== 에러 표시 =====
            state.error?.let {
                Spacer(Modifier.height(10.dp))
                Text(
                    text = "로그인 실패: $it",
                    color = androidx.compose.material3.MaterialTheme.colorScheme.error,
                    style = TextStyle(fontSize = 12.sp),
                    textAlign = TextAlign.Center,
                )
            }

            Spacer(Modifier.height(14.dp))

            // ===== 하단 약관 안내 =====
            Text(
                text = "로그인 시 이용약관 및 개인정보 처리방침에\n동의하는 것으로 간주됩니다.",
                style = TextStyle(
                    fontSize = 11.sp,
                    color = AppColors.TextSecondary,
                    lineHeight = 16.sp,
                ),
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(28.dp))
        }
    }
}