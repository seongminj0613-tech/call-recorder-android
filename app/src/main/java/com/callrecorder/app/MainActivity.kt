package com.callrecorder.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.callrecorder.app.ui.screens.*
import com.callrecorder.app.ui.theme.CallRecorderTheme
import android.util.Log
import com.kakao.sdk.common.util.Utility

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 키 해시 출력 (카카오 콘솔 등록용 - 발표 후 삭제!)
        val keyHash = Utility.getKeyHash(this)
        Log.d("KEY_HASH", "===== 카카오 키 해시: $keyHash =====")

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

    val start = if (token.isNullOrBlank()) Routes.LOGIN else Routes.PERMISSION

    NavHost(navController = nav, startDestination = start) {
        composable(Routes.LOGIN) {
            LoginScreen(
                onLoggedIn = {
                    nav.navigate(Routes.PERMISSION) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
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
                    nav.navigate(Routes.CALLS) {
                        popUpTo(Routes.STORES) { inclusive = true }
                    }
                },
            )
        }
        composable(Routes.CALLS) {
            CallsScreen(
                onCallClick = { id -> nav.navigate("${Routes.CALL_DETAIL}/$id") },
                onSettings = { nav.navigate(Routes.SETTINGS) },
            )
        }
        composable("${Routes.CALL_DETAIL}/{callId}") { backStack ->
            val id = backStack.arguments?.getString("callId").orEmpty()
            CallDetailScreen(callId = id, onBack = { nav.popBackStack() })
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { nav.popBackStack() },
                onChangeStore = { nav.navigate(Routes.STORES) },
                onLoggedOut = {
                    nav.navigate(Routes.LOGIN) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }
    }
}

object Routes {
    const val LOGIN = "login"
    const val PERMISSION = "permission"
    const val STORES = "stores"
    const val CALLS = "calls"
    const val CALL_DETAIL = "call_detail"
    const val SETTINGS = "settings"
}