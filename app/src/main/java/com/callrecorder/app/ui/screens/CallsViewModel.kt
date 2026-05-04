package com.callrecorder.app.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callrecorder.app.CallRecorderApp
import com.callrecorder.app.data.local.RecordingEntity
import com.callrecorder.app.data.model.Call
import com.callrecorder.app.data.model.CallDetail
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class CallsViewModel : ViewModel() {
    private val container = CallRecorderApp.instance.container
    private val callRepo = container.callRepo

    private val _state = MutableStateFlow(CallsUiState())
    val state: StateFlow<CallsUiState> = _state.asStateFlow()

    val localRecordings: StateFlow<List<RecordingEntity>> =
        callRepo.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init { refresh() }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(loading = true)
            val storeId = container.storeRepo.activeStoreId()
            callRepo.listCalls(storeId).fold(
                onSuccess = { _state.value = _state.value.copy(loading = false, calls = it, error = null) },
                onFailure = { _state.value = _state.value.copy(loading = false, error = it.message) },
            )
        }
    }
}

data class CallsUiState(
    val loading: Boolean = false,
    val calls: List<Call> = emptyList(),
    val error: String? = null,
)

class CallDetailViewModel : ViewModel() {
    private val callRepo = CallRecorderApp.instance.container.callRepo

    private val _state = MutableStateFlow(CallDetailUiState())
    val state: StateFlow<CallDetailUiState> = _state.asStateFlow()

    fun load(callId: String) {
        viewModelScope.launch {
            _state.value = CallDetailUiState(loading = true)
            callRepo.getDetail(callId).fold(
                onSuccess = { _state.value = CallDetailUiState(loading = false, detail = it) },
                onFailure = { _state.value = CallDetailUiState(loading = false, error = it.message) },
            )
        }
    }
}

data class CallDetailUiState(
    val loading: Boolean = false,
    val detail: CallDetail? = null,
    val error: String? = null,
)