package com.callrecorder.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.callrecorder.app.data.model.Call
import com.callrecorder.app.data.model.CallCategoryLabel
import com.callrecorder.app.data.model.extractedInfoOrNull
import com.callrecorder.app.ui.theme.AppColors
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

/**
 * 통화 요약 목록 (시안 8 / 두 번째 이미지).
 * 시안 5의 대시보드 카드보다 더 상세한 카드 — 이름/시간/인원/특이사항 노출.
 *
 * 정렬:
 *  - 이름순 (extracted_info.customer_name 기준 가나다순)
 *  - 날짜순 (created_at 기준 최신 위)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallSummaryListScreen(
    onCallClick: (String) -> Unit,
    vm: HomeViewModel = viewModel(),  // 같은 데이터 소스 사용 (홈과 통화 탭 동기화)
) {
    val state by vm.state.collectAsState()
    var sort by remember { mutableStateOf(SortOrder.DATE) }

    val sortedCalls = remember(state.recentCalls, sort) {
        // 중복 id 제거 (서버 응답에 같은 통화가 여러 번 올 수 있음 - 방어 코드)
        val unique = state.recentCalls.distinctBy { it.id }
        when (sort) {
            SortOrder.NAME -> unique.sortedBy {
                it.extractedInfoOrNull()?.customerName ?: "힣"
            }
            SortOrder.DATE -> unique.sortedByDescending { it.createdAt }
        }
    }

    Scaffold(
        containerColor = AppColors.Background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "AI 통화 비서",
                        style = TextStyle(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.TextPrimary,
                        ),
                    )
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
                .padding(horizontal = 16.dp),
        ) {
            Text(
                text = "통화 요약 목록",
                style = TextStyle(
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = AppColors.TextPrimary,
                ),
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "최근 예약 요청 및 문의 사항을 확인하세요.",
                style = TextStyle(
                    fontSize = 12.sp,
                    color = AppColors.TextSecondary,
                ),
            )
            Spacer(Modifier.height(16.dp))

            // 정렬 토글
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                SortChip(
                    label = "📛 이름순",
                    selected = sort == SortOrder.NAME,
                    onClick = { sort = SortOrder.NAME },
                )
                SortChip(
                    label = "📅 날짜순",
                    selected = sort == SortOrder.DATE,
                    onClick = { sort = SortOrder.DATE },
                )
            }

            Spacer(Modifier.height(16.dp))

            if (state.loading && sortedCalls.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AppColors.BrandBlue)
                }
            } else if (sortedCalls.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        "분석된 통화가 없어요.",
                        style = TextStyle(fontSize = 13.sp, color = AppColors.TextSecondary),
                    )
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 12.dp),
                ) {
                    itemsIndexed(
                        items = sortedCalls,
                        key = { index, call -> "${call.id}_$index" },
                    ) { _, call ->
                        SummaryCard(call = call, onClick = { onCallClick(call.id) })
                    }
                }
            }
        }
    }
}

private enum class SortOrder { NAME, DATE }

/* ─────────────────────────────────────────────────────
 * 정렬 토글 칩
 * ───────────────────────────────────────────────────── */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SortChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        color = if (selected) SortChipBgSelected else AppColors.Surface,
        shape = RoundedCornerShape(20.dp),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (selected) AppColors.BrandBlue else AppColors.Divider,
        ),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
            style = TextStyle(
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (selected) AppColors.BrandBlue else AppColors.TextSecondary,
            ),
        )
    }
}

/* ─────────────────────────────────────────────────────
 * 요약 카드 (이름 강조 + 시간/인원/특이사항)
 * ───────────────────────────────────────────────────── */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SummaryCard(call: Call, onClick: () -> Unit) {
    val info = call.extractedInfoOrNull()

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
            // ── 분류 배지 ──
            CategoryPill(category = call.category)

            Spacer(Modifier.height(6.dp))

            // ── 이름 (또는 발신자) ──
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = info?.customerName ?: call.callerNumber ?: "발신번호 없음",
                    style = TextStyle(
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary,
                    ),
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = formatDateShort(call.createdAt),
                    style = TextStyle(
                        fontSize = 11.sp,
                        color = AppColors.TextSecondary,
                    ),
                )
            }

            Spacer(Modifier.height(6.dp))

            // ── 시간 / 인원 한 줄 ──
            if (info != null && (info.time != null || info.partySize != null)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    info.time?.let {
                        InfoChip(icon = Icons.Filled.AccessTime, text = "시간 $it")
                    }
                    if (info.time != null && info.partySize != null) {
                        Spacer(Modifier.width(10.dp))
                    }
                    info.partySize?.let {
                        InfoChip(icon = Icons.Filled.Group, text = "인원 ${it}명")
                    }
                }
            }

            // ── 특이사항 ──
            val notes = info?.specialNotes?.takeIf { it.isNotBlank() }
            if (!notes.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Surface(
                    color = NoteBg,
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.Top,
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Info,
                            contentDescription = null,
                            tint = NoteAccent,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "특이사항: $notes",
                            style = TextStyle(
                                fontSize = 12.sp,
                                color = AppColors.TextPrimary,
                                lineHeight = 18.sp,
                            ),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoChip(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = AppColors.TextSecondary,
            modifier = Modifier.size(13.dp),
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = text,
            style = TextStyle(
                fontSize = 12.sp,
                color = AppColors.TextSecondary,
            ),
        )
    }
}

@Composable
private fun CategoryPill(category: String?) {
    val (label, color) = when (category) {
        CallCategoryLabel.RESERVATION -> "신규 예약" to Color(0xFF005ABE)
        CallCategoryLabel.CANCEL -> "취소" to Color(0xFFC44545)
        CallCategoryLabel.COMPLAINT -> "불만 사항" to Color(0xFFD97706)
        CallCategoryLabel.INQUIRY -> "문의" to Color(0xFF4F46E5)
        else -> "기타" to Color(0xFF6B7280)
    }
    Text(
        text = label,
        style = TextStyle(
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = color,
        ),
    )
}

/* ─────────────────────────────────────────────────────
 * 유틸
 * ───────────────────────────────────────────────────── */
private fun formatDateShort(serverTime: String?): String {
    if (serverTime.isNullOrBlank()) return ""
    val fmts = listOf(
        "yyyy-MM-dd HH:mm:ss",
        "yyyy-MM-dd'T'HH:mm:ss",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
    )
    for (fmt in fmts) {
        try {
            val sdf = SimpleDateFormat(fmt, Locale.US).apply {
                timeZone = TimeZone.getTimeZone("UTC")
            }
            val date = sdf.parse(serverTime) ?: continue
            return SimpleDateFormat("yyyy.MM.dd", Locale.KOREAN).format(date)
        } catch (_: Exception) {
            // 다음 포맷 시도
        }
    }
    return ""
}
/* 색상 */
private val SortChipBgSelected = Color(0xFFF2F3FB)
private val NoteBg = Color(0xFFFFF7E6)            // 연주황 - 특이사항 강조
private val NoteAccent = Color(0xFFD97706)