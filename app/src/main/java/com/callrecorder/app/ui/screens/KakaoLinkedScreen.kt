package com.callrecorder.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ChatBubble
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.callrecorder.app.CallRecorderApp
import com.callrecorder.app.ui.theme.AppColors

/**
 * 카카오 계정 연결 완료 화면 (시안 3).
 *
 * 흐름: LoginScreen → 이 화면 → PermissionScreen
 *
 * - 상단 앱바: 작은 폰 아이콘 + "AI 통화 비서" + X 버튼
 * - 본문 카드: 체크 원 + "카카오 계정 연결 완료" + 닉네임 박스
 * - 카카오 노란 버튼: "카카오로 로그인됨" (이미 로그인 상태 표시, 비활성)
 * - 파란 박스 버튼: "권한 설정으로 이동 →"
 * - 텍스트 버튼: "연결 취소" (= 로그아웃)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KakaoLinkedScreen(
    onContinue: () -> Unit,
    onCancel: () -> Unit,
    vm: AuthViewModel = viewModel(),
) {
    // 닉네임을 TokenStore의 nicknameFlow에서 직접 관찰
    val nickname by CallRecorderApp.instance.container.tokenStore.nicknameFlow
        .collectAsState(initial = null)

    Scaffold(
        containerColor = AppColors.Background,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        // 앱바 좌측 작은 폰 아이콘
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .background(AppColors.Surface, RoundedCornerShape(6.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PhoneAndroid,
                                contentDescription = null,
                                tint = AppColors.BrandBlue,
                                modifier = Modifier.size(14.dp),
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "AI 통화 비서",
                            style = TextStyle(
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = AppColors.TextPrimary,
                            ),
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onCancel) {
                        Icon(
                            imageVector = Icons.Filled.Close,
                            contentDescription = "닫기",
                            tint = AppColors.TextPrimary,
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppColors.Background,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.Background)
                .padding(padding)
                .padding(horizontal = 20.dp),
        ) {
            Spacer(Modifier.height(24.dp))

            // ===== 메인 카드 =====
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 4.dp,
                        shape = RoundedCornerShape(16.dp),
                        ambientColor = Color(0x12000000),
                        spotColor = Color(0x12000000),
                    ),
                color = AppColors.Surface,
                shape = RoundedCornerShape(16.dp),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    // ===== 체크 원 =====
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(SuccessSoftBg),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Check,
                            contentDescription = null,
                            tint = SuccessIcon,
                            modifier = Modifier.size(28.dp),
                        )
                    }

                    Spacer(Modifier.height(16.dp))

                    // ===== 헤드라인 =====
                    Text(
                        text = "카카오 계정 연결 완료",
                        style = TextStyle(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.TextPrimary,
                        ),
                    )

                    Spacer(Modifier.height(8.dp))

                    // ===== 안내 문구 =====
                    Text(
                        text = "비즈니스 카카오 계정이 성공적으로 연결되었습니다.",
                        style = TextStyle(
                            fontSize = 12.sp,
                            color = AppColors.TextSecondary,
                            lineHeight = 18.sp,
                        ),
                        textAlign = TextAlign.Center,
                    )

                    Spacer(Modifier.height(20.dp))

                    // ===== 닉네임 박스 =====
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        color = AppColors.Background,
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(
                            width = 1.dp,
                            color = AppColors.Divider,
                        ),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = nickname ?: "사용자",
                                    style = TextStyle(
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = AppColors.TextPrimary,
                                    ),
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = "카카오 계정으로 로그인됨",
                                    style = TextStyle(
                                        fontSize = 11.sp,
                                        color = AppColors.TextSecondary,
                                    ),
                                )
                            }
                            Icon(
                                imageVector = Icons.Filled.Verified,
                                contentDescription = null,
                                tint = AppColors.TextSecondary,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }

                    Spacer(Modifier.height(16.dp))

                    // ===== 카카오 로그인됨 표시 (비활성 상태로) =====
                    Button(
                        onClick = { /* 이미 로그인된 상태 */ },
                        enabled = false,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(
                            disabledContainerColor = AppColors.KakaoYellow,
                            disabledContentColor = AppColors.KakaoBlack,
                        ),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.ChatBubble,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "카카오로 로그인됨",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }

                    Spacer(Modifier.height(10.dp))

                    // ===== 권한 설정으로 이동 (연한 파랑) =====
                    Surface(
                        onClick = onContinue,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        color = SoftBlueButtonBg,
                        shape = RoundedCornerShape(10.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxSize(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                "권한 설정으로 이동",
                                style = TextStyle(
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = AppColors.BrandBlue,
                                ),
                            )
                            Spacer(Modifier.width(6.dp))
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = AppColors.BrandBlue,
                                modifier = Modifier.size(16.dp),
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // ===== 연결 취소 (텍스트 버튼) =====
                    Text(
                        text = "연결 취소",
                        style = TextStyle(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = AppColors.TextSecondary,
                        ),
                        modifier = Modifier
                            .clickable {
                                vm.logout()
                                onCancel()
                            }
                            .padding(vertical = 8.dp, horizontal = 16.dp),
                    )
                }
            }

            Spacer(Modifier.weight(1f))
        }
    }
}

// ===== 이 화면 전용 색상 (시안에서 추출) =====
private val SuccessSoftBg = Color(0xFFD9E6DA)   // 체크 원 연한 회녹 배경
private val SuccessIcon = Color(0xFF5F6D61)     // 체크 마크 진한 회녹
private val SoftBlueButtonBg = Color(0xFFF2F3FB) // "권한 설정으로 이동" 연한 파란 배경