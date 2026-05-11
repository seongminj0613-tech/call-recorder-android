package com.callrecorder.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * 시안에서 추출한 색상값을 그대로 사용.
 * Material3 컬러스킴에 의존하지 않고 Composable에서 직접 참조한다.
 *
 * 시안: AI 통화 비서 (소상공인용) - 파란색 + 화이트 톤
 */
object AppColors {
    // ===== 배경 =====
    val Background = Color(0xFFFAF9FF)        // 메인 배경 (살짝 보라기 도는 화이트)
    val Surface = Color(0xFFFFFFFF)           // 카드/표면

    // ===== 텍스트 =====
    val TextPrimary = Color(0xFF1B1C23)       // 메인 헤드라인 (거의 검정)
    val TextSecondary = Color(0xFF8A8B94)     // 서브 설명 회색
    val TextOnPrimary = Color(0xFFFFFFFF)     // 파란 버튼 위 흰 글씨

    // ===== 브랜드 (파란색) =====
    val BrandBlue = Color(0xFF005ABE)         // 시작하기 버튼 / 메인 액션
    val BrandBlueDark = Color(0xFF004A9E)     // 눌렸을 때 (조금 더 진하게)
    val BrandBlueSoft = Color(0xFF86AAE0)     // 연한 파랑 (아이콘 등)

    // ===== 카카오 =====
    val KakaoYellow = Color(0xFFFEE500)
    val KakaoBlack = Color(0xFF191919)

    // ===== 보조 =====
    val Divider = Color(0xFFEDECF2)           // 구분선
    val IconBoxBg = Color(0xFFFFFFFF)         // 로고 박스 배경
    val IconBoxShadow = Color(0x14000000)     // 로고 박스 그림자
}