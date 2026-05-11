package com.callrecorder.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.callrecorder.app.data.model.Call
import com.callrecorder.app.data.model.CallCategoryLabel
import com.callrecorder.app.data.model.CallStatus
import com.callrecorder.app.data.model.extractedInfoOrNull
import com.callrecorder.app.ui.theme.AppColors
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * 홈(대시보드) 화면 — 시안 5.
 *
 * 구성:
 *  - 상단 앱바: 폰 로고 + "AI 통화 비서" + 알림 아이콘
 *  - 통화 요약 카드(파란 카드): 오늘의 총 통화 / 요약 완료 / 일정 등록 카운트
 *  - "최근 통화" + 자동 업데이트 표시 + 통화 카드 리스트
 *
 * 카드 클릭 -> onCallClick(callId) 호출
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onCallClick: (String) -> Unit,
    onSettings: () -> Unit,
    vm: HomeViewModel = viewModel(),
) {
    val state by vm.state.collectAsState()

    Scaffold(
        containerColor = AppColors.Background,
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
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
                    IconButton(onClick = { /* TODO: 알림 화면 */ }) {
                        Icon(
                            imageVector = Icons.Filled.NotificationsNone,
                            contentDescription = "알림",
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
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.Background)
                .padding(padding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // ==== 통화 요약 카드 (파란 큰 카드) ====
            item {
                TodaySummaryCard(
                    total = state.todayTotal,
                    summarized = state.todaySummarized,
                    scheduled = state.todayScheduled,
                    latestCall = state.recentCalls.firstOrNull(),
                )
            }

            // ==== 최근 통화 헤더 ====
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "최근 통화",
                        style = TextStyle(
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.TextPrimary,
                        ),
                    )
                    AutoUpdateChip(onRefresh = vm::refresh)
                }
            }

            // ==== 로딩 / 빈 상태 / 카드 리스트 ====
            if (state.loading && state.recentCalls.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(40.dp),
                        contentAlignment = Alignment.Center,
                    ) { CircularProgressIndicator(color = AppColors.BrandBlue) }
                }
            } else if (state.recentCalls.isEmpty()) {
                item { EmptyRecentCalls() }
            } else {
                // 중복 id 제거 (서버 응답 방어)
                val uniqueCalls = state.recentCalls.distinctBy { it.id }
                items(uniqueCalls, key = { it.id }) { call ->
                    RecentCallCard(call = call, onClick = { onCallClick(call.id) })
                }
            }

            item { Spacer(Modifier.height(8.dp)) }
        }
    }
}

/* ─────────────────────────────────────────────────────
 * 오늘의 통화 요약 카드 (연하늘 카드)
 * ───────────────────────────────────────────────────── */
/* ─────────────────────────────────────────────────────
 * 오늘의 통화 요약 카드 (연하늘 카드)
 * ───────────────────────────────────────────────────── */
@Composable
private fun TodaySummaryCard(
    total: Int,
    summarized: Int,
    scheduled: Int,
    latestCall: Call?,
) {
    val today = remember { todayDateLabel() }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 2.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = Color(0x14000000),
                spotColor = Color(0x14000000),
            ),
        color = SummaryCardBg,
        shape = RoundedCornerShape(16.dp),
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
        ) {
            // ── 상단: 오늘의 통화 / 날짜 + 폰 아이콘 ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        "오늘의 통화",
                        style = TextStyle(
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SummaryCardTextSecondary,
                        ),
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        today,
                        style = TextStyle(
                            fontSize = 12.sp,
                            color = SummaryCardTextSecondary,
                        ),
                    )
                }
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(AppColors.BrandBlue.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Filled.PhoneAndroid,
                        contentDescription = null,
                        tint = AppColors.BrandBlue,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            Spacer(Modifier.height(20.dp))

            // ── 카운트 3종 ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                CountStat(value = total, label = "총 통화", modifier = Modifier.weight(1f))
                CountStat(value = summarized, label = "요약 완료", modifier = Modifier.weight(1f))
                CountStat(value = scheduled, label = "일정 등록", modifier = Modifier.weight(1f))
            }

            // ── 최근 통화 수신 (있을 때만) ──
            if (latestCall != null) {
                Spacer(Modifier.height(16.dp))
                LatestCallStrip(call = latestCall)
            }
        }
    }
}

@Composable
private fun CountStat(value: Int, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "${value}건",
            style = TextStyle(
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = AppColors.BrandBlue,
            ),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            style = TextStyle(
                fontSize = 12.sp,
                color = SummaryCardTextSecondary,
            ),
        )
    }
}

/**
 * 카드 하단에 표시되는 "최근 통화 수신" 한 줄.
 * 가장 최근 통화 1건의 이름/번호/상대시간을 보여준다.
 */
@Composable
private fun LatestCallStrip(call: Call) {
    val info = call.extractedInfoOrNull()
    val name = info?.customerName
    val phone = info?.phone ?: call.callerNumber
    val timeAgo = relativeTime(call.createdAt)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = Color.White.copy(alpha = 0.6f),
        shape = RoundedCornerShape(10.dp),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
        ) {
            Text(
                text = "최근 통화 수신",
                style = TextStyle(
                    fontSize = 11.sp,
                    color = SummaryCardTextSecondary,
                ),
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = phone ?: "발신번호 없음",
                style = TextStyle(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary,
                ),
            )
            if (!name.isNullOrBlank() || timeAgo.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = listOfNotNull(
                        timeAgo.takeIf { it.isNotBlank() },
                        name?.takeIf { it.isNotBlank() },
                    ).joinToString(" · "),
                    style = TextStyle(
                        fontSize = 12.sp,
                        color = SummaryCardTextSecondary,
                    ),
                )
            }
        }
    }
}

/* ─────────────────────────────────────────────────────
 * 최근 통화 카드 (간략 버전 - 시안 5)
 * ───────────────────────────────────────────────────── */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RecentCallCard(call: Call, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 1.dp,
                shape = RoundedCornerShape(14.dp),
                ambientColor = Color(0x10000000),
                spotColor = Color(0x10000000),
            ),
        color = AppColors.Surface,
        shape = RoundedCornerShape(14.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // ── 1줄: 카테고리 배지 + NEW + 시간 ──
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    CategoryBadge(call.category)
                    if (call.isRead == 0) {
                        Spacer(Modifier.width(6.dp))
                        NewBadge()
                    }
                }
                Text(
                    text = relativeTime(call.createdAt),
                    style = TextStyle(
                        fontSize = 11.sp,
                        color = AppColors.TextSecondary,
                    ),
                )
            }

            Spacer(Modifier.height(8.dp))

            // ── 2줄: 발신자 (전화번호 또는 "발신번호 없음") ──
            Text(
                text = call.callerNumber ?: "발신번호 없음",
                style = TextStyle(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary,
                ),
            )

            // ── 라벨 행: 성명/날짜/시간/인원/메뉴/특이사항 ──
            val info = call.extractedInfoOrNull()
            val rows = buildList {
                info?.customerName?.takeIf { it.isNotBlank() }
                    ?.let { add("👤 성명" to it) }
                info?.date?.takeIf { it.isNotBlank() }
                    ?.let { add("📅 날짜" to formatNiceDate(it)) }
                info?.time?.takeIf { it.isNotBlank() }
                    ?.let { add("🕐 시간" to it) }
                info?.partySize?.let { add("👥 인원" to "${it}명") }
                info?.menu?.takeIf { it.isNotEmpty() }
                    ?.let { add("🍽️ 메뉴" to it.joinToString(", ")) }
                info?.specialNotes?.takeIf { it.isNotBlank() }
                    ?.let { add("⚠️ 특이사항" to it) }
            }
            if (rows.isNotEmpty()) {
                Spacer(Modifier.height(10.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    rows.forEach { (label, value) ->
                        Row(verticalAlignment = Alignment.Top) {
                            Text(
                                text = label,
                                modifier = Modifier.width(80.dp),
                                style = TextStyle(
                                    fontSize = 12.sp,
                                    color = AppColors.TextSecondary,
                                ),
                            )
                            Text(
                                text = value,
                                style = TextStyle(
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = AppColors.TextPrimary,
                                ),
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }
                }
            }

            // ── 3줄: AI 요약 한 줄 (있으면) ──
            val summary = call.summary
            if (!summary.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Surface(
                    color = AiSummaryBg,
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Text(
                            text = "✦ AI 요약",
                            style = TextStyle(
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = AiSummaryAccent,
                            ),
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = summary,
                            style = TextStyle(
                                fontSize = 12.sp,
                                color = AppColors.TextPrimary,
                                lineHeight = 18.sp,
                            ),
                            modifier = Modifier.weight(1f),
                            maxLines = 2,
                        )
                    }
                }
            } else if (call.status.equals(CallStatus.PROCESSING, true) ||
                call.status.equals(CallStatus.UPLOADED, true)) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "분석 중…",
                    style = TextStyle(
                        fontSize = 12.sp,
                        color = AppColors.BrandBlue,
                        fontWeight = FontWeight.Medium,
                    ),
                )
            }
        }
    }
}

/* ─────────────────────────────────────────────────────
 * 배지들
 * ───────────────────────────────────────────────────── */
@Composable
private fun CategoryBadge(category: String?) {
    val (label, bg, fg) = when (category) {
        CallCategoryLabel.RESERVATION -> Triple("예약", BadgeReservationBg, BadgeReservationFg)
        CallCategoryLabel.CANCEL -> Triple("취소", BadgeCancelBg, BadgeCancelFg)
        CallCategoryLabel.COMPLAINT -> Triple("불만", BadgeComplaintBg, BadgeComplaintFg)
        CallCategoryLabel.INQUIRY -> Triple("문의", BadgeInquiryBg, BadgeInquiryFg)
        else -> Triple("기타", BadgeNeutralBg, BadgeNeutralFg)
    }
    Surface(
        color = bg,
        shape = RoundedCornerShape(6.dp),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = TextStyle(
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = fg,
            ),
        )
    }
}

@Composable
private fun NewBadge() {
    Surface(
        color = NewBadgeBg,
        shape = RoundedCornerShape(6.dp),
    ) {
        Text(
            text = "NEW",
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
            style = TextStyle(
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            ),
        )
    }
}

/* ─────────────────────────────────────────────────────
 * 자동 업데이트 칩 + 빈 상태
 * ───────────────────────────────────────────────────── */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AutoUpdateChip(onRefresh: () -> Unit) {
    Surface(
        onClick = onRefresh,
        color = AppColors.Surface,
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.Divider),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = Icons.Filled.Refresh,
                contentDescription = null,
                tint = AppColors.BrandBlue,
                modifier = Modifier.size(13.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = "자동 업데이트",
                style = TextStyle(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.BrandBlue,
                ),
            )
        }
    }
}

@Composable
private fun EmptyRecentCalls() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = "📞",
            style = TextStyle(fontSize = 40.sp),
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "아직 분석된 통화가 없어요",
            style = TextStyle(
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                color = AppColors.TextPrimary,
            ),
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = "통화 후 잠시만 기다려 주세요.",
            style = TextStyle(
                fontSize = 12.sp,
                color = AppColors.TextSecondary,
            ),
        )
    }
}

/* ─────────────────────────────────────────────────────
 * 유틸 (시간/날짜 포맷)
 * ───────────────────────────────────────────────────── */

/** "방금 전" / "10분 전" / "10시간 전" / "3일 전" / "MM월 d일" */
private fun relativeTime(serverTime: String?): String {
    if (serverTime.isNullOrBlank()) return ""
    val date = parseServerTime(serverTime) ?: return ""
    val diffMs = System.currentTimeMillis() - date.time
    val mins = diffMs / 60_000L
    val hours = diffMs / 3_600_000L
    val days = diffMs / 86_400_000L
    return when {
        mins < 1 -> "방금 전"
        mins < 60 -> "${mins}분 전"
        hours < 24 -> "${hours}시간 전"
        days < 7 -> "${days}일 전"
        else -> SimpleDateFormat("M월 d일", Locale.KOREAN).format(date)
    }
}

private fun todayDateLabel(): String =
    SimpleDateFormat("yyyy년 M월 d일", Locale.KOREAN).format(Date())

private fun parseServerTime(s: String): Date? {
    val fmts = listOf(
        "yyyy-MM-dd HH:mm:ss",
        "yyyy-MM-dd'T'HH:mm:ss",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
    )
    for (fmt in fmts) {
        try {
            val sdf = SimpleDateFormat(fmt, Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val date = sdf.parse(s) ?: continue
            return date
        } catch (_: Exception) {
            // 다음 포맷 시도
        }
    }
    return null
}

/**
 * "2026-05-12" → "내일 5/12(화)" 같은 친근한 형태로 변환.
 * 웹 dashboard와 동일한 포맷.
 */
private val Weekdays = arrayOf("일", "월", "화", "수", "목", "금", "토")

private fun formatNiceDate(dateStr: String): String {
    return try {
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val target = sdf.parse(dateStr) ?: return dateStr

        val cal = java.util.Calendar.getInstance().apply {
            time = target
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val today = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val diffDays = ((cal.timeInMillis - today.timeInMillis) / 86_400_000L).toInt()

        val prefix = when (diffDays) {
            0 -> "오늘 "
            1 -> "내일 "
            -1 -> "어제 "
            else -> ""
        }
        val month = cal.get(java.util.Calendar.MONTH) + 1
        val day = cal.get(java.util.Calendar.DAY_OF_MONTH)
        val dow = Weekdays[cal.get(java.util.Calendar.DAY_OF_WEEK) - 1]

        "${prefix}${month}/${day}(${dow})"
    } catch (_: Exception) {
        dateStr
    }
}


/* ─────────────────────────────────────────────────────
 * 색상 팔레트 (이 화면 전용)
 * ───────────────────────────────────────────────────── */
private val SummaryCardBg = Color(0xFFE3F2FF)              // 오늘의 통화 카드 배경 - 연하늘
private val SummaryCardTextSecondary = Color(0xFF5B6B7E)   // 카드 안 보조 텍스트

private val AiSummaryBg = Color(0xFFE8F7EE)        // 연두색 - AI 요약 배경
private val AiSummaryAccent = Color(0xFF1F8A4C)    // 진한 초록 - AI 요약 강조

private val BadgeReservationBg = Color(0xFFE3F2FF) // 예약 - 연파랑
private val BadgeReservationFg = Color(0xFF005ABE)
private val BadgeCancelBg = Color(0xFFFEE7E7)      // 취소 - 연빨강
private val BadgeCancelFg = Color(0xFFC44545)
private val BadgeComplaintBg = Color(0xFFFFF1E0)   // 불만 - 연주황
private val BadgeComplaintFg = Color(0xFFD97706)
private val BadgeInquiryBg = Color(0xFFEEF2FF)     // 문의 - 연보라
private val BadgeInquiryFg = Color(0xFF4F46E5)
private val BadgeNeutralBg = Color(0xFFF1F2F7)     // 기타 - 회색
private val BadgeNeutralFg = Color(0xFF6B7280)
private val NewBadgeBg = Color(0xFFFF4D4F)          // NEW 빨강