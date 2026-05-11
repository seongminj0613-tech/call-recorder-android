package com.callrecorder.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.CallReceived
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import com.callrecorder.app.data.model.Call
import com.callrecorder.app.data.model.CallCategoryLabel
import com.callrecorder.app.data.model.ExtractedInfo
import com.callrecorder.app.data.model.extractedInfoOrNull
import com.callrecorder.app.ui.theme.AppColors
import com.callrecorder.app.util.SttMessage
import com.callrecorder.app.util.SttParser
import com.callrecorder.app.util.SttSpeaker
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.absoluteValue

/**
 * 통화 상세 화면 (시안 7).
 *
 * 구성:
 *  1) 상단 앱바: ← / "통화 상세" / 공유 + 더보기
 *  2) 연락처 카드: 사람 아이콘 + 이름/번호 + 카테고리 배지
 *  3) 메타 정보: 📅 시간 · ⏱ 길이 · ✓ 수신
 *  4) 음성 재생 위젯: ▶/⏸ + 파형 + 진행시간 + 다운로드 + 재생속도
 *  5) AI 요약: 헤더 + 본문 + 칩들(날짜/인원/메뉴)
 *  6) 통화 원문: 화자별 말풍선 + "복사" 버튼
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CallSummaryDetailScreen(
    callId: String,
    onBack: () -> Unit,
    vm: CallSummaryDetailViewModel = viewModel(),
) {
    val state by vm.state.collectAsState()

    LaunchedEffect(callId) { vm.load(callId) }

    Scaffold(
        containerColor = AppColors.Background,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "통화 상세",
                        style = TextStyle(
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.TextPrimary,
                        ),
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "뒤로",
                            tint = AppColors.TextPrimary,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { /* TODO: 공유 */ }) {
                        Icon(Icons.Filled.Share, "공유", tint = AppColors.TextPrimary)
                    }
                    IconButton(onClick = { /* TODO: 더보기 */ }) {
                        Icon(Icons.Filled.MoreVert, "더보기", tint = AppColors.TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = AppColors.Background,
                ),
            )
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppColors.Background)
                .padding(padding),
        ) {
            when {
                state.loading && state.detail == null -> CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = AppColors.BrandBlue,
                )
                state.error != null -> Text(
                    text = "불러오기 실패: ${state.error}",
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(20.dp),
                    style = TextStyle(fontSize = 13.sp, color = AppColors.TextSecondary),
                )
                state.detail != null -> DetailBody(
                    call = state.detail!!.call,
                    audioUrl = state.audioUrl,
                )
            }
        }
    }
}

/* ─────────────────────────────────────────────────────
 * 본문
 * ───────────────────────────────────────────────────── */
@Composable
private fun DetailBody(call: Call, audioUrl: String?) {
    val info = call.extractedInfoOrNull()
    val messages = remember(call.sttResult) { SttParser.parse(call.sttResult) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        // 1) 연락처 카드
        ContactCard(call = call, info = info)

        // 2) 메타 정보 한 줄
        MetaRow(call = call)

        // 3) 음성 재생 위젯
        AudioPlayerWidget(audioUrl = audioUrl)

        // 4) AI 요약
        if (!call.summary.isNullOrBlank() || info != null) {
            AiSummarySection(summary = call.summary, info = info)
        }

        // 5) 통화 원문
        TranscriptSection(messages = messages, fullText = call.sttResult)

        Spacer(Modifier.height(20.dp))
    }
}

/* ─────────────────────────────────────────────────────
 * 1. 연락처 카드
 * ───────────────────────────────────────────────────── */
@Composable
private fun ContactCard(call: Call, info: ExtractedInfo?) {
    val displayName = info?.customerName ?: "발신자 정보 없음"
    val displayPhone = info?.phone ?: call.callerNumber ?: "발신번호 없음"

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = ContactCardBg,
        shape = RoundedCornerShape(14.dp),
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = null,
                    tint = AppColors.TextSecondary,
                    modifier = Modifier.size(22.dp),
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = displayName,
                    style = TextStyle(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = AppColors.TextPrimary,
                    ),
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = displayPhone,
                    style = TextStyle(
                        fontSize = 12.sp,
                        color = AppColors.TextSecondary,
                    ),
                )
            }
            CategoryChip(category = call.category)
        }
    }
}

@Composable
private fun CategoryChip(category: String?) {
    val (label, bg, fg) = when (category) {
        CallCategoryLabel.RESERVATION -> Triple("예약", Color(0xFFE5DEF7), Color(0xFF5B4FB6))
        CallCategoryLabel.CANCEL -> Triple("취소", Color(0xFFFEE7E7), Color(0xFFC44545))
        CallCategoryLabel.COMPLAINT -> Triple("불만", Color(0xFFFFF1E0), Color(0xFFD97706))
        CallCategoryLabel.INQUIRY -> Triple("문의", Color(0xFFEEF2FF), Color(0xFF4F46E5))
        else -> Triple("기타", Color(0xFFEAEAEF), Color(0xFF6B7280))
    }
    Surface(color = bg, shape = RoundedCornerShape(10.dp)) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
            style = TextStyle(
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = fg,
            ),
        )
    }
}

/* ─────────────────────────────────────────────────────
 * 2. 메타 정보 한 줄
 * ───────────────────────────────────────────────────── */
@Composable
private fun MetaRow(call: Call) {
    val timeLabel = formatCallTime(call.createdAt)
    val durationLabel = formatDuration(call.duration)

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MetaItem(icon = Icons.Filled.AccessTime, text = timeLabel)
        MetaItem(icon = Icons.Filled.AccessTime, text = durationLabel)
        MetaItem(icon = Icons.Filled.CallReceived, text = "수신")
    }
}

@Composable
private fun MetaItem(icon: ImageVector, text: String) {
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

/* ─────────────────────────────────────────────────────
 * 3. 음성 재생 위젯
 * ───────────────────────────────────────────────────── */
@Composable
private fun AudioPlayerWidget(audioUrl: String?) {
    val context = LocalContext.current
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            playWhenReady = false
        }
    }
    var isPlaying by remember { mutableStateOf(false) }
    var currentMs by remember { mutableLongStateOf(0L) }
    var totalMs by remember { mutableLongStateOf(0L) }
    var playbackSpeed by remember { mutableFloatStateOf(1f) }

    // 음성 URL 세팅
    LaunchedEffect(audioUrl) {
        if (!audioUrl.isNullOrBlank()) {
            exoPlayer.setMediaItem(MediaItem.fromUri(Uri.parse(audioUrl)))
            exoPlayer.prepare()
        }
    }

    // 진행 상태 업데이트 루프
    LaunchedEffect(isPlaying) {
        while (isPlaying) {
            currentMs = exoPlayer.currentPosition.coerceAtLeast(0L)
            totalMs = exoPlayer.duration.coerceAtLeast(0L)
            delay(200L)
        }
    }

    // 재생 끝나면 자동으로 isPlaying = false
    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_ENDED) {
                    isPlaying = false
                    exoPlayer.seekTo(0)
                    currentMs = 0L
                }
                if (state == Player.STATE_READY && totalMs == 0L) {
                    totalMs = exoPlayer.duration.coerceAtLeast(0L)
                }
            }
        }
        exoPlayer.addListener(listener)
        onDispose {
            exoPlayer.removeListener(listener)
            exoPlayer.release()
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = AppColors.Surface,
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.Divider),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // 재생 버튼 + 파형 + 시간
            Row(verticalAlignment = Alignment.CenterVertically) {
                // ▶/⏸ 버튼
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(
                            if (audioUrl.isNullOrBlank()) AppColors.Divider
                            else AppColors.BrandBlue
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    IconButton(
                        onClick = {
                            if (audioUrl.isNullOrBlank()) return@IconButton
                            if (isPlaying) {
                                exoPlayer.pause()
                                isPlaying = false
                            } else {
                                exoPlayer.setPlaybackSpeed(playbackSpeed)
                                exoPlayer.play()
                                isPlaying = true
                            }
                        },
                        enabled = !audioUrl.isNullOrBlank(),
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (isPlaying) "일시정지" else "재생",
                            tint = Color.White,
                        )
                    }
                }

                Spacer(Modifier.width(12.dp))

                // 파형 + 시간
                Column(modifier = Modifier.weight(1f)) {
                    Waveform(
                        progress = if (totalMs > 0) currentMs.toFloat() / totalMs else 0f,
                    )
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = msToTime(currentMs),
                            style = TextStyle(fontSize = 11.sp, color = AppColors.TextSecondary),
                        )
                        Text(
                            text = msToTime(totalMs),
                            style = TextStyle(fontSize = 11.sp, color = AppColors.TextSecondary),
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))

            // 다운로드 + 재생속도 버튼
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SmallActionButton(
                    icon = Icons.Filled.Download,
                    label = "다운로드",
                    enabled = !audioUrl.isNullOrBlank(),
                    onClick = {
                        if (!audioUrl.isNullOrBlank()) {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(audioUrl))
                            context.startActivity(intent)
                        }
                    },
                )
                Spacer(Modifier.width(8.dp))
                SmallActionButton(
                    icon = null,
                    label = "${formatSpeed(playbackSpeed)}×",
                    enabled = !audioUrl.isNullOrBlank(),
                    onClick = {
                        playbackSpeed = nextSpeed(playbackSpeed)
                        exoPlayer.setPlaybackSpeed(playbackSpeed)
                    },
                )
            }

            if (audioUrl.isNullOrBlank()) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "음성 파일을 불러오는 중이거나 사용할 수 없습니다.",
                    style = TextStyle(fontSize = 11.sp, color = AppColors.TextSecondary),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

/** 가짜 파형 - 진행도에 따라 좌측은 파랑, 우측은 회색 */
@Composable
private fun Waveform(progress: Float) {
    val bars = remember { generateFakeBars(56) }
    val cappedProgress = progress.coerceIn(0f, 1f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(28.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        bars.forEachIndexed { i, h ->
            val pos = (i + 0.5f) / bars.size
            val color = if (pos <= cappedProgress) AppColors.BrandBlue
            else AppColors.Divider
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height((h * 24f + 4f).dp)
                    .padding(horizontal = 1.dp)
                    .background(color, RoundedCornerShape(1.5.dp)),
            )
        }
    }
}

private fun generateFakeBars(count: Int): List<Float> {
    // 0~1 사이 랜덤 + 가운데가 좀 더 큰 형태로
    return (0 until count).map { i ->
        val base = ((i * 137 + 23) % 100) / 100f
        val mid = 1f - ((i - count / 2f).absoluteValue / (count / 2f))
        (base * 0.6f + mid * 0.5f).coerceIn(0.2f, 1f)
    }
}

@Composable
private fun SmallActionButton(
    icon: ImageVector?,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .border(1.dp, AppColors.Divider, RoundedCornerShape(8.dp)),
        color = AppColors.Surface,
    ) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (enabled) AppColors.TextPrimary else AppColors.TextSecondary,
                    modifier = Modifier.size(13.dp),
                )
                Spacer(Modifier.width(4.dp))
            }
            Text(
                text = label,
                style = TextStyle(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (enabled) AppColors.TextPrimary else AppColors.TextSecondary,
                ),
                modifier = Modifier.clickableEnabled(enabled, onClick),
            )
        }
    }
}

private fun Modifier.clickableEnabled(enabled: Boolean, onClick: () -> Unit): Modifier =
    if (enabled) this.then(Modifier.clickable { onClick() }) else this

private fun nextSpeed(current: Float): Float = when (current) {
    1f -> 1.25f
    1.25f -> 1.5f
    1.5f -> 2f
    2f -> 0.75f
    else -> 1f
}

private fun formatSpeed(speed: Float): String =
    if (speed % 1f == 0f) speed.toInt().toString() else "%.2f".format(speed).trimEnd('0').trimEnd('.')

private fun msToTime(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val m = totalSec / 60
    val s = totalSec % 60
    return "%d:%02d".format(m, s)
}

/* ─────────────────────────────────────────────────────
 * 4. AI 요약 섹션
 * ───────────────────────────────────────────────────── */
@Composable
private fun AiSummarySection(summary: String?, info: ExtractedInfo?) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = AiSummaryBg,
        shape = RoundedCornerShape(14.dp),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // 헤더
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "✦",
                    style = TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = AiSummaryAccent,
                    ),
                )
                Spacer(Modifier.width(4.dp))
                Text(
                    text = "AI 요약",
                    style = TextStyle(
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = AiSummaryAccent,
                    ),
                )
            }

            // 본문
            if (!summary.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = summary,
                    style = TextStyle(
                        fontSize = 13.sp,
                        color = AppColors.TextPrimary,
                        lineHeight = 20.sp,
                    ),
                )
            }

            // 칩들
            if (info != null) {
                val chips = buildList {
                    if (!info.date.isNullOrBlank() || !info.time.isNullOrBlank()) {
                        val v = listOfNotNull(info.date, info.time).joinToString(" ")
                        add(Triple(Icons.Filled.CalendarToday, v, AiChipDateBg))
                    }
                    info.partySize?.let {
                        add(Triple(Icons.Filled.Group, "${it}명", AiChipPartyBg))
                    }
                    if (info.menu.isNotEmpty()) {
                        add(Triple(Icons.Filled.Restaurant, info.menu.joinToString(", "), AiChipMenuBg))
                    }
                }
                if (chips.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        chips.forEach { (icon, label, bg) ->
                            InfoBadge(icon = icon, label = label, bg = bg)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoBadge(icon: ImageVector, label: String, bg: Color) {
    Surface(
        color = bg,
        shape = RoundedCornerShape(8.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = AppColors.TextPrimary,
                modifier = Modifier.size(11.dp),
            )
            Spacer(Modifier.width(4.dp))
            Text(
                text = label,
                style = TextStyle(
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AppColors.TextPrimary,
                ),
            )
        }
    }
}

/* ─────────────────────────────────────────────────────
 * 5. 통화 원문 섹션
 * ───────────────────────────────────────────────────── */
@Composable
private fun TranscriptSection(messages: List<SttMessage>, fullText: String?) {
    val clipboard = LocalClipboardManager.current

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = AppColors.Surface,
        shape = RoundedCornerShape(14.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, AppColors.Divider),
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // 헤더
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("💬", fontSize = 14.sp)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "통화 원문",
                        style = TextStyle(
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = AppColors.TextPrimary,
                        ),
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .padding(4.dp),
                ) {
                    Icon(
                        imageVector = Icons.Filled.ContentCopy,
                        contentDescription = null,
                        tint = AppColors.TextSecondary,
                        modifier = Modifier.size(13.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = "복사",
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .clickable {
                                if (!fullText.isNullOrBlank()) {
                                    clipboard.setText(AnnotatedString(fullText))
                                }
                            }
                            .padding(horizontal = 4.dp, vertical = 2.dp),
                        style = TextStyle(
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = AppColors.TextSecondary,
                        ),
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            if (messages.isEmpty()) {
                Text(
                    text = "통화 원문이 아직 준비되지 않았습니다.",
                    style = TextStyle(fontSize = 12.sp, color = AppColors.TextSecondary),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                )
            } else {
                messages.forEachIndexed { idx, msg ->
                    if (idx > 0) Spacer(Modifier.height(10.dp))
                    MessageBubble(message = msg)
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(message: SttMessage) {
    val isCustomer = message.speaker == SttSpeaker.CUSTOMER || message.speaker == SttSpeaker.UNKNOWN
    val alignment = if (isCustomer) Alignment.Start else Alignment.End
    val bubbleColor = if (isCustomer) BubbleCustomer else AppColors.BrandBlue
    val textColor = if (isCustomer) AppColors.TextPrimary else Color.White
    val speakerLabel = when (message.speaker) {
        SttSpeaker.CUSTOMER -> "고객"
        SttSpeaker.BOT -> "AI 통화 비서"
        SttSpeaker.UNKNOWN -> ""
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment,
    ) {
        if (speakerLabel.isNotBlank()) {
            Text(
                text = speakerLabel,
                style = TextStyle(
                    fontSize = 10.sp,
                    color = AppColors.TextSecondary,
                ),
                modifier = Modifier.padding(horizontal = 6.dp),
            )
            Spacer(Modifier.height(2.dp))
        }
        Surface(
            color = bubbleColor,
            shape = if (isCustomer) {
                RoundedCornerShape(topStart = 4.dp, topEnd = 14.dp, bottomEnd = 14.dp, bottomStart = 14.dp)
            } else {
                RoundedCornerShape(topStart = 14.dp, topEnd = 4.dp, bottomEnd = 14.dp, bottomStart = 14.dp)
            },
            modifier = Modifier.padding(horizontal = 0.dp).fillMaxWidth(0.78f),
        ) {
            Text(
                text = message.text,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                style = TextStyle(
                    fontSize = 13.sp,
                    color = textColor,
                    lineHeight = 19.sp,
                ),
            )
        }
    }
}

/* ─────────────────────────────────────────────────────
 * 유틸 - 시간 포맷
 * ───────────────────────────────────────────────────── */
private fun formatCallTime(serverTime: String?): String {
    if (serverTime.isNullOrBlank()) return ""
    val date = parseDate(serverTime) ?: return ""
    val now = java.util.Calendar.getInstance()
    val cal = java.util.Calendar.getInstance().apply { time = date }
    val isToday = now.get(java.util.Calendar.YEAR) == cal.get(java.util.Calendar.YEAR) &&
            now.get(java.util.Calendar.DAY_OF_YEAR) == cal.get(java.util.Calendar.DAY_OF_YEAR)
    val hm = SimpleDateFormat("HH:mm", Locale.KOREAN).format(date)
    return if (isToday) "오늘 $hm" else SimpleDateFormat("M월 d일 HH:mm", Locale.KOREAN).format(date)
}

private fun formatDuration(durationSec: Int?): String {
    val sec = durationSec ?: return "재생 길이 미상"
    val m = sec / 60
    val s = sec % 60
    return "${m}분 ${s}초"
}

private fun parseDate(s: String): Date? {
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
        } catch (_: Exception) { /* 다음 포맷 */ }
    }
    return null
}

/* ─────────────────────────────────────────────────────
 * 색상
 * ───────────────────────────────────────────────────── */
private val ContactCardBg = Color(0xFFF5F4FA)
private val AiSummaryBg = Color(0xFFF6F6FC)
private val AiSummaryAccent = Color(0xFF005ABE)
private val BubbleCustomer = Color(0xFFF1F2F7)

private val AiChipDateBg = Color(0xFFFEF3C7)         // 연노랑 - 날짜
private val AiChipPartyBg = Color(0xFFE0E7FF)        // 연파랑 - 인원
private val AiChipMenuBg = Color(0xFFD1FAE5)         // 연초록 - 메뉴