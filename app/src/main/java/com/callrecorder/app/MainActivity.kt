package com.callrecorder.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.callrecorder.app.ui.screens.AuthViewModel
import com.callrecorder.app.ui.screens.CallSummaryDetailScreen
import com.callrecorder.app.ui.screens.IntroScreen
import com.callrecorder.app.ui.screens.KakaoLinkedScreen
import com.callrecorder.app.ui.screens.LoginScreen
import com.callrecorder.app.ui.screens.MainScreen
import com.callrecorder.app.ui.screens.PermissionScreen
import com.callrecorder.app.ui.screens.StoresScreen
import com.callrecorder.app.ui.theme.CallRecorderTheme
import com.callrecorder.app.util.SafeLog
import com.kakao.sdk.common.util.Utility

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val keyHash = Utility.getKeyHash(this)
        SafeLog.d("KEY_HASH", "===== 카카오 키 해시: $keyHash =====")

        setContent {
            CallRecorderTheme { AppRoot() }
        }
    }
}

@Composable
private fun AppRoot() {
    val nav = rememberNavController()
    val auth: AuthViewModel = viewModel()
    val token by auth.isLoggedIn.collectAsState(initial = null)

    // 새 사용자: INTRO → LOGIN → KAKAO_LINKED → PERMISSION → STORES → MAIN(탭바)
    // 재방문 사용자: 토큰 있으면 PERMISSION 부터 시작 (가게/탭은 다음 자연스럽게)
    val start = if (token.isNullOrBlank()) Routes.INTRO else Routes.PERMISSION

    NavHost(navController = nav, startDestination = start) {
        composable(Routes.INTRO) {
            IntroScreen(
                onStart = {
                    nav.navigate(Routes.LOGIN) {
                        popUpTo(Routes.INTRO) { inclusive = true }
                    }
                },
                onSkip = {
                    nav.navigate(Routes.LOGIN) {
                        popUpTo(Routes.INTRO) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.LOGIN) {
            LoginScreen(
                onLoggedIn = {
                    nav.navigate(Routes.KAKAO_LINKED) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.KAKAO_LINKED) {
            KakaoLinkedScreen(
                onContinue = {
                    nav.navigate(Routes.PERMISSION) {
                        popUpTo(Routes.KAKAO_LINKED) { inclusive = true }
                    }
                },
                onCancel = {
                    nav.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.PERMISSION) {
            PermissionScreen(
                onGranted = {
                    nav.navigate(Routes.STORES) {
                        popUpTo(Routes.PERMISSION) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.STORES) {
            StoresScreen(
                onContinue = {
                    nav.navigate(Routes.MAIN) {
                        popUpTo(Routes.STORES) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.MAIN) {
            MainScreen(
                onCallClick = { id -> nav.navigate("${Routes.CALL_DETAIL}/$id") },
                onLoggedOut = {
                    nav.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onChangeStore = {
                    nav.navigate(Routes.STORES)
                },
            )
        }
        composable("${Routes.CALL_DETAIL}/{callId}") { backStack ->
            val id = backStack.arguments?.getString("callId").orEmpty()
            CallSummaryDetailScreen(callId = id, onBack = { nav.popBackStack() })
        }
    }
}

object Routes {
    const val INTRO = "intro"
    const val LOGIN = "login"
    const val KAKAO_LINKED = "kakao_linked"
    const val PERMISSION = "permission"
    const val STORES = "stores"
    const val MAIN = "main"             // 하단 탭바 컨테이너 (홈/통화/캘린더/설정)
    const val CALL_DETAIL = "call_detail"
}