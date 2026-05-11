package com.callrecorder.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callrecorder.app.CallRecorderApp
import com.callrecorder.app.data.model.Call
import com.callrecorder.app.data.model.CallStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * 홈(대시보드) 화면의 상태.
 *
 * - todayTotal: 오늘 들어온 통화 수
 * - todaySummarized: 그 중 요약 완료된 수
 * - todayScheduled: 그 중 일정(예약) 카테고리로 분류된 수
 * - recentCalls: 화면 하단 카드 리스트에 노출할 최근 통화 (최대 N개)
 */
data class HomeUiState(
    val loading: Boolean = false,
    val todayTotal: Int = 0,
    val todaySummarized: Int = 0,
    val todayScheduled: Int = 0,
    val recentCalls: List<Call> = emptyList(),
    val error: String? = null,
)

class HomeViewModel : ViewModel() {

    private val container = CallRecorderApp.instance.container
    private val callRepo = container.callRepo
    private val storeRepo = container.storeRepo

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true, error = null)
            val storeId = storeRepo.activeStoreId()
            callRepo.listCalls(storeId).fold(
                onSuccess = { allCalls ->
                    // 백엔드가 같은 통화를 중복으로 내려주는 케이스 방어
                    val calls = allCalls.distinctBy { it.id }
                    _state.value = HomeUiState(
                        loading = false,
                        todayTotal = countToday(calls),
                        todaySummarized = countTodaySummarized(calls),
                        todayScheduled = countTodayScheduled(calls),
                        recentCalls = calls.take(20),
                    )
                },
                onFailure = {
                    _state.value = _state.value.copy(loading = false, error = it.message)
                },
            )
        }
    }

    // ======= 집계 =======

    private fun countToday(calls: List<Call>): Int =
        calls.count { isToday(it.createdAt) }

    private fun countTodaySummarized(calls: List<Call>): Int =
        calls.count { isToday(it.createdAt) && it.status.equals(CallStatus.SUMMARIZED, true) }

    private fun countTodayScheduled(calls: List<Call>): Int =
        calls.count { isToday(it.createdAt) && it.category == "예약" }

    /**
     * created_at 이 "오늘" 인지 판정.
     * 백엔드는 "2026-05-08 00:41:09" 같은 UTC naive 형식으로 내려줌.
     */
    private fun isToday(createdAt: String?): Boolean {
        if (createdAt.isNullOrBlank()) return false
        val date = parseServerDate(createdAt) ?: return false
        val cal = Calendar.getInstance().apply { time = date }
        val now = Calendar.getInstance()
        return cal.get(Calendar.YEAR) == now.get(Calendar.YEAR) &&
                cal.get(Calendar.DAY_OF_YEAR) == now.get(Calendar.DAY_OF_YEAR)
    }

    private fun parseServerDate(s: String): Date? {
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
}