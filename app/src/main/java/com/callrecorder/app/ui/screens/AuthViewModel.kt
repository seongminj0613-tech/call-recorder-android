package com.callrecorder.app.ui.screens

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.callrecorder.app.CallRecorderApp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel : ViewModel() {

    private val container = CallRecorderApp.instance.container
    private val repo = container.authRepo

    private val _state = MutableStateFlow(AuthUiState())
    val state: StateFlow<AuthUiState> = _state.asStateFlow()

    val isLoggedIn = container.tokenStore.accessTokenFlow

    fun loginWithKakao(context: Context) {
        if (_state.value.loading) return
        _state.value = _state.value.copy(loading = true, error = null)
        viewModelScope.launch {
            repo.loginWithKakao(context).fold(
                onSuccess = { _state.value = AuthUiState(loading = false, success = true) },
                onFailure = { _state.value = AuthUiState(loading = false, error = it.message) },
            )
        }
    }

    fun logout() {
        viewModelScope.launch { repo.logout() }
    }
}

data class AuthUiState(
    val loading: Boolean = false,
    val success: Boolean = false,
    val error: String? = null,
)
