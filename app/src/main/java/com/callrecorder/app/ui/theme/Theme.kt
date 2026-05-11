package com.callrecorder.app.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

/**
 * 시안 톤(파란색 + 화이트)에 맞춘 ColorScheme.
 * 새 화면은 AppColors를 직접 참조하는 것을 권장하며,
 * 기존 화면(MaterialTheme.colorScheme.* 사용)도 시안과 톤이 맞게 유지된다.
 */
private val LightColors = lightColorScheme(
    primary = AppColors.BrandBlue,
    onPrimary = AppColors.TextOnPrimary,
    primaryContainer = Color(0xFFE3EEFF),
    onPrimaryContainer = Color(0xFF002B5C),
    secondary = Color(0xFF4A5160),
    onSecondary = Color.White,
    background = AppColors.Background,
    onBackground = AppColors.TextPrimary,
    surface = AppColors.Surface,
    onSurface = AppColors.TextPrimary,
    surfaceVariant = Color(0xFFF1F2F7),
    onSurfaceVariant = AppColors.TextSecondary,
    outline = AppColors.Divider,
    error = Color(0xFFC44545),
)

val AppTypography = androidx.compose.material3.Typography(
    headlineLarge = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold, lineHeight = 36.sp),
    headlineMedium = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, lineHeight = 30.sp),
    titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
    titleMedium = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.SemiBold),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium),
)

@Composable
fun CallRecorderTheme(
    content: @Composable () -> Unit
) {
    // 다크 모드 / 다이내믹 컬러는 사용하지 않음 (시안 톤 고정)
    val colors = LightColors
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = AppColors.Background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = true
        }
    }
    MaterialTheme(colorScheme = colors, typography = AppTypography, content = content)
}